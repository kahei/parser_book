package parser;

import java.util.ArrayList;
import java.util.List;

public class SimpleJsonParser implements JsonParser {
    private List<Token> tokens;
    private int index;

    public ParseResult<Ast.JsonValue> parse(String input) {
        SimpleJsonTokenizer tokenizer = new SimpleJsonTokenizer(input);
        this.tokens = tokenizer.tokenizeAll();
        this.index = 0;
        var value = parseValue();
        // 残りの入力を返すため、現在のトークン位置以降を再構築する必要がある
        // ここでは簡単のため空文字列を返す
        return new ParseResult<>(value, "");
    }
    
    private Token current() {
        if (index < tokens.size()) {
            return tokens.get(index);
        }
        return new Token(Token.Type.EOF, null);
    }
    
    private boolean moveNext() {
        if (index < tokens.size() - 1) {
            index++;
            return true;
        }
        return false;
    }

    private Ast.JsonValue parseValue() {
        var token = current();
        switch(token.type) {
            case INTEGER:
                return parseNumber();
            case STRING:
                return parseString();
            case TRUE:
                return parseTrue();
            case FALSE:
                return parseFalse();
            case NULL:
                return parseNull();
            case LBRACKET:
                return parseArray();
            case LBRACE:
                return parseObject();
        }
        throw new RuntimeException("cannot reach here");
    }

    private Ast.JsonTrue parseTrue() {
        if(current().type == Token.Type.TRUE) {
            return Ast.JsonTrue.getInstance();
        }
        throw new parser.ParseException("expected: true, actual: " + current().value);
    }

    private Ast.JsonFalse parseFalse() {
        if(current().type == Token.Type.FALSE) {
            return Ast.JsonFalse.getInstance();
        }
        throw new parser.ParseException("expected: false, actual: " + current().value);
    }

    private Ast.JsonNull parseNull() {
        if(current().type == Token.Type.NULL) {
            return Ast.JsonNull.getInstance();
        }
        throw new parser.ParseException("expected: null, actual: " + current().value);
    }

    private Ast.JsonString parseString() {
        return new Ast.JsonString((String)current().value);
    }

    private Ast.JsonNumber parseNumber() {
        var value = (Integer)current().value;
        return new Ast.JsonNumber(value);
    }

    private Pair<Ast.JsonString, Ast.JsonValue> parsePair() {
        var key = parseString();
        moveNext();
        if(current().type != Token.Type.COLON) {
            throw new parser.ParseException("expected: `:`, actual: " + current().value);
        }
        moveNext();
        var value = parseValue();
        return new Pair<>(key, value);
    }

    private Ast.JsonObject parseObject() {
        if(current().type != Token.Type.LBRACE) {
            throw new parser.ParseException("expected `{`, actual: " + current().value);
        }

        moveNext();
        if(current().type == Token.Type.RBRACE) {
            return new Ast.JsonObject(new ArrayList<>());
        }

        List<Pair<Ast.JsonString, Ast.JsonValue>> members = new ArrayList<>();
        var pair= parsePair();
        members.add(pair);

        while(moveNext()) {
            if(current().type == Token.Type.RBRACE) {
                return new Ast.JsonObject(members);
            }
            if(current().type != Token.Type.COMMA) {
                throw new parser.ParseException("expected: `,`, actual: " + current().value);
            }
            moveNext();
            pair = parsePair();
            members.add(pair);
        }

        throw new parser.ParseException("unexpected EOF");
    }

    private Ast.JsonArray parseArray() {
        if(current().type != Token.Type.LBRACKET) {
            throw new parser.ParseException("expected: `[`, actual: " + current().value);
        }

        moveNext();
        if(current().type == Token.Type.RBRACKET) {
            return new Ast.JsonArray(new ArrayList<>());
        }

        List<Ast.JsonValue> values = new ArrayList<>();
        var value = parseValue();
        values.add(value);

        while(moveNext()) {
            if(current().type == Token.Type.RBRACKET) {
                return new Ast.JsonArray(values);
            }
            if(current().type != Token.Type.COMMA) {
                throw new parser.ParseException("expected: `,`, actual: " + current().value);
            }
            moveNext();
            value = parseValue();
            values.add(value);
        }

        throw new ParseException("unexpected EOF");
    }
}