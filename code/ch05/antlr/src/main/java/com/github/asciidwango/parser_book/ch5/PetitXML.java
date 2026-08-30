package com.github.asciidwango.parser_book.ch5;

import java.util.List;

public class PetitXML {
    public static record Element(String name, List<Element> children) {
        public Element(String name) { this(name, List.of()); }
    }
}
