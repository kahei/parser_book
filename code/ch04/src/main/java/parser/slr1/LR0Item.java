package parser.slr1;

import java.util.Objects;

public record LR0Item(Rule rule, int dotPosition) {
    public LR0Item {
        Objects.requireNonNull(rule);
        if (dotPosition < 0 || dotPosition > rule.body().size()) {
            throw new IllegalArgumentException("Invalid dot position");
        }
    }

    public Expression nextSymbol() {
        return dotPosition < rule.body().size() ? rule.body().get(dotPosition) : null;
    }

    public LR0Item advance() {
        return new LR0Item(rule, dotPosition + 1);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(rule.name()).append(" -> ");
        for (int i = 0; i < rule.body().size(); i++) {
            if (i == dotPosition) sb.append("•");
            sb.append(rule.body().get(i)).append(" ");
        }
        if (dotPosition == rule.body().size()) sb.append("•");
        return sb.toString();
    }
}