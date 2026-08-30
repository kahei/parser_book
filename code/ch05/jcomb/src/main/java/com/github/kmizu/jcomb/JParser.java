package com.github.kmizu.jcomb;
import java.util.function.*;

interface JParser<R> {
    Result<R> parse(String input);

    default <T> JParser<T> map(Function<R, T> f) {
        return (input) -> {
            var result = this.parse(input);
            if (result == null) return null;
            return new Result<>(f.apply(result.value()), result.rest());
        };
    }
}
