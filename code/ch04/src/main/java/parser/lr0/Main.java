package parser.lr0;

import static java.util.List.of;

public class Main {
    public static void main(String[] args) {
        // 簡単な算術式の文法
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

        var recognizer = new LR0Recognizer(grammar);
        
        // 状態とテーブルを表示
        recognizer.printStates();
        recognizer.printTables();

        // テスト入力
        System.out.println("\n=== Testing: id + id * id ===");
        var input1 = of(
            new Expression.Terminal("id"),
            new Expression.Terminal("+"),
            new Expression.Terminal("id"),
            new Expression.Terminal("*"),
            new Expression.Terminal("id")
        );
        boolean result1 = recognizer.recognize(input1);
        System.out.println("Result: " + result1);

        System.out.println("\n=== Testing: ( id + id ) ===");
        var input2 = of(
            new Expression.Terminal("("),
            new Expression.Terminal("id"),
            new Expression.Terminal("+"),
            new Expression.Terminal("id"),
            new Expression.Terminal(")")
        );
        boolean result2 = recognizer.recognize(input2);
        System.out.println("Result: " + result2);

        System.out.println("\n=== Testing: id + + id (invalid) ===");
        var input3 = of(
            new Expression.Terminal("id"),
            new Expression.Terminal("+"),
            new Expression.Terminal("+"),
            new Expression.Terminal("id")
        );
        boolean result3 = recognizer.recognize(input3);
        System.out.println("Result: " + result3);
    }
}