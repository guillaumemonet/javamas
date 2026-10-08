package fr.eloane.javamas.kernel.messages;

import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class MessageTest {

    @Test
    void cloneHasItsOwnReceivers() {
        Message<String> mess = new Message<>("hello").addReceiver("a");
        Message<String> clone = mess.clone();
        clone.addReceiver("b");
        assertEquals(List.of("a"), mess.getReceivers());
        assertEquals(List.of("a", "b"), clone.getReceivers());
        assertEquals(mess, clone);
        assertEquals(mess.hashCode(), clone.hashCode());
    }

    @Test
    void idsAreUnique() {
        assertNotEquals(new Message<>().getId(), new Message<>().getId());
    }

    @Test
    void toStringAcceptsNullContent() {
        assertTrue(new Message<>().toString().contains("Content:\nnull"));
    }

    @Test
    void expireIsOptional() {
        assertEquals(0, new Message<>().getExpire());
        assertEquals(42, new Message<>(42L).getExpire());
    }

    @Test
    void aclPerformatives() {
        assertEquals(ACLMessage.NOT_UNDERSTOOD_STRING, new ACLMessage().getPerformative());
        assertEquals(ACLMessage.CONFIRM_STRING, new ACLMessage(ACLMessage.CONFIRM, "ok").getPerformative());
        assertEquals("CONFIRM", ACLMessage.CONFIRM_STRING);
        assertEquals("INFORM", new ACLMessage("inform", "x").getPerformative());
    }
}
