package parser.lr0;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.*;

class LR0RecognizerTest {
    private LR0Recognizer recognizer;

    @BeforeEach
    void setUp() {
        // 簡単な算術式の文法（LR(0)文法）
        // S -> A a | B b
        // A -> c
        // B -> c
        // この文法はSLR(1)では解析可能だが、LR(0)では競合が発生する
        var grammar = new Grammar("S", of(
            new Rule("S", of(
                new Expression.NonTerminal("A"),
                new Expression.Terminal("a")
            )),
            new Rule("S", of(
                new Expression.NonTerminal("B"),
                new Expression.Terminal("b")
            )),
            new Rule("A", of(new Expression.Terminal("c"))),
            new Rule("B", of(new Expression.Terminal("c")))
        ));
        recognizer = new LR0Recognizer(grammar);
    }

    @Test
    void testSimpleLR0Grammar() {
        // より単純なLR(0)文法でテスト
        // S -> a S b | c
        var simpleLR0Grammar = new Grammar("S", of(
            new Rule("S", of(
                new Expression.Terminal("a"),
                new Expression.NonTerminal("S"),
                new Expression.Terminal("b")
            )),
            new Rule("S", of(new Expression.Terminal("c")))
        ));
        
        var simpleRecognizer = new LR0Recognizer(simpleLR0Grammar);
        
        // "c" を認識
        assertTrue(simpleRecognizer.recognize(of(
            new Expression.Terminal("c")
        )));
        
        // "acb" を認識
        assertTrue(simpleRecognizer.recognize(of(
            new Expression.Terminal("a"),
            new Expression.Terminal("c"),
            new Expression.Terminal("b")
        )));
        
        // "aacbb" を認識
        assertTrue(simpleRecognizer.recognize(of(
            new Expression.Terminal("a"),
            new Expression.Terminal("a"),
            new Expression.Terminal("c"),
            new Expression.Terminal("b"),
            new Expression.Terminal("b")
        )));
        
        // "ab" は認識しない（不正）
        assertFalse(simpleRecognizer.recognize(of(
            new Expression.Terminal("a"),
            new Expression.Terminal("b")
        )));
    }

    @Test
    void testParenthesesGrammar() {
        // バランスの取れた括弧の文法
        // S -> ( S ) | ε
        var parenGrammar = new Grammar("S", of(
            new Rule("S", of(
                new Expression.Terminal("("),
                new Expression.NonTerminal("S"),
                new Expression.Terminal(")")
            )),
            new Rule("S", of())  // 空規則
        ));
        
        var parenRecognizer = new LR0Recognizer(parenGrammar);
        
        // 空文字列を認識
        assertTrue(parenRecognizer.recognize(of()));
        
        // "()" を認識
        assertTrue(parenRecognizer.recognize(of(
            new Expression.Terminal("("),
            new Expression.Terminal(")")
        )));
        
        // "(())" を認識
        assertTrue(parenRecognizer.recognize(of(
            new Expression.Terminal("("),
            new Expression.Terminal("("),
            new Expression.Terminal(")"),
            new Expression.Terminal(")")
        )));
        
        // "(()" は認識しない（不正）
        assertFalse(parenRecognizer.recognize(of(
            new Expression.Terminal("("),
            new Expression.Terminal("("),
            new Expression.Terminal(")")
        )));
    }

    @Test
    void testListGrammar() {
        // リスト文法
        // L -> L , E | E
        // E -> id
        var listGrammar = new Grammar("L", of(
            new Rule("L", of(
                new Expression.NonTerminal("L"),
                new Expression.Terminal(","),
                new Expression.NonTerminal("E")
            )),
            new Rule("L", of(new Expression.NonTerminal("E"))),
            new Rule("E", of(new Expression.Terminal("id")))
        ));
        
        var listRecognizer = new LR0Recognizer(listGrammar);
        
        // "id" を認識
        assertTrue(listRecognizer.recognize(of(
            new Expression.Terminal("id")
        )));
        
        // "id, id" を認識
        assertTrue(listRecognizer.recognize(of(
            new Expression.Terminal("id"),
            new Expression.Terminal(","),
            new Expression.Terminal("id")
        )));
        
        // "id, id, id" を認識
        assertTrue(listRecognizer.recognize(of(
            new Expression.Terminal("id"),
            new Expression.Terminal(","),
            new Expression.Terminal("id"),
            new Expression.Terminal(","),
            new Expression.Terminal("id")
        )));
        
        // ", id" は認識しない（不正）
        assertFalse(listRecognizer.recognize(of(
            new Expression.Terminal(","),
            new Expression.Terminal("id")
        )));
    }
}