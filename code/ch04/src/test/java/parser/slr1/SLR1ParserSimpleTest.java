package parser.slr1;

import org.junit.jupiter.api.Test;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.*;

class SLR1ParserSimpleTest {
    
    @Test
    void testVerySimpleGrammar() {
        // 非常に簡単な文法
        // S -> a
        var grammar = new Grammar("S", of(
            new Rule("S", of(new Expression.Terminal("a")))
        ));
        
        var parser = new SLR1Parser(grammar);
        
        // "a" を認識
        assertTrue(parser.parse(of(
            new Expression.Terminal("a")
        )));
        
        // "b" は認識しない
        assertFalse(parser.parse(of(
            new Expression.Terminal("b")
        )));
        
        // 空は認識しない
        assertFalse(parser.parse(of()));
    }
    
    @Test
    void testSimpleSequence() {
        // S -> a b
        var grammar = new Grammar("S", of(
            new Rule("S", of(
                new Expression.Terminal("a"),
                new Expression.Terminal("b")
            ))
        ));
        
        var parser = new SLR1Parser(grammar);
        
        // "a b" を認識
        assertTrue(parser.parse(of(
            new Expression.Terminal("a"),
            new Expression.Terminal("b")
        )));
        
        // "a" だけは認識しない
        assertFalse(parser.parse(of(
            new Expression.Terminal("a")
        )));
        
        // "b" だけは認識しない
        assertFalse(parser.parse(of(
            new Expression.Terminal("b")
        )));
    }
    
    @Test
    void testSimpleChoice() {
        // S -> a | b
        var grammar = new Grammar("S", of(
            new Rule("S", of(new Expression.Terminal("a"))),
            new Rule("S", of(new Expression.Terminal("b")))
        ));
        
        var parser = new SLR1Parser(grammar);
        
        // "a" を認識
        assertTrue(parser.parse(of(
            new Expression.Terminal("a")
        )));
        
        // "b" を認識
        assertTrue(parser.parse(of(
            new Expression.Terminal("b")
        )));
        
        // "c" は認識しない
        assertFalse(parser.parse(of(
            new Expression.Terminal("c")
        )));
    }
}