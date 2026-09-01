package com.github.kmizu.peg2java;

import static com.github.kmizu.peg2java.PEG2Java.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class PEG2JavaTest {
    @Test
    public void generatesTheParserShownInTheBook() {
        // D <- P;
        // P <- "(" P ")" P / "";
        List<Rule> rules = Arrays.asList(
            new Rule("D", new NT("P")),
            new Rule("P",
                new Choice(
                    new Seq(
                        new Lit("("),
                        new NT("P"),
                        new Lit(")"),
                        new NT("P")
                    ),
                    // 空文字列 ε（イプシロン）
                    new Lit("")
                 )
            )
        );

        String expected = """
            public class Parser {
                public static class Failure extends RuntimeException {
                    public Failure(String message) {
                        super(message);
                    }
                }
                private String input;
                private int pos;
                public Parser(String input) {
                    this.input = input;
                    this.pos = 0;
                }
                public void match(String str) {
                    if (pos + str.length() <= input.length() &&
                        input.startsWith(str, pos)) {
                        pos += str.length();
                        return;
                    }
                    throw new Failure("Expected '" + str + "' at pos " + pos);
                }

                public void parseD() {
                    parseP();
                    return;
                }

                public void parseP() {
                    int saved = pos;
                    try {
                        match("(");
                        parseP();
                        match(")");
                        parseP();
                    } catch (Failure e) {
                        pos = saved;
                        match("");
                    }
                    return;
                }
            }
            """;

        assertEquals(expected, PEG2Java.generateParser(rules));
    }
}
