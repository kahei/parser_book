package parser;

// 文法規則：
// D -> P
// P -> ( P ) P
// P -> ε（イプシロン：空文字列）
public class Dyck {
    private final String input;
    private int pos;

    public Dyck(String input) {
        this.input = input;
        this.pos = 0;
    }

    public boolean parse() {
        boolean accepted = D();
        if(accepted && pos == input.length()) {
            return true;
        } else {
            return false;
        }
    }

    private boolean D() {
        return P();
    }

    private boolean P() {
        // 先読み文字が '(' の場合
        if (pos < input.length() && input.charAt(pos) == '(') {
            // P -> ( P ) P
            pos++; // '(' を読み進める
            if (!P()) return false;
            if (pos < input.length() && input.charAt(pos) == ')') {
                pos++; // ')' を読み進める
                return P();
            } else {
                return false;
            }
        } else {
            // P -> ε
            return true;
        }
    }
}
