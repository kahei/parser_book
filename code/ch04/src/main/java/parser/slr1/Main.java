package parser.slr1;

import java.util.ArrayList;
import java.util.List;
import static java.util.List.of;
import static parser.slr1.Expression.*;

public class Main {
    private static List<Expression> toExpressions(String input) {
        List<Expression> result = new ArrayList<>();
        for(int i = 0; i < input.length(); i++) {
            result.add(new Expression.Terminal(input.substring(i, i + 1)));
        }
        return result;
    }
    public static void main(String[] args) {
        Grammar grammar = new Grammar("E", List.of(
                new Rule("E", of(new NonTerminal("E"), new Terminal("+"), new NonTerminal("T"))),
                new Rule("E", of(new NonTerminal("T"))),
                new Rule("T", of(new NonTerminal("T"), new Terminal("*"), new NonTerminal("F"))),
                new Rule("T", of(new NonTerminal("F"))),
                new Rule("F", of(new Terminal("("), new NonTerminal("E"), new Terminal(")"))),
                new Rule("F", of(new Terminal("x")))
        ));
        /*
        Grammar grammar = new Grammar(
                "E",
                List.of(
                        // E := T E
                        new Rule("E", of(new NonTerminal("T"), new NonTerminal("E'"))),
                        new Rule("E'", of(new Terminal("+"), new NonTerminal("T"), new NonTerminal("E'"))),
                        new Rule("E'", of()),  // ε (空生成)
                        new Rule("T", of(new NonTerminal("F"), new NonTerminal("T'"))),
                        new Rule("T'", of(new Terminal("*"), new NonTerminal("F"), new NonTerminal("T'"))),
                        new Rule("T'", of()),  // ε (空生成)
                        new Rule("F", of(new Terminal("("), new NonTerminal("E"), new Terminal(")"))),
                        new Rule("F", of(new Terminal("x")))
                )
        );
         */

        SLR1Parser parser = new SLR1Parser(grammar);
        boolean result = parser.parse(toExpressions("((x+(x*x)))"));

        System.out.println("Parsing result: " + result);
    }
}