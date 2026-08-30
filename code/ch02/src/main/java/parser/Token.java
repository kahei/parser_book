package parser;

import java.util.Objects;

public class Token {
    public final Type type;
    public final Object value;
    public enum Type {
        STRING, INTEGER, NULL, TRUE, FALSE,
        LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET, COMMA, COLON, SPACE, EOF
    }

    public Token(Type type, Object value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Token{" +
                "type=" + type +
                ", value=" + value +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Token token = (Token) o;
        return type == token.type &&
                Objects.equals(value, token.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }
}
