package com.github.kmizu.jcomb;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.github.kmizu.jcomb.JComb.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class JCombMathematicalExpressionTest {
    public JParser<Integer> expression() {
        /*
         * expression <- multitive ( ("+" / "-") multitive )*
         */
        return seq(
                lazy(() -> multitive()),
                rep0(
                        seq(
                                alt(string("+"), string("-")),
                                lazy(() -> multitive())
                        )
                )
        ).map(p -> {
            var left = p.a();
            var rights = p.b();
            for (var right : rights) {
                var op = right.a();
                var rightValue = right.b();
                if (op.equals("+")) {
                    left += rightValue;
                } else {
                    left -= rightValue;
                }
            }
            return left;
        });
    }

    public JParser<Integer> multitive() {
        /*
         * multitive <- primary ( ("*" / "/") primary )*
         */
        return seq(
                lazy(() -> primary()),
                rep0(
                        seq(
                                alt(string("*"), string("/")),
                                lazy(() -> primary())
                        )
                )
        ).map(p -> {
            var left = p.a();
            var rights = p.b();
            for (var right : rights) {
                var op = right.a();
                var rightValue = right.b();
                if (op.equals("*")) {
                    left *= rightValue;
                } else {
                    if (rightValue == 0)
                        throw new ArithmeticException("Division by zero");
                    left /= rightValue;
                }
            }
            return left;
        });
    }

    public JParser<Integer> primary() {
        /*
         * primary <- number / "(" expression ")"
         */
        return alt(
                number,
                seq(
                        string("("),
                        seq(
                            lazy(() -> expression()),
                            string(")")
                        )
                ).map(p -> p.b().a())
        );
    }

    // number <- [0-9]+
    JParser<Integer> number = regex("[0-9]+").map(Integer::parseInt);

    @Test
    @DisplayName("1+2*3")
    public void testExpression() {
        assertEquals(new Result<>(7, ""), expression().parse("1+2*3"));
    }
}