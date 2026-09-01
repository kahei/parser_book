package parser;

import java.util.List;
import java.util.ArrayList;

public class DyckShiftReduce {
    private final String input;
    private int position;
    private final List<Rule> rules;
    private final List<Element> stack = new ArrayList<>();

    public DyckShiftReduce(String input) {
        this.input = input;
        this.position = 0;

        // 文法規則の定義
        this.rules = List.of(
            // D -> $ P $
            new Rule('D',
                new Terminal('$'),
                new NonTerminal('P'),
                new Terminal('$')
            ),
            // D -> $ $ (空文字列の場合)
            new Rule('D',
                new Terminal('$'),
                new Terminal('$')
            ),
            // P -> P X
            new Rule('P',
                new NonTerminal('P'),
                new NonTerminal('X')
            ),
            // P -> X
            new Rule('P',
                new NonTerminal('X')
            ),
            // X -> ( P )
            new Rule('X',
                new Terminal('('),
                new NonTerminal('P'),
                new Terminal(')')
            ),
            // X -> ()
            new Rule('X',
                new Terminal('('),
                new Terminal(')')
            )
        );
    }

    public boolean parse() {
        // 入力の開始を表す$をスタックに積む
        stack.add(new Terminal('$'));

        // メインループ：シフトと還元を繰り返す
        while (true) {
            // まず還元を試みる
            if (!tryReduce()) {
                // 還元できない場合、シフトを試みる
                if (position < input.length()) {
                    char c = input.charAt(position);
                    stack.add(new Terminal(c));
                    position++;
                } else {
                    // シフトもできないので終了
                    break;
                }
            }
        }

        // 入力の終端を表す$をスタックに積む
        stack.add(new Terminal('$'));

        // 最後に可能な限り還元を繰り返す
        while (tryReduce()) {
            // 還元ができなくなるまで続ける
        }

        // スタックが[D]のみになったら受理
        return stack.size() == 1 &&
               stack.get(0).equals(new NonTerminal('D'));
    }

    private boolean tryReduce() {
        for (Rule rule : rules) {
            if (rule.matches(stack)) {
                // マッチしたら右辺の長さ分スタックから削除
                for (int i = 0; i < rule.rhs().size(); i++) {
                    stack.remove(stack.size() - 1);
                }
                // 左辺の非終端記号をスタックに追加
                stack.add(new NonTerminal(rule.lhs()));
                return true; // 還元成功
            }
        }
        return false; // 還元できる規則がなかった
    }
}
