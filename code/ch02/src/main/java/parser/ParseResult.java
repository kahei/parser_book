package parser;
public class ParseResult<T> {
        public final T value;
        public final String next;
        public ParseResult(T value, String next) {
                this.value = value;
                this.next = next;
        }
}