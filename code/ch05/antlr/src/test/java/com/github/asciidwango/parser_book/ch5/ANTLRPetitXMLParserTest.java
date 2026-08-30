package com.github.asciidwango.parser_book.ch5;

import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ANTLRPetitXMLParserTest {
private static CommonTokenStream streamFrom(String input) throws Exception {
        var antlrStream = new org.antlr.v4.runtime.ANTLRInputStream(new java.io.StringReader(input));
        var lexer       = new PetitXMLLexer(antlrStream);
        return new CommonTokenStream(lexer);
    }

    @DisplayName("ANTLRで<e/>をパースした結果がElement(e)になることをテストする")
    @Test
    public void test1() throws Exception {
        var parser = new PetitXMLParser(streamFrom("<e/>"));
        assertEquals(new PetitXML.Element("e"), parser.root().e);
    }

    @DisplayName("ANTLRで<e></e>をパースした結果がElement(e)になることをテストする")
    @Test
    public void test2() throws Exception {
        var parser = new PetitXMLParser(streamFrom("<e></e>"));
        assertEquals(new PetitXML.Element("e"), parser.root().e);
    }

    @DisplayName("ANTLRで<e><f/></e>をパースした結果がElement(e, Element(f))になることをテストする")
    @Test
    public void test3() throws Exception {
        var parser = new PetitXMLParser(streamFrom("<e><f/></e>"));
        assertEquals(new PetitXML.Element("e", java.util.List.of(new PetitXML.Element("f"))), parser.root().e);
    }
}
