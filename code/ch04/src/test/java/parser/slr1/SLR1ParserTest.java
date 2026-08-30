package parser.slr1;

import org.junit.jupiter.api.Test;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.*;

class SLR1ParserTest {
    
    @Test
    void testArithmeticGrammar() {
        // 算術式の文法（3レベルの優先順位：括弧、*、+）
        // E -> E + T | T
        // T -> T * F | F
        // F -> ( E ) | id
        var grammar = new Grammar("E", of(
            new Rule("E", of(
                new Expression.NonTerminal("E"),
                new Expression.Terminal("+"),
                new Expression.NonTerminal("T")
            )),
            new Rule("E", of(new Expression.NonTerminal("T"))),
            new Rule("T", of(
                new Expression.NonTerminal("T"),
                new Expression.Terminal("*"),
                new Expression.NonTerminal("F")
            )),
            new Rule("T", of(new Expression.NonTerminal("F"))),
            new Rule("F", of(
                new Expression.Terminal("("),
                new Expression.NonTerminal("E"),
                new Expression.Terminal(")")
            )),
            new Rule("F", of(new Expression.Terminal("id")))
        ));
        
        var parser = new SLR1Parser(grammar);
        
        // "id" を認識
        assertTrue(parser.parse(of(
            new Expression.Terminal("id")
        )));
        
        // "id + id" を認識
        assertTrue(parser.parse(of(
            new Expression.Terminal("id"),
            new Expression.Terminal("+"),
            new Expression.Terminal("id")
        )));
        
        // "id * id" を認識
        assertTrue(parser.parse(of(
            new Expression.Terminal("id"),
            new Expression.Terminal("*"),
            new Expression.Terminal("id")
        )));
        
        // "id + id * id" を認識（優先順位: id + (id * id)）
        assertTrue(parser.parse(of(
            new Expression.Terminal("id"),
            new Expression.Terminal("+"),
            new Expression.Terminal("id"),
            new Expression.Terminal("*"),
            new Expression.Terminal("id")
        )));
        
        // "id * id + id" を認識（*が優先される: (id * id) + id）
        assertTrue(parser.parse(of(
            new Expression.Terminal("id"),
            new Expression.Terminal("*"),
            new Expression.Terminal("id"),
            new Expression.Terminal("+"),
            new Expression.Terminal("id")
        )));
        
        // "(id + id) * id" を認識（括弧が優先される）
        assertTrue(parser.parse(of(
            new Expression.Terminal("("),
            new Expression.Terminal("id"),
            new Expression.Terminal("+"),
            new Expression.Terminal("id"),
            new Expression.Terminal(")"),
            new Expression.Terminal("*"),
            new Expression.Terminal("id")
        )));
        
        // "((id))" を認識（ネストした括弧）
        assertTrue(parser.parse(of(
            new Expression.Terminal("("),
            new Expression.Terminal("("),
            new Expression.Terminal("id"),
            new Expression.Terminal(")"),
            new Expression.Terminal(")")
        )));
        
        // "+ id" は認識しない（不正）
        assertFalse(parser.parse(of(
            new Expression.Terminal("+"),
            new Expression.Terminal("id")
        )));
        
        // "id +" は認識しない（不完全）
        assertFalse(parser.parse(of(
            new Expression.Terminal("id"),
            new Expression.Terminal("+")
        )));
        
        // "(id" は認識しない（括弧が閉じていない）
        assertFalse(parser.parse(of(
            new Expression.Terminal("("),
            new Expression.Terminal("id")
        )));
        
        // "id)" は認識しない（括弧が対応していない）
        assertFalse(parser.parse(of(
            new Expression.Terminal("id"),
            new Expression.Terminal(")")
        )));
    }
    
