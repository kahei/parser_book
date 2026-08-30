package parser.ll1;

import org.junit.jupiter.api.Test;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.*;

class LL1RecognizerTest {
    
    @Test
    void testSimpleGrammar() {
        // S -> A B
        // A -> a A | ε
        // B -> b
        var grammar = new Grammar("S", of(
            new Rule("S", of(
                new Expression.NonTerminal("A"),
                new Expression.NonTerminal("B")
            )),
            new Rule("A", of(
                new Expression.Terminal("a"),
                new Expression.NonTerminal("A")
            )),
            new Rule("A", of()),  // 空規則
            new Rule("B", of(new Expression.Terminal("b")))
        ));
        
        var recognizer = new LL1Recognizer(grammar);
        
        // "aab" を認識
        assertTrue(recognizer.recognize(of(
            new Expression.Terminal("a"),
            new Expression.Terminal("a"),
            new Expression.Terminal("b")
        )));
        
        // "b" を認識
        assertTrue(recognizer.recognize(of(
            new Expression.Terminal("b")
        )));
        
        // "aaab" を認識
        assertTrue(recognizer.recognize(of(
            new Expression.Terminal("a"),
            new Expression.Terminal("a"),
            new Expression.Terminal("a"),
            new Expression.Terminal("b")
        )));
        
        // "aa" は認識しない（bが必要）
        assertFalse(recognizer.recognize(of(
            new Expression.Terminal("a"),
            new Expression.Terminal("a")
        )));
        
        // "ba" は認識しない（順序が違う）
        assertFalse(recognizer.recognize(of(
            new Expression.Terminal("b"),
            new Expression.Terminal("a")
        )));
    }
    
    @Test
    void testArithmeticGrammar() {
        // LL(1)版の算術式文法
        // E -> T E'
        // E' -> + T E' | ε
        // T -> F T'
        // T' -> * F T' | ε
        // F -> ( E ) | id
        var grammar = new Grammar("E", of(
            new Rule("E", of(
                new Expression.NonTerminal("T"),
                new Expression.NonTerminal("E'")
            )),
            new Rule("E'", of(
                new Expression.Terminal("+"),
                new Expression.NonTerminal("T"),
                new Expression.NonTerminal("E'")
            )),
            new Rule("E'", of()),
            new Rule("T", of(
                new Expression.NonTerminal("F"),
                new Expression.NonTerminal("T'")
            )),
            new Rule("T'", of(
                new Expression.Terminal("*"),
                new Expression.NonTerminal("F"),
                new Expression.NonTerminal("T'")
            )),
            new Rule("T'", of()),
            new Rule("F", of(
                new Expression.Terminal("("),
                new Expression.NonTerminal("E"),
                new Expression.Terminal(")")
            )),
            new Rule("F", of(new Expression.Terminal("id")))
        ));
        
        var recognizer = new LL1Recognizer(grammar);
        
        // "id" を認識
        assertTrue(recognizer.recognize(of(
            new Expression.Terminal("id")
        )));
        
        // "id + id" を認識
        assertTrue(recognizer.recognize(of(
            new Expression.Terminal("id"),
            new Expression.Terminal("+"),
            new Expression.Terminal("id")
        )));
        
        // "id * id" を認識
        assertTrue(recognizer.recognize(of(
            new Expression.Terminal("id"),
            new Expression.Terminal("*"),
            new Expression.Terminal("id")
        )));
        
        // "id + id * id" を認識
        assertTrue(recognizer.recognize(of(
            new Expression.Terminal("id"),
            new Expression.Terminal("+"),
            new Expression.Terminal("id"),
            new Expression.Terminal("*"),
            new Expression.Terminal("id")
        )));
        
        // "(id + id)" を認識
        assertTrue(recognizer.recognize(of(
            new Expression.Terminal("("),
            new Expression.Terminal("id"),
            new Expression.Terminal("+"),
            new Expression.Terminal("id"),
            new Expression.Terminal(")")
        )));
        
        // "((id))" を認識
        assertTrue(recognizer.recognize(of(
            new Expression.Terminal("("),
            new Expression.Terminal("("),
            new Expression.Terminal("id"),
            new Expression.Terminal(")"),
            new Expression.Terminal(")")
        )));
        
        // "+ id" は認識しない（不正）
        assertFalse(recognizer.recognize(of(
            new Expression.Terminal("+"),
            new Expression.Terminal("id")
        )));
        
        // "id +" は認識しない（不完全）
        assertFalse(recognizer.recognize(of(
            new Expression.Terminal("id"),
            new Expression.Terminal("+")
        )));
        
        // "(id" は認識しない（括弧が閉じていない）
        assertFalse(recognizer.recognize(of(
            new Expression.Terminal("("),
            new Expression.Terminal("id")
        )));
    }
    
