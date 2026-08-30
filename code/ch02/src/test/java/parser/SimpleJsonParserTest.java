package parser;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimpleJsonParserTest {
    @Test
    public void parseSimpleNumber() {
        var parser = new SimpleJsonParser();
        var result = parser.parse("100");
        assertEquals(100.0, ((Ast.JsonNumber) result.value).value);
    }

    @Test
    public void parseSimpleString() {
        var parser = new SimpleJsonParser();
        var result = parser.parse("\"" + "hoge" + "\"");
        assertEquals("hoge", ((Ast.JsonString) result.value).value);
    }

    @Test
    public void parseBackEscapedString() {
        var parser = new SimpleJsonParser();
        var result = parser.parse("\"" + "\\b" + "\"");
        assertEquals("\b", ((Ast.JsonString) result.value).value);
    }

    @Test
    public void parseFormFeedEscapedString() {
        var parser = new SimpleJsonParser();
        var result = parser.parse("\"" + "\\f" + "\"");
        assertEquals("\f", ((Ast.JsonString) result.value).value);
    }

    @Test
    public void parseTabEscapedString() {
        var parser = new SimpleJsonParser();
        var result = parser.parse("\"" + "\\t" + "\"");
        assertEquals("\t", ((Ast.JsonString) result.value).value);
    }

    @Test
    public void parseNlEscapedString() {
        var parser = new SimpleJsonParser();
        var result = parser.parse("\"" + "\\n" + "\"");
        assertEquals("\n", ((Ast.JsonString) result.value).value);
    }

    @Test
    public void testLfEscapedString() {
        var parser = new SimpleJsonParser();
        var result = parser.parse("\""  + "\\r" + "\"");
        assertEquals("\r", ((Ast.JsonString) result.value).value);
    }

    @Test
    public void testBachSlashEscapedString() {
        var parser = new SimpleJsonParser();
        var result = parser.parse("\""  + "\\\\" + "\"");
        assertEquals("\\", ((Ast.JsonString) result.value).value);
    }

    @Test
    public void testSlashEscapedString() {
        var parser = new SimpleJsonParser();
        var result = parser.parse("\""  + "\\/" + "\"");
        assertEquals("/", ((Ast.JsonString) result.value).value);
    }

    @Test
    public void testDoubleQuoteEscapedString() {
        var parser = new SimpleJsonParser();
        var result = parser.parse("\""  + "\\\"" + "\"");
        assertEquals("\"", ((Ast.JsonString) result.value).value);
    }

    @Test
    public void testUnicodeEscapedString() {
        var parser = new SimpleJsonParser();
        var result = parser.parse("\""  + "\\u0041\\u0042\\u0043\\u006A" + "\"");
        assertEquals("ABCj", ((Ast.JsonString) result.value).value);
    }

    @Test
    public void parseTrue() {
        var parser = new SimpleJsonParser();
        var result = parser.parse("true");
        assertTrue(result.value instanceof Ast.JsonTrue);
    }

    @Test
    public void parseFalse() {
        var parser = new SimpleJsonParser();
        var result = parser.parse("false");
        assertTrue(result.value instanceof Ast.JsonFalse);
    }

    @Test
    public void parseNull() {
        var parser = new SimpleJsonParser();
        ParseResult<Ast.JsonValue> nullValue = parser.parse("null");
    }

    @Test
    public void parseEmptyArray() {
        var parser = new SimpleJsonParser();
        var result = parser.parse("[]");
        assertEquals(true, result.value instanceof Ast.JsonArray);
        var array = (Ast.JsonArray)result.value;
        assertEquals(List.of(), array.elements);
    }

    @Test
    public void parseOneElementArray() {
        var parser = new SimpleJsonParser();
        var result = parser.parse("[1]");
        assertEquals(true, result.value instanceof Ast.JsonArray);
        var array = (Ast.JsonArray)result.value;
        assertEquals(List.of(new Ast.JsonNumber(1)), array.elements);
    }

    @Test
    public void parseTwoElementArray() {
        var parser = new SimpleJsonParser();
        var result = parser.parse("[1, 2]");
        assertEquals(true, result.value instanceof Ast.JsonArray);
        var array = (Ast.JsonArray)result.value;
        assertEquals(List.of(new Ast.JsonNumber(1), new Ast.JsonNumber(2)), array.elements);
    }

    @Test
    public void parseEmptyObject() {
        var parser = new SimpleJsonParser();
        var result = parser.parse("{}");
        assertEquals(true, result.value instanceof Ast.JsonObject);
        var object = (Ast.JsonObject)result.value;
        assertEquals(List.of(), object.properties);
    }

    @Test
    public void parseOnePropertyObject() {
        var parser = new SimpleJsonParser();
        var result = parser.parse("{\"foo\":1}");
        assertEquals(true, result.value instanceof Ast.JsonObject);
        var object = (Ast.JsonObject)result.value;
        assertEquals(List.of(Pair.of(new Ast.JsonString("foo"), new Ast.JsonNumber(1))), object.properties);
    }

    @Test
    public void parseMultiPropertyObject() {
        var parser = new SimpleJsonParser();
        var result = parser.parse("{\"foo\":1,\"bar\":2}");
        assertEquals(true, result.value instanceof Ast.JsonObject);
        var object = (Ast.JsonObject)result.value;
        assertEquals(
            List.of(
                Pair.of(new Ast.JsonString("foo"), new Ast.JsonNumber(1)),
                Pair.of(new Ast.JsonString("bar"), new Ast.JsonNumber(2))
            ),
            object.properties
        );
    }
    
    @Test
    public void parseComplexObject() {
        var parser = new SimpleJsonParser();
        var result = parser.parse("{\"foo\" : 1, \"bar\" : { \"baz\" : 2}}");
        assertEquals(true, result.value instanceof Ast.JsonObject);
        var object = (Ast.JsonObject)result.value;
        assertEquals(
            new Ast.JsonObject(
                List.of(
                    Pair.of(new Ast.JsonString("foo"), new Ast.JsonNumber(1)),
                    Pair.of(
                        new Ast.JsonString("bar"),
                        new Ast.JsonObject(
                            List.of(Pair.of(new Ast.JsonString("baz"), new Ast.JsonNumber(2)))
                        )
                    )
                )
            ),
            object
        );
    }
}
