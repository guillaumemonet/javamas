package fr.eloane.javamas.kernel.transport;

import fr.eloane.javamas.kernel.messages.ACLMessage;
import fr.eloane.javamas.kernel.messages.Message;
import fr.eloane.javamas.kernel.messages.Performative;
import fr.eloane.javamas.kernel.messages.Priority;
import fr.eloane.javamas.kernel.organization.Target;
import java.io.File;
import java.io.InvalidClassException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class JavaSerializationCodecTest {

    private final JavaSerializationCodec codec = new JavaSerializationCodec();

    private Message<?> roundTrip(Message<?> message) throws Exception {
        byte[] data = codec.encode(message);
        return codec.decode(data, 0, data.length);
    }

    @Test
    void messageWithFullEnvelope() throws Exception {
        Message<String> mess = new Message<>("hello")
                .to("A:1")
                .to(Target.role("lab", "team", "leader"))
                .priority(Priority.HIGH)
                .expiresAfter(Duration.ofMinutes(1))
                .header(Message.ONTOLOGY, "test")
                .sender("A:0");
        Message<?> received = roundTrip(mess);
        assertEquals(mess, received);
        assertEquals("hello", received.getContent());
        assertEquals(mess.getReceivers(), received.getReceivers());
        assertEquals(mess.getTargets(), received.getTargets());
        assertEquals(Priority.HIGH, received.getPriority());
        assertEquals(mess.getExpiresAt(), received.getExpiresAt());
        assertEquals("test", received.getHeader(Message.ONTOLOGY));
    }

    @Test
    void aclMessage() throws Exception {
        ACLMessage received = (ACLMessage) roundTrip(new ACLMessage(Performative.INFORM, "fact"));
        assertEquals(Performative.INFORM, received.getPerformative());
    }

    @Test
    void contentNotAllowedIsRejected() {
        assertThrows(InvalidClassException.class, () -> roundTrip(new Message<>(new File("x"))));
    }

    @Test
    void contentExplicitlyAllowed() throws Exception {
        assertThrows(InvalidClassException.class, () -> roundTrip(new Message<>(new AtomicLong(42))));
        codec.allowClass(AtomicLong.class);
        assertEquals(42, ((AtomicLong) roundTrip(new Message<>(new AtomicLong(42))).getContent()).get());
    }

    @Test
    void arraysAreCheckedByComponentType() {
        assertEquals(true, codec.isAllowed(int[].class));
        assertEquals(true, codec.isAllowed(String[][].class));
        assertEquals(false, codec.isAllowed(File[].class));
    }
}