    @Test
    void testListGrammar() {
        // リスト文法（左再帰）
        // L -> L , E | E
        // E -> id
        var grammar = new Grammar("L", of(
            new Rule("L", of(
                new Expression.NonTerminal("L"),
                new Expression.Terminal(","),
                new Expression.NonTerminal("E")
            )),
            new Rule("L", of(new Expression.NonTerminal("E"))),
            new Rule("E", of(new Expression.Terminal("id")))
        ));
        
        var parser = new SLR1Parser(grammar);
        
        // "id" を認識
        assertTrue(parser.parse(of(
            new Expression.Terminal("id")
        )));
        
        // "id, id" を認識
        assertTrue(parser.parse(of(
            new Expression.Terminal("id"),
            new Expression.Terminal(","),
            new Expression.Terminal("id")
        )));
        
        // "id, id, id" を認識
        assertTrue(parser.parse(of(
            new Expression.Terminal("id"),
            new Expression.Terminal(","),
            new Expression.Terminal("id"),
            new Expression.Terminal(","),
            new Expression.Terminal("id")
        )));
        
        // ", id" は認識しない（不正）
        assertFalse(parser.parse(of(
            new Expression.Terminal(","),
            new Expression.Terminal("id")
        )));
        
        // "id," は認識しない（不完全）
        assertFalse(parser.parse(of(
            new Expression.Terminal("id"),
            new Expression.Terminal(",")
        )));
        
        // "id id" は認識しない（カンマがない）
        assertFalse(parser.parse(of(
            new Expression.Terminal("id"),
            new Expression.Terminal("id")
        )));
    }
    
    @Test
    void testSimpleAssignmentGrammar() {
        // より単純な代入文の文法
        // S -> id = E | E
        // E -> E + T | T
        // T -> id
        var grammar = new Grammar("S", of(
            new Rule("S", of(
                new Expression.Terminal("id"),
                new Expression.Terminal("="),
                new Expression.NonTerminal("E")
            )),
            new Rule("S", of(new Expression.NonTerminal("E"))),
            new Rule("E", of(
                new Expression.NonTerminal("E"),
                new Expression.Terminal("+"),
                new Expression.NonTerminal("T")
            )),
            new Rule("E", of(new Expression.NonTerminal("T"))),
            new Rule("T", of(new Expression.Terminal("id")))
        ));
        
        var parser = new SLR1Parser(grammar);
        
        // "id" を認識
        assertTrue(parser.parse(of(
            new Expression.Terminal("id")
        )));
        
        // "id = id" を認識
        assertTrue(parser.parse(of(
            new Expression.Terminal("id"),
            new Expression.Terminal("="),
            new Expression.Terminal("id")
        )));
        
        // "id = id + id" を認識
        assertTrue(parser.parse(of(
            new Expression.Terminal("id"),
            new Expression.Terminal("="),
            new Expression.Terminal("id"),
            new Expression.Terminal("+"),
            new Expression.Terminal("id")
        )));
        
        // "id + id" を認識
        assertTrue(parser.parse(of(
            new Expression.Terminal("id"),
            new Expression.Terminal("+"),
            new Expression.Terminal("id")
        )));
        
        // "= id" は認識しない（不正）
        assertFalse(parser.parse(of(
            new Expression.Terminal("="),
            new Expression.Terminal("id")
        )));
        
        // "id =" は認識しない（不完全）
        assertFalse(parser.parse(of(
            new Expression.Terminal("id"),
            new Expression.Terminal("=")
        )));
    }
    
    @Test
    void testBalancedParentheses() {
        // バランスの取れた括弧の文法（SLR(1)版）
        // S -> S S | ( S ) | ε
        // 注：この文法は実際にはSLR(1)では扱えないため、競合が発生する
        var grammar = new Grammar("S", of(
            new Rule("S", of(
                new Expression.NonTerminal("S"),
                new Expression.NonTerminal("S")
            )),
            new Rule("S", of(
                new Expression.Terminal("("),
                new Expression.NonTerminal("S"),
                new Expression.Terminal(")")
            )),
            new Rule("S", of())  // 空規則
        ));
        
        // SLR(1)パーサーの構築時に競合が検出されることを確認
        assertThrows(IllegalStateException.class, () -> {
            new SLR1Parser(grammar);
        });
    }
    
    @Test
    void testConflictDetection() {
        // シフト/リデュース競合を含む文法
        // S -> if E then S | if E then S else S | a
        // E -> b
        var grammar = new Grammar("S", of(
            new Rule("S", of(
                new Expression.Terminal("if"),
                new Expression.NonTerminal("E"),
                new Expression.Terminal("then"),
                new Expression.NonTerminal("S")
            )),
            new Rule("S", of(
                new Expression.Terminal("if"),
                new Expression.NonTerminal("E"),
                new Expression.Terminal("then"),
                new Expression.NonTerminal("S"),
                new Expression.Terminal("else"),
                new Expression.NonTerminal("S")
            )),
            new Rule("S", of(new Expression.Terminal("a"))),
            new Rule("E", of(new Expression.Terminal("b")))
        ));
        
        // Dangling else問題は典型的なshift/reduce競合を引き起こす
        assertThrows(IllegalStateException.class, () -> {
            new SLR1Parser(grammar);
        });
    }
}