package parser.ll1;

import static java.util.List.of;

public class Main {
    public static void main(String[] args) {
        // LL(1)文法の例
        // S -> A B
        // A -> a A | ε
        // B -> b
        var grammar1 = new Grammar("S", of(
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
        
        System.out.println("=== Grammar 1: S -> AB, A -> aA | ε, B -> b ===");
        var recognizer1 = new LL1Recognizer(grammar1);
        recognizer1.printFirstSets();
        recognizer1.printFollowSets();
        recognizer1.printParseTable();
        
        System.out.println("\n=== Testing: aab ===");
        var input1 = of(
            new Expression.Terminal("a"),
            new Expression.Terminal("a"),
            new Expression.Terminal("b")
        );
        recognizer1.recognize(input1);
        
        System.out.println("\n=== Testing: b ===");
        var input2 = of(new Expression.Terminal("b"));
        recognizer1.recognize(input2);
        
        System.out.println("\n=== Testing: aa (invalid - missing b) ===");
        var input3 = of(
            new Expression.Terminal("a"),
            new Expression.Terminal("a")
        );
        recognizer1.recognize(input3);
        
        // より複雑なLL(1)文法の例
        // E -> T E'
        // E' -> + T E' | ε
        // T -> F T'
        // T' -> * F T' | ε
        // F -> ( E ) | id
        var grammar2 = new Grammar("E", of(
            new Rule("E", of(
                new Expression.NonTerminal("T"),
                new Expression.NonTerminal("E'")
            )),
            new Rule("E'", of(
                new Expression.Terminal("+"),
                new Expression.NonTerminal("T"),
                new Expression.NonTerminal("E'")
            )),
            new Rule("E'", of()),  // 空規則
            new Rule("T", of(
                new Expression.NonTerminal("F"),
                new Expression.NonTerminal("T'")
            )),
            new Rule("T'", of(
                new Expression.Terminal("*"),
                new Expression.NonTerminal("F"),
                new Expression.NonTerminal("T'")
            )),
            new Rule("T'", of()),  // 空規則
            new Rule("F", of(
                new Expression.Terminal("("),
                new Expression.NonTerminal("E"),
                new Expression.Terminal(")")
            )),
            new Rule("F", of(new Expression.Terminal("id")))
        ));
        
        System.out.println("\n\n=== Grammar 2: LL(1) version of arithmetic expressions ===");
        var recognizer2 = new LL1Recognizer(grammar2);
        recognizer2.printFirstSets();
        recognizer2.printFollowSets();
        recognizer2.printParseTable();
        
        System.out.println("\n=== Testing: id + id * id ===");
        var input4 = of(
            new Expression.Terminal("id"),
            new Expression.Terminal("+"),
            new Expression.Terminal("id"),
            new Expression.Terminal("*"),
            new Expression.Terminal("id")
        );
        recognizer2.recognize(input4);
        
        System.out.println("\n=== Testing: ( id + id ) ===");
        var input5 = of(
            new Expression.Terminal("("),
            new Expression.Terminal("id"),
            new Expression.Terminal("+"),
            new Expression.Terminal("id"),
            new Expression.Terminal(")")
        );
        recognizer2.recognize(input5);
    }
}