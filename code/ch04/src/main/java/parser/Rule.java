package parser;

import java.util.List;

public record Rule(char lhs, List<Element> rhs) {
    // 可変長引数コンストラクタ（便利のため）
    public Rule(char lhs, Element... rhs) {
        this(lhs, List.of(rhs));
    }

    // スタックの上端がこの規則の右辺と一致するか判定
    public boolean matches(List<Element> stack) {
        if (stack.size() < rhs.size()) return false;

        // スタックの上からrhs.size()個の要素を比較
        for (int i = 0; i < rhs.size(); i++) {
            Element elementInRule = rhs.get(i);
            Element elementInStack = stack.get(
                stack.size() - rhs.size() + i
            );
            if (!elementInRule.equals(elementInStack)) {
                return false;
            }
        }
        return true;
    }
}
