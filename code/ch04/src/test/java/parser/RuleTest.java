package parser;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.List;

public class RuleTest {
    @Test
    public void testEmptyRuleShouldMatchEmptySequence() {
        var r = new Rule('A', "");
        assertTrue(r.matches(""));
    }

    @Test
    public void testRegularRuleShouldMatchCorrectSequence() {
        var r = new Rule('A', "abc");
        assertTrue(r.matches("abc"));
    }

    @Test
    public void testRegularRuleShouldNotMatchIncorrectSequence() {
        var r = new Rule('A', "abc");
        assertFalse(r.matches("abd"));
    }

    @Test
    public void testRegularRuleShouldNotMatchShorterSequence() {
        var r = new Rule('A', "abc");
        assertFalse(r.matches("ab"));
    }

    @Test
    public void testRegularRuleShouldMatchLongerSuffix() {
        var r = new Rule('A', "abc");
        assertTrue(r.matches("dabc"));
    }

    @Test
    public void testRegularRuleShouldNotMatchPrefix() {
        var r = new Rule('A', "abc");
        assertFalse(r.matches("abcd"));
    }
}
