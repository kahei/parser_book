package parser;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class DyckTest {
    @Test
    public void acceptsBalancedStrings() {
        assertTrue(new Dyck("").parse());
        assertTrue(new Dyck("()").parse());
        assertTrue(new Dyck("(())").parse());
        assertTrue(new Dyck("()()").parse());
        assertTrue(new Dyck("(()())()").parse());
    }

    @Test
    public void rejectsUnbalancedStrings() {
        assertFalse(new Dyck(")(").parse());
        assertFalse(new Dyck("(()").parse());
        assertFalse(new Dyck("())").parse());
    }
}
