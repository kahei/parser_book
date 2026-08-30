package com.github.asciidwango.parser_book.ch5;


import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ANTLRExpressionParserTest {
    private static CommonTokenStream streamFrom(String input) throws Exception {
        var antlrStream = new ANTLRInputStream(new StringReader(input));
        var lexer       = new ExpressionLexer(antlrStream);
        return new CommonTokenStream(lexer);
    }
    @DisplayName("ANTLRで1をパースした結果が1になることをテストする")
    @Test
    public void test1() throws Exception {
        var parser = new ExpressionParser(streamFrom("1"));
        assertEquals(1, parser.expression().e);
    }

    @DisplayName("ANTLRで(1 + 2) * 3をパースした結果が9になることをテストする")
    @Test
    public void test2() throws Exception {
        var parser = new ExpressionParser(streamFrom("(1 + 2) * 3"));
        assertEquals(9, parser.expression().e);
    }

    @DisplayName("ANTLRで(1 + 2) * 4 / 3をパースした結果が4になることをテストする")
    @Test
    public void test3() throws Exception {
        var parser = new ExpressionParser(streamFrom("(1 + 2) * 4 / 3"));
        assertEquals(4, parser.expression().e);
    }
}
