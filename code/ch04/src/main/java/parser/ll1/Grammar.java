package parser.ll1;
import java.util.List;

public record Grammar(String start, List<Rule> rules) {}