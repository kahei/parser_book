package com.github.kmizu.peg2java;

public class DyckParser {
    private String input;
    private int pos;

    public static class Failure extends RuntimeException {
        public Failure(String message) {
            super(message);
        }
    }

    public DyckParser(String input) {
        this.input = input;
        this.pos = 0;
    }

    // D <- P
    public void parseD() {
        parseP();
    }

    // P <- "(" P ")" P / ""
    public void parseP() {
        // 現在位置を保存（バックトラック用）
        int saved = pos;

        try {
            match("(");  // "(" にマッチするか？
            parseP();  // Pを再帰的に呼び出す（1回目）
            match(")");  // ")" にマッチするか？
            parseP();  // Pを再帰的に呼び出す（2回目）
            return;  // 成功
        } catch (Failure e) {
            // 失敗したらバックトラック
            pos = saved;
            // 空文字列 "" を試す（常に成功）
            return;
        }
    }

    // 文字列のマッチングを行うヘルパーメソッド
    private void match(String str) {
        if (pos + str.length() <= input.length() &&
            input.startsWith(str, pos)) {
            pos += str.length();
            return;
        }
        throw new Failure("Expected '" + str + "' at position " + pos);
    }

    // パース実行メソッド
    public boolean parse() {
        parseD();
        // 例外が起きず、すべての文字が消費されていれば成功
        if (pos == input.length()) {
            return true;
        } else {
            return false;
        }
    }
}
