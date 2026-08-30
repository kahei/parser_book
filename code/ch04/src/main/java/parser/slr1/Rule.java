package parser.slr1;

import java.util.List;

public record Rule(String name, List<Expression> body) {}
