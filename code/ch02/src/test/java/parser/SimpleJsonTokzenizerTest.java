package parser;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

public class SimpleJsonTokzenizerTest {
    @Test
    public void tokenizeLParen() {
        var t = new SimpleJsonTokenizer("(");
        t.moveNext();
        assertEquals(Token.Type.LPAREN, t.current().type);
    }

    @Test
    public void tokenizeRParen() {
        var t = new SimpleJsonTokenizer(")");
        t.moveNext();
        assertEquals(Token.Type.RPAREN, t.current().type);
    }

    @Test
    public void tokenizeLBrace() {
        var t = new SimpleJsonTokenizer("{");
        t.moveNext();
        assertEquals(Token.Type.LBRACE, t.current().type);
    }

    @Test
    public void tokenizeRBrace() {
        var t = new SimpleJsonTokenizer("}");
        t.moveNext();
        assertEquals(Token.Type.RBRACE, t.current().type);
    }

    @Test
    public void tokenizeLBracket() {
        var t = new SimpleJsonTokenizer("[");
        t.moveNext();
        assertEquals(Token.Type.LBRACKET, t.current().type);
    }

    @Test
    public void tokenizeRBracket() {
        var t = new SimpleJsonTokenizer("]");
        t.moveNext();
        assertEquals(Token.Type.RBRACKET, t.current().type);
    }

    @Test
    public void tokenizeComma() {
        var t = new SimpleJsonTokenizer(",");
        t.moveNext();
        assertEquals(Token.Type.COMMA, t.current().type);
    }

    @Test
    public void tokenizeColon() {
        var t = new SimpleJsonTokenizer(":");
        t.moveNext();
        assertEquals(Token.Type.COLON, t.current().type);
    }

    @Test
    public void tokenizeTrue() {
        var t = new SimpleJsonTokenizer("true");
        t.moveNext();
        assertEquals(Token.Type.TRUE, t.current().type);
    }

    @Test
    public void tokenizeFalse() {
        var t = new SimpleJsonTokenizer("false");
        t.moveNext();
        assertEquals(Token.Type.FALSE, t.current().type);
    }

    @Test
    public void tokenizeNull() {
        var t = new SimpleJsonTokenizer("null");
        t.moveNext();
        assertEquals(Token.Type.NULL, t.current().type);
    }

    @Test void tokenizeMultpleToken() {
        var t = new SimpleJsonTokenizer("[true]");
        t.moveNext();
        assertEquals(Token.Type.LBRACKET, t.current().type);
        t.moveNext();
        assertEquals(Token.Type.TRUE, t.current().type);
        t.moveNext();
        assertEquals(Token.Type.RBRACKET, t.current().type);
    }

    @Test void tokenizeStringLiterals() {
        JsonTokenizer t;

        t = new SimpleJsonTokenizer("\"\"");
        t.moveNext();
        assertEquals(Token.Type.STRING, t.current().type);
        assertEquals("", t.current().value);

        t = new SimpleJsonTokenizer("\"a\"");
        t.moveNext();
        assertEquals(Token.Type.STRING, t.current().type);
        assertEquals("a", t.current().value);

        t = new SimpleJsonTokenizer("\"ab\"");
        t.moveNext();
        assertEquals(Token.Type.STRING, t.current().type);
        assertEquals("ab", t.current().value);

        t = new SimpleJsonTokenizer("\"\\r\\n\\f\\b\"");
        t.moveNext();
        assertEquals(Token.Type.STRING, t.current().type);
        assertEquals("\r\n\f\b", t.current().value);

    }

