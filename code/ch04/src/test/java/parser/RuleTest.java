package parser;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.List;

public class RuleTest {
    // テスト用に、文字列を終端記号の列へ変換する
    private static List<Element> seq(String symbols) {
        return symbols.chars()
                .mapToObj(c -> (Element) new Terminal((char) c))
                .toList();
    }

    @Test
    public void testEmptyRuleShouldMatchEmptySequence() {
        var r = new Rule('A');
        assertTrue(r.matches(seq("")));
    }

    @Test
    public void testRegularRuleShouldMatchCorrectSequence() {
        var r = new Rule('A', new Terminal('a'), new Terminal('b'), new Terminal('c'));
        assertTrue(r.matches(seq("abc")));
    }

    @Test
    public void testRegularRuleShouldNotMatchIncorrectSequence() {
        var r = new Rule('A', new Terminal('a'), new Terminal('b'), new Terminal('c'));
        assertFalse(r.matches(seq("abd")));
    }

    @Test
    public void testRegularRuleShouldNotMatchShorterSequence() {
        var r = new Rule('A', new Terminal('a'), new Terminal('b'), new Terminal('c'));
        assertFalse(r.matches(seq("ab")));
    }

    @Test
    public void testRegularRuleShouldMatchLongerSuffix() {
        var r = new Rule('A', new Terminal('a'), new Terminal('b'), new Terminal('c'));
        assertTrue(r.matches(seq("dabc")));
    }

    @Test
    public void testRegularRuleShouldNotMatchPrefix() {
        var r = new Rule('A', new Terminal('a'), new Terminal('b'), new Terminal('c'));
        assertFalse(r.matches(seq("abcd")));
    }
}
