package parser;

// 記号を表すインタフェース
interface Element {
    char value();
}

// 終端記号（実際の文字：'(', ')', '$'など）
record Terminal(char value) implements Element {}

// 非終端記号（文法記号：'D', 'P', 'X'など）
record NonTerminal(char value) implements Element {}
