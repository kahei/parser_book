package parser;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class DyckShiftReduceTest {
    @Test
    public void acceptsBalancedStrings() {
        assertTrue(new DyckShiftReduce("").parse());
        assertTrue(new DyckShiftReduce("()").parse());
        assertTrue(new DyckShiftReduce("(())").parse());
        assertTrue(new DyckShiftReduce("()()").parse());
        assertTrue(new DyckShiftReduce("(()())()").parse());
    }

    @Test
    public void rejectsUnbalancedStrings() {
        assertFalse(new DyckShiftReduce(")(").parse());
        assertFalse(new DyckShiftReduce("(()").parse());
        assertFalse(new DyckShiftReduce("())").parse());
    }
}
