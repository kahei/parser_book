package parser;
import java.util.*;

public record Rule(char lhs, List<Elements.Element> rhs) {
    private static List<Elements.Element> toElements(String string) {
        List<Elements.Element> elements = new ArrayList<>();
        for(int i = 0; i < string.length(); i++) {
            elements.add(new Elements.Terminal(string.charAt(i)));
        }
        return elements;
    }
    public Rule(char lhs, String rhs) {
        this(lhs, toElements(rhs));
    }
    public boolean matches(String sequence) {
        return matches(toElements(sequence));
    }
    public boolean matches(List<Elements.Element> sequence) {
        for(int i = 1; i <= rhs.size(); i++) {
            var a = rhs.get(rhs.size() - i);
            if(sequence.size() - i < 0) return false;
            var b = sequence.get(sequence.size() - i);
            if(!a.equals(b)) return false;
        }
        return true;
    }
}