package com.github.kmizu.jcomb;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;
import java.util.function.*;

public class JComb {
    public static class JLiteralParser implements JParser<String> {
        private String literal;
        public JLiteralParser(String literal) {
            this.literal = literal;

        }
        public Result<String> parse(String input) {
            if(input.startsWith(literal)) {
                return new Result<String>(literal, input.substring(literal.length()));
            } else {
                return null;
            }
        }
    }

    public static JParser<String> string(String literal) {
        return new JLiteralParser(literal);
    }

    public static <A> JParser<A> alt(JParser<A> p1, JParser<A> p2) {
        return (input) -> {
            var result = p1.parse(input);
            if(result != null) return result;
            return p2.parse(input);
        };
    }

    public static <A> JParser<List<A>> rep0(JParser<A> p) {
        return (input) -> {
            var result = p.parse(input);
            if(result == null) return new Result<>(List.of(), input);
            var value = result.value();
            var rest = result.rest();
            var result2 = rep0(p).parse(rest);
            if(result2 == null) return new Result<>(List.of(value), rest);
            List<A> values = new ArrayList<>();
            values.add(value);
            values.addAll(result2.value());
            return new Result<>(values, result2.rest());
        };
    }

    public static <A> JParser<List<A>> rep1(JParser<A> p) {
        return (input) -> {
            var result = p.parse(input);
            if(result == null) return null;
            var value = result.value();
            var rest = result.rest();
            var result2 = rep0(p).parse(rest);
            if(result2 == null) return new Result<>(List.of(value), rest);
            List<A> values = new ArrayList<>();
            values.add(value);
            values.addAll(result2.value());
            return new Result<>(values, result2.rest());
        };
    }

    public static <A, B> JParser<Pair<A, B>> seq(JParser<A> p1, JParser<B> p2) {
        return (input) -> {
            var result1 = p1.parse(input);
            if(result1 == null) return null;
            var rest = result1.rest();
            var result2 = p2.parse(rest);
            if(result2 == null) return null;
            return new Result<>(new Pair<A, B>(result1.value(), result2.value()), result2.rest());
        };
    }

    public static JParser<String> regex(String regex) {
        return (input) -> {
            var matcher = Pattern.compile(regex).matcher(input);
            if(matcher.lookingAt()) {
                return new Result<>(matcher.group(), input.substring(matcher.end()));
            } else {
                return null;
            }
        };
    }

    public static <A> JParser<A> lazy(Supplier<JParser<A>> supplier) {
        return (input) -> supplier.get().parse(input);
    }
}
