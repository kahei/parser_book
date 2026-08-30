package parser;

import java.util.ArrayList;
import java.util.List;

public class PegJsonParser implements JsonParser {
    private int cursor;
    private String input;

    private int progressiveCursor;
    private ParseException progressiveException;

    private static class ParseException extends RuntimeException {
        public ParseException(String message) {
            super(message);
        }
    }

    public ParseResult<Ast.JsonValue> parse(String input) {
        this.input = input;
        this.cursor = 0;
        try {
            var value = parseValue();
            return new ParseResult<>(value, input.substring(this.cursor));
        } catch (ParseException e) {
            throw progressiveException;
        }
    }

    private void recognize(String literal) {
        if(input.substring(cursor).startsWith(literal)) {
            cursor += literal.length();
        } else {
            String substring = input.substring(cursor);
            int endIndex = cursor + (literal.length() > substring.length() ? substring.length() : literal.length());
            throwParseException("expected: " + literal + ", actual: " + input.substring(cursor, endIndex));
        }
    }

    private boolean isHexChar(char ch) {
        return ('0' <= ch && ch <= '9') || ('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z');
    }

    private void skipWhitespace() {
        OUTER:
        while(cursor < input.length()) {
            char currentCharacter = input.charAt(cursor);
            switch (currentCharacter) {
                case '\f':
                case '\t':
                case '\r':
                case '\n':
                case '\b':
                case ' ':
                    cursor++;
                    continue OUTER;
                default:
                    break OUTER;
            }
        }
    }

    private Ast.JsonValue parseValue() {
        int backup = cursor;
        try {
            return parseTrue();
        } catch (ParseException e) {
            cursor = backup;
        }

        try {
            return parseFalse();
        } catch (ParseException e) {
            cursor = backup;
        }

        try {
            return parseNull();
        } catch (ParseException e) {
            cursor = backup;
        }

        try {
            return parseNumber();
        } catch (ParseException e) {
            cursor = backup;
        }

        try {
            return parseString();
        } catch (ParseException e) {
            cursor = backup;
        }

        try {
            return parseObject();
        } catch (ParseException e) {
            cursor = backup;
        }

        return parseArray();
    }

    private Ast.JsonTrue parseTrue() {
        recognize("true");
        skipWhitespace();
        return Ast.JsonTrue.getInstance();
    }

    private Ast.JsonFalse parseFalse() {
        recognize("false");
        skipWhitespace();
        return Ast.JsonFalse.getInstance();
    }

    private Ast.JsonNull parseNull() {
        recognize("null");
        skipWhitespace();
        return Ast.JsonNull.getInstance();
    }

    private void parseLBrace() {
        recognize("{");
        skipWhitespace();
    }

    private void parseRBrace() {
        recognize("}");
        skipWhitespace();
    }

    private void parseLBracket() {
        recognize("[");
        skipWhitespace();
    }

    private void parseRBracket() {
        recognize("]");
        skipWhitespace();
    }

    private void parseComma() {
        recognize(",");
        skipWhitespace();
    }

    private void parseColon() {
        recognize(":");
        skipWhitespace();
    }


    private Ast.JsonString parseString() {
        if(cursor >= input.length()) {
            throwParseException("expected: \"" + " actual: EOF");
        }
        char ch = input.charAt(cursor);
        if(ch != '"') {
            throwParseException("expected: \"" + "actual: " + ch);
        }
        cursor++;
        var builder = new StringBuilder();
        var closed = false;
        OUTER:
        while(cursor < input.length()) {
            ch = input.charAt(cursor);
            switch(ch) {
                case '\\':
                    cursor++;
                    if(cursor >= input.length()) break OUTER;
                    char nextCh = input.charAt(cursor);
                    cursor++;
                    switch (nextCh) {
                        case 'b':
                            builder.append('\b');
                            break;
                        case 'f':
                            builder.append('\f');
                            break;
                        case 'n':
                            builder.append('\n');
                            break;
                        case 'r':
                            builder.append('\r');
                            break;
                        case 't':
                            builder.append('\t');
                            break;
                        case '\\':
                            builder.append('\\');
                            break;
                        case '"':
                            builder.append('"');
                            break;
                        case '/':
                            builder.append('/');
                            break;
                        case 'u':
                            if(cursor + 4 <= input.length()) {
                                char[] characters = input.substring(cursor, cursor + 4).toCharArray();
                                for(char character:characters) {
                                    if(!isHexChar(character)) {
                                        throwParseException("invalid unicode escape: " + character);
                                    }
                                }
                                char result = (char)Integer.parseInt(new String(characters), 16);
                                builder.append(result);
                                cursor += 4;
                            } else {
                                throwParseException("invalid unicode escape: " + input.substring(cursor));
                            }
                            break;
                        default:
                            throwParseException("expected: b|f|n|r|t|\"|\\|/ actual: " + nextCh);
                    }
                    break;
                case '"':
                    cursor++;
                    closed = true;
                    break OUTER;
                default:
                    builder.append(ch);
                    cursor++;
                    break;
            }
        }

        if(!closed) {
            throwParseException("expected: " + "\"" + " actual: EOF");
        } else {
            skipWhitespace();
            return new Ast.JsonString(builder.toString());
        }
        throw new RuntimeException("never reach here");
    }

    private void throwParseException(String message) throws ParseException {
        var exception = new ParseException(message);
        if(progressiveCursor < cursor) {
            progressiveCursor = cursor;
            progressiveException = exception;
        }
        throw exception;
    }

    private Ast.JsonNumber parseNumber() {
        int start = cursor;
        char ch = 0;
        while(cursor < input.length()) {
            ch = input.charAt(cursor);
            if(!('0' <= ch && ch <= '9')) break;
            cursor++;
        }
        if(start == cursor) {
            throwParseException("expected: [0-9] actual: " + (ch != 0 ? ch : "EOF"));
        }
        return new Ast.JsonNumber(Integer.parseInt(input.substring(start, cursor)));
    }

    private Pair<Ast.JsonString, Ast.JsonValue> parsePair() {
        var key = parseString();
        parseColon();
        var value = parseValue();
        return new Pair<>(key, value);
    }

    private Ast.JsonObject parseObject() {
        int backup = cursor;
        try {
            parseLBrace();
            parseRBrace();
            return new Ast.JsonObject(new ArrayList<>());
        } catch (ParseException e) {
            cursor = backup;
        }

        parseLBrace();
        List<Pair<Ast.JsonString, Ast.JsonValue>> members = new ArrayList<>();
        var member = parsePair();
        members.add(member);
        try {
            while (true) {
                parseComma();
                member = parsePair();
                members.add(member);
            }
        } catch (ParseException e) {
            parseRBrace();
            return new Ast.JsonObject(members);
        }
    }

    public Ast.JsonArray parseArray() {
        int backup = cursor;
        try {
            parseLBracket();
            parseRBracket();
            return new Ast.JsonArray(new ArrayList<>());
        } catch (ParseException e) {
            cursor = backup;
        }

        parseLBracket();
        List<Ast.JsonValue> values = new ArrayList<>();
        var value = parseValue();
        values.add(value);
        try {
            while (true) {
                parseComma();
                value = parseValue();
                values.add(value);
            }
        } catch (ParseException e) {
            parseRBracket();
            return new Ast.JsonArray(values);
        }
    }
}