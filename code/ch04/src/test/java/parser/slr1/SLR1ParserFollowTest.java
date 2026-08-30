package parser.slr1;

import org.junit.jupiter.api.Test;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.*;

class SLR1ParserFollowTest {
    
    @Test
    void testSimpleFollowSets() {
        // E -> E + T | T
        // T -> T * id | id
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
                new Expression.Terminal("id")
            )),
            new Rule("T", of(new Expression.Terminal("id")))
        ));
        
        var parser = new SLR1Parser(grammar);
        
        // FOLLOWセットをデバッグ出力
        System.out.println("\n=== FOLLOW Sets Debug ===");
        var followSets = parser.getFollowSets(); // このメソッドを追加する必要がある
        for (var entry : followSets.entrySet()) {
            System.out.println("FOLLOW(" + entry.getKey() + ") = " + entry.getValue());
        }
        
        // "id * id + id" を認識できるはず
        assertTrue(parser.parse(of(
            new Expression.Terminal("id"),
            new Expression.Terminal("*"),
            new Expression.Terminal("id"),
            new Expression.Terminal("+"),
            new Expression.Terminal("id")
        )));
    }
}