package com.github.kmizu.peg2java;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class DyckParserTest {
    @Test
    public void acceptsBalancedStrings() {
        assertTrue(new DyckParser("").parse());
        assertTrue(new DyckParser("()").parse());
        assertTrue(new DyckParser("(())").parse());
        assertTrue(new DyckParser("()()").parse());
        assertTrue(new DyckParser("(()())()").parse());
    }

    @Test
    public void rejectsUnbalancedStrings() {
        assertFalse(new DyckParser(")(").parse());
        assertFalse(new DyckParser("(()").parse());
        assertFalse(new DyckParser("())").parse());
    }
}