    @Test void tokenizePositiveIntegerLiterals() {
        JsonTokenizer t;

        t = new SimpleJsonTokenizer("100");
        t.moveNext();
        assertEquals(Token.Type.INTEGER, t.current().type);
        assertEquals(100, t.current().value);

        t = new SimpleJsonTokenizer("50");
        t.moveNext();
        assertEquals(Token.Type.INTEGER, t.current().type);
        assertEquals(50, t.current().value);

        t = new SimpleJsonTokenizer("1");
        t.moveNext();
        assertEquals(Token.Type.INTEGER, t.current().type);
        assertEquals(1, t.current().value);

        t = new SimpleJsonTokenizer("0");
        t.moveNext();
        assertEquals(Token.Type.INTEGER, t.current().type);
        assertEquals(0, t.current().value);

        t = new SimpleJsonTokenizer(Integer.toString(Integer.MAX_VALUE));
        t.moveNext();
        assertEquals(Token.Type.INTEGER, t.current().type);
        assertEquals(Integer.MAX_VALUE, t.current().value);
    }

    @Test void tokenizeNegativeIntegerLiterals() {
        JsonTokenizer t;

        t = new SimpleJsonTokenizer("-100");
        t.moveNext();
        assertEquals(Token.Type.INTEGER, t.current().type);
        assertEquals(-100, t.current().value);

        t = new SimpleJsonTokenizer("-50");
        t.moveNext();
        assertEquals(Token.Type.INTEGER, t.current().type);
        assertEquals(-50, t.current().value);

        t = new SimpleJsonTokenizer("-1");
        t.moveNext();
        assertEquals(Token.Type.INTEGER, t.current().type);
        assertEquals(-1, t.current().value);

        t = new SimpleJsonTokenizer("-0");
        t.moveNext();
        assertEquals(Token.Type.INTEGER, t.current().type);
        assertEquals(0, t.current().value);

        t = new SimpleJsonTokenizer(Integer.toString(Integer.MIN_VALUE));
        t.moveNext();
        assertEquals(Token.Type.INTEGER, t.current().type);
        assertEquals(Integer.MIN_VALUE, t.current().value);
    }

    @Test void tokenizeMultpleTokenWithWhitespace1() {
        var t = new SimpleJsonTokenizer("[ true ]");
        t.moveNext();
        assertEquals(Token.Type.LBRACKET, t.current().type);
        t.moveNext();
        assertEquals(Token.Type.TRUE, t.current().type);
        t.moveNext();
        assertEquals(Token.Type.RBRACKET, t.current().type);
    }

    @Test void tokenizeMultpleTokenWithWhitespace2() {
        var t = new SimpleJsonTokenizer("[ 1, true, \"foo\" ]");
        t.moveNext();
        assertEquals(Token.Type.LBRACKET, t.current().type);

        t.moveNext();
        assertEquals(Token.Type.INTEGER, t.current().type);
        assertEquals(1, t.current().value);

        t.moveNext();
        assertEquals(Token.Type.COMMA, t.current().type);

        t.moveNext();
        assertEquals(Token.Type.TRUE, t.current().type);

        t.moveNext();
        assertEquals(Token.Type.COMMA, t.current().type);

        t.moveNext();
        assertEquals(Token.Type.STRING, t.current().type);
        assertEquals("foo", t.current().value);

        t.moveNext();
        assertEquals(Token.Type.RBRACKET, t.current().type);
    }

    @Test void tokenizeTokenizeArrayLiteral() {
        var t = new SimpleJsonTokenizer("[ true, false ]");
        t.moveNext();
        assertEquals(Token.Type.LBRACKET, t.current().type);
        t.moveNext();
        assertEquals(Token.Type.TRUE, t.current().type);
        t.moveNext();
        assertEquals(Token.Type.COMMA, t.current().type);
        t.moveNext();
        assertEquals(Token.Type.FALSE, t.current().type);
        t.moveNext();
        assertEquals(Token.Type.RBRACKET, t.current().type);
    }

    @Test
    public void failToTokenizeTru() {
        assertThrows(TokenizerException.class, () -> {
            var t = new SimpleJsonTokenizer("tru");
            t.moveNext();
        });
    }
}
