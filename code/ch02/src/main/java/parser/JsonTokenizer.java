package parser;

public interface JsonTokenizer {
    boolean moveNext();
    Token current();
}