    @Test
    void testListGrammar() {
        // リスト文法（LL(1)版）
        // L -> E L'
        // L' -> , E L' | ε
        // E -> id
        var grammar = new Grammar("L", of(
            new Rule("L", of(
                new Expression.NonTerminal("E"),
                new Expression.NonTerminal("L'")
            )),
            new Rule("L'", of(
                new Expression.Terminal(","),
                new Expression.NonTerminal("E"),
                new Expression.NonTerminal("L'")
            )),
            new Rule("L'", of()),  // 空規則
            new Rule("E", of(new Expression.Terminal("id")))
        ));
        
        var recognizer = new LL1Recognizer(grammar);
        
        // "id" を認識
        assertTrue(recognizer.recognize(of(
            new Expression.Terminal("id")
        )));
        
        // "id, id" を認識
        assertTrue(recognizer.recognize(of(
            new Expression.Terminal("id"),
            new Expression.Terminal(","),
            new Expression.Terminal("id")
        )));
        
        // "id, id, id" を認識
        assertTrue(recognizer.recognize(of(
            new Expression.Terminal("id"),
            new Expression.Terminal(","),
            new Expression.Terminal("id"),
            new Expression.Terminal(","),
            new Expression.Terminal("id")
        )));
        
        // ", id" は認識しない（不正）
        assertFalse(recognizer.recognize(of(
            new Expression.Terminal(","),
            new Expression.Terminal("id")
        )));
        
        // "id," は認識しない（不完全）
        assertFalse(recognizer.recognize(of(
            new Expression.Terminal("id"),
            new Expression.Terminal(",")
        )));
    }
    
    @Test
    void testBalancedParentheses() {
        // バランスの取れた括弧の文法
        // S -> ( S ) S | ε
        var grammar = new Grammar("S", of(
            new Rule("S", of(
                new Expression.Terminal("("),
                new Expression.NonTerminal("S"),
                new Expression.Terminal(")"),
                new Expression.NonTerminal("S")
            )),
            new Rule("S", of())  // 空規則
        ));
        
        var recognizer = new LL1Recognizer(grammar);
        
        // 空文字列を認識
        assertTrue(recognizer.recognize(of()));
        
        // "()" を認識
        assertTrue(recognizer.recognize(of(
            new Expression.Terminal("("),
            new Expression.Terminal(")")
        )));
        
        // "()()" を認識
        assertTrue(recognizer.recognize(of(
            new Expression.Terminal("("),
            new Expression.Terminal(")"),
            new Expression.Terminal("("),
            new Expression.Terminal(")")
        )));
        
        // "(())" を認識
        assertTrue(recognizer.recognize(of(
            new Expression.Terminal("("),
            new Expression.Terminal("("),
            new Expression.Terminal(")"),
            new Expression.Terminal(")")
        )));
        
        // "((()))" を認識
        assertTrue(recognizer.recognize(of(
            new Expression.Terminal("("),
            new Expression.Terminal("("),
            new Expression.Terminal("("),
            new Expression.Terminal(")"),
            new Expression.Terminal(")"),
            new Expression.Terminal(")")
        )));
        
        // "(()" は認識しない（不正）
        assertFalse(recognizer.recognize(of(
            new Expression.Terminal("("),
            new Expression.Terminal("("),
            new Expression.Terminal(")")
        )));
        
        // "())" は認識しない（不正）
        assertFalse(recognizer.recognize(of(
            new Expression.Terminal("("),
            new Expression.Terminal(")"),
            new Expression.Terminal(")")
        )));
    }
}