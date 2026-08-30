package parser;
public class Elements {
    public sealed interface Element permits NonTerminal, Terminal {}
    public record NonTerminal(char name) implements Element {}
    public record Terminal(char value) implements Element {}
}