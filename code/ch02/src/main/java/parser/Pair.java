package parser;

import java.util.Objects;

public class Pair<T, U> {
    public final T fst;
    public final U snd;
    public Pair(T fst, U snd) {
        this.fst = fst;
        this.snd = snd;
    }
    
    public static <T, U> Pair<T, U> of(T fst, U snd) {
        return new Pair<>(fst, snd);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(fst, pair.fst) &&
            Objects.equals(snd, pair.snd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fst, snd);
    }
    
    @Override
    public String toString() {
        return "(" + fst + ", " + snd + ")";
    }
}
