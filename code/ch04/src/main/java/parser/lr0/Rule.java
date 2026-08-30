package parser.lr0;

import java.util.List;

public record Rule(String name, List<Expression> body) {}