package parser;

import java.util.ArrayList;
import java.util.List;

public class SimpleJsonTokenizer implements JsonTokenizer {
    private final String input;
    private int index;
    private Token fetched;

    public SimpleJsonTokenizer(String input) {
        this.input = input;
        this.index = 0;
    }

    public String rest() {
        return input.substring(index);
    }
    
    public List<Token> tokenizeAll() {
        List<Token> tokens = new ArrayList<>();
        while (moveNext()) {
            tokens.add(current());
        }
        tokens.add(new Token(Token.Type.EOF, null));
        return tokens;
    }

    private static boolean isDigit(char ch) {
        return '0' <= ch && ch <= '9';
    }

    private boolean tokenizeNumber(boolean positive) {
        char firstChar = input.charAt(index);
        if(!isDigit(firstChar)) return false;
        int result = 0;
        while(index < input.length()) {
            char ch = input.charAt(index);
            if(!isDigit(ch)) {
                fetched = new Token(Token.Type.INTEGER, positive ? result : -result);
                return true;
            }
            result = result * 10 + (ch - '0');
            index++;
        }
        fetched = new Token(Token.Type.INTEGER, positive ? result : -result);
        return true;
    }

    private boolean tokenizeStringLiteral() {
        char firstChar = input.charAt(index);
        int beginIndex = index;
        if(firstChar != '"') return false;
        index++;
        var builder = new StringBuffer();
        while(index < input.length()) {
            char ch = input.charAt(index);
            if(ch == '"') {
                fetched = new Token(Token.Type.STRING, builder.toString());
                index++;
                return true;
            }
            if(ch == '\\') {
                index++;
                if(index >= input.length()) return false;
                char nextCh = input.charAt(index);
                switch(nextCh) {
                    case '\\':
                        builder.append('\\');
                        break;
                    case '"':
                        builder.append('"');
                        break;
                    case '/':
                        builder.append('/');
                        break;
                    case 't':
                        builder.append('\t');
                        break;
                    case 'f':
                        builder.append('\f');
                        break;
                    case 'b':
                        builder.append('\b');
                        break;
                    case 'r':
                        builder.append('\r');
                        break;
                    case 'n':
                        builder.append('\n');
                        break;
                    case 'u':
                        if((index + 1) + 4 >= input.length()) {
                            throw new TokenizerException("unicode escape ends with EOF: " + input.substring(index));
                        }
                        var unicodeEscape= input.substring(index + 1, index + 1 + 4);
                        if(!unicodeEscape.matches("[0-9a-fA-F]{4}")) {
                            throw new TokenizerException("illegal unicode escape: \\u" + unicodeEscape);
                        }
                        builder.append((char)Integer.parseInt(unicodeEscape, 16));
                        index += 4;
                        break;
                }
            } else {
                builder.append(ch);
            }
            index++;
        }
        return false;
    }

    private void accept(String literal, Token.Type type, Object value) {
        String head = input.substring(index);
        if(head.indexOf(literal) == 0) {
            fetched = new Token(type, value);
            index += literal.length();
        } else {
            throw new TokenizerException("expected: " + literal + ", actual: " + head);
        }
    }

    @Override
    public Token current() {
        return fetched;
    }

    @Override
    public boolean moveNext() {
        LOOP:
        while(index < input.length()) {
            char ch = input.charAt(index);
            switch (ch) {
                case '[':
                    accept("[", Token.Type.LBRACKET, "[");
                    return true;
                case ']':
                    accept("]", Token.Type.RBRACKET, "]");
                    return true;
                case '{':
                    accept("{", Token.Type.LBRACE, "{");
                    return true;
                case '}':
                    accept("}", Token.Type.RBRACE, "}");
                    return true;
                case '(':
                    accept("(", Token.Type.LPAREN, "(");
                    return true;
                case ')':
                    accept(")", Token.Type.RPAREN, ")");
                    return true;
                case ',':
                    accept(",", Token.Type.COMMA, ",");
                    return true;
                case ':':
                    accept(":", Token.Type.COLON, ":");
                    return true;
                // true
                case 't':
                    accept("true", Token.Type.TRUE, true);
                    return true;
                // false
                case 'f':
                    accept("false", Token.Type.FALSE, false);
                    return true;
                case 'n': {
                    String actual;
                    if (index + 4 <= input.length()) {
                        actual = input.substring(index, index + 4);
                        if (actual.equals("null")) {
                            fetched = new Token(Token.Type.NULL, null);
                            index += 4;
                            return true;
                        } else {
                            throw new TokenizerException("expected: null, actual: " + actual);
                        }
                    } else {
                        actual = input.substring(index);
                        throw new TokenizerException("expected: null, actual: " + actual);
                    }
                }
                case '"':
                    return tokenizeStringLiteral();
                // whitespace
                case ' ':
                case '\t':
                case '\n':
                case '\r':
                case '\b':
                case '\f':
                    char next = 0;
                    do {
                        index++;
                        next = input.charAt(index);
                    } while (index < input.length() && Character.isWhitespace(next));
                    continue LOOP;
                default:
                    if('0' <= ch && ch <= '9') {
                        return tokenizeNumber(true);
                    } else if (ch == '+') {
                        index++;
                        return tokenizeNumber(true);
                    } else if (ch == '-') {
                        index++;
                        return tokenizeNumber(false);
                    } else {
                        throw new TokenizerException("unexpected character: " + ch);
                    }
            }
        }
        return false;
    }
}
