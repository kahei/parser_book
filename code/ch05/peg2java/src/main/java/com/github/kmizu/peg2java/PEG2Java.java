package com.github.kmizu.peg2java;

import java.util.*;
import java.util.stream.Collectors;
// 簡単な構文解析器生成系の例
public class PEG2Java {
    // 文法規則を表すレコード
    public record Rule(String name, Expr body) {}
    
    // PEG式を表す抽象クラス
    public abstract static class Expr {
        abstract String generate(int indentLevel);
    }
    
    // 文字列リテラル
    public static class Lit extends Expr {
        public final String value;
        public Lit(String value) {
            this.value = value;
        }
        String generate(int indentLevel) {
            return " ".repeat(indentLevel) + "match(\"" + value + "\");";
        }
    }

    // 非終端記号
    public static class NT extends Expr {
        public final String name;
        public NT(String name) {
            this.name = name;
        }
        // 非終端記号のコード生成
        String generate(int indentLevel) {
            return " ".repeat(indentLevel) + "parse" + name + "();";
        }
    }
    
    // 連接
    static class Seq extends Expr {
        public final List<Expr> exprs;
        public Seq(Expr... exprs) {
            this.exprs = List.of(exprs);
        }

        // 連接のコード生成。単純にN個の式を順に生成
        String generate(int indentLevel) {
            return exprs.stream()
                .map(e -> e.generate(indentLevel))
                .collect(Collectors.joining("\n"));
        }
    }

    // 順序付き選択
    public static class Choice extends Expr {
        public final Expr alt1, alt2;
        public Choice(Expr alt1, Expr alt2) {
            this.alt1 = alt1;
            this.alt2 = alt2;
        }

        // 順序付き選択のコード生成
        // try-catchを使ってバックトラックを実装
        String generate(int indentLevel) {
            String indent = " ".repeat(indentLevel);
            return indent + "int saved = pos;\n"
                + indent + "try {\n"
                + alt1.generate(indentLevel + 4) + "\n"
                + indent + "} catch (Failure e) {\n"
                + indent + "    pos = saved;\n"
                + alt2.generate(indentLevel + 4) + "\n"
                + indent + "}";
        }
    }
    
    // コード生成のメインメソッド
    public static String generateParser(List<Rule> rules) {
        StringBuilder code = new StringBuilder();
        code.append("""
            public class Parser {
                public static class Failure extends RuntimeException {
                    public Failure(String message) {
                        super(message);
                    }
                }
                private String input;
                private int pos;
                public Parser(String input) {
                    this.input = input;
                    this.pos = 0;
                }
                public void match(String str) {
                    if (pos + str.length() <= input.length() &&
                        input.startsWith(str, pos)) {
                        pos += str.length();
                        return;
                    }
                    throw new Failure("Expected '" + str + "' at pos " + pos);
                }
            """);
        // 各規則に対してメソッドを生成
        for (Rule rule : rules) {
            code.append("\n    public void parse" + rule.name + "() {\n");
            code.append(rule.body.generate(8));
            code.append("\n        return;\n");
            code.append("    }\n");
        }
        code.append("}\n");
        return code.toString();
    }
}
