package parser;
public interface JsonParser {
        public ParseResult<Ast.JsonValue> parse(String input);
}
