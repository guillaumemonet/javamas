package fr.eloane.javamas.kernel.messages;

import fr.eloane.javamas.kernel.organization.Target;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class MessageTest {

    @Test
    void copyHasItsOwnEnvelope() {
        Message<String> mess = new Message<>("hello").to("a").header(Message.LANGUAGE, "fr");
        Message<String> copy = mess.copy();
        copy.to("b").header(Message.LANGUAGE, "en").to(Target.community("c"));
        assertEquals(List.of("a"), mess.getReceivers());
        assertEquals("fr", mess.getHeader(Message.LANGUAGE));
        assertTrue(mess.getTargets().isEmpty());
        assertEquals(mess, copy);
        assertEquals(mess.hashCode(), copy.hashCode());
    }

    @Test
    void replyGoesToTheSenderInTheSameConversation() {
        Message<String> question = new Message<>("ping").sender("A:1");
        Message<Integer> answer = question.reply(42);
        assertEquals(List.of("A:1"), answer.getReceivers());
        assertEquals(question.getConversationId(), answer.getConversationId());
        assertEquals(question.getId(), answer.getHeader(Message.IN_REPLY_TO));
        assertNotEquals(question.getId(), answer.getId());
    }

    @Test
    void expiration() {
        Message<String> mess = new Message<>("x");
        assertFalse(mess.isExpired(Instant.now().plusSeconds(3600)));
        mess.expiresAfter(Duration.ofSeconds(10));
        assertFalse(mess.isExpired(Instant.now()));
        assertTrue(mess.isExpired(Instant.now().plusSeconds(11)));
    }

    @Test
    void aclMessages() {
        ACLMessage request = new ACLMessage(Performative.REQUEST, "open the door");
        request.sender("A:1");
        ACLMessage copy = request.copy();
        assertEquals(Performative.REQUEST, copy.getPerformative());
        ACLMessage agree = request.reply(Performative.AGREE, "ok");
        assertEquals(List.of("A:1"), agree.getReceivers());
        assertEquals(request.getConversationId(), agree.getConversationId());
        assertEquals("accept-proposal", Performative.ACCEPT_PROPOSAL.fipaName());
        assertEquals(Performative.NOT_UNDERSTOOD, Performative.fromFipaName("Not-Understood"));
    }

    @Test
    void targetsNeedTheirParent() {
        assertThrows(IllegalArgumentException.class, () -> new Target(null, "group", null));
        assertThrows(IllegalArgumentException.class, () -> new Target("c", null, "role"));
        assertEquals("lab/team/leader", Target.role("lab", "team", "leader").toString());
        assertEquals("*", Target.all().toString());
    }
}
