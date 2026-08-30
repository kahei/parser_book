package com.github.kmizu.jcomb;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.io.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.github.kmizu.jcomb.JComb.*;

public class JCombTest {
    @Test
    @DisplayName("f")
    public void testEmpty() throws Exception {
        JParser<String> foo = string("");
        assertEquals(new Result<>("", "f" +
                ""), foo.parse("f")); // Result<String>("f", "")
    }
    @Test
    @DisplayName("f")
    public void testF() throws Exception {
        JParser<String> foo = string("f");
        assertEquals(new Result<>("f", ""), foo.parse("f")); // Result<String>("f", "")
    }
    @Test
    @DisplayName("foo")
    public void testFoo() throws Exception {
        JParser<String> foo = string("foo");
        assertEquals(new Result<>("foo", "_bar"), foo.parse("foo_bar")); // Result<String>("foo", "_bar")
    }

    @Test
    @DisplayName("a / b")
    public void testAltAB() throws Exception {
        JParser<String> a = string("a");
        JParser<String> b = string("b");
        JParser<String> ab = alt(a, b);
        assertEquals(new Result<>("a", ""), ab.parse("a")); // Result<String>("a", "")
        assertEquals(new Result<>("b", ""), ab.parse("b")); // Result<String>("b", "")
    }

    @Test
    @DisplayName("a b")
    public void testSeqAB() throws Exception {
        JParser<Pair<String, String>> ab = seq(string("a"), string("b"));
        assertEquals(new Result<>(new Pair<>("a", "b"), ""), ab.parse("ab")); // Result<Pair<String, String>>(new Pair<>("a", "b"), "")
    }

    @Test
    @DisplayName("1000")
    public void testRegex() throws Exception {
        JParser<String> regex = regex("[0-9]+");
        assertEquals(new Result<>("1000", ""), regex.parse("1000")); // Result<String>("1000", "")
    }

    @Test
    @DisplayName("1000")
    public void testMap() throws Exception {
        JParser<Integer> regex = regex("[0-9]+").map(Integer::parseInt);
        assertEquals(new Result<>(1000, ""), regex.parse("1000")); // Result<Integer>(1000, "")
    }
}
