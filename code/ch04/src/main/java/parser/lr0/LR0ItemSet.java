package parser.lr0;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class LR0ItemSet {
    private final Set<LR0Item> items;

    public LR0ItemSet(Set<LR0Item> items) {
        this.items = new HashSet<>(Objects.requireNonNull(items));
    }

    public Set<LR0Item> items() {
        return items;
    }

    public LR0ItemSet closure(Grammar grammar) {
        Set<LR0Item> closure = new HashSet<>(items);
        boolean added;
        do {
            added = false;
            Set<LR0Item> newItems = new HashSet<>();  // 新しいアイテムを一時的に保持
            for (LR0Item item : closure) {
                Expression next = item.nextSymbol();
                if (next instanceof Expression.NonTerminal nt) {
                    for (Rule rule : grammar.rules()) {
                        if (rule.name().equals(nt.name())) {
                            LR0Item newItem = new LR0Item(rule, 0);
                            if (!closure.contains(newItem)) {  // 追加前にチェック
                                newItems.add(newItem);  // 一時セットに追加
                                added = true;
                            }
                        }
                    }
                }
            }
            closure.addAll(newItems);  // 一時セットのアイテムをまとめて追加
        } while (added);
        return new LR0ItemSet(closure);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LR0ItemSet that = (LR0ItemSet) o;
        return items.equals(that.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{\n");
        for (LR0Item item : items) {
            sb.append("  ").append(item).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
}