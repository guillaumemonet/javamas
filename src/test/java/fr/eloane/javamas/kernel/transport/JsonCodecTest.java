package fr.eloane.javamas.kernel.transport;

import fr.eloane.javamas.kernel.messages.ACLMessage;
import fr.eloane.javamas.kernel.messages.Message;
import fr.eloane.javamas.kernel.messages.Performative;
import fr.eloane.javamas.kernel.messages.Priority;
import fr.eloane.javamas.kernel.organization.Target;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class JsonCodecTest {

    public record Position(int x, int y) {

    }

    private final JsonCodec codec = new JsonCodec();

    private Message<?> roundTrip(Message<?> message) throws IOException {
        byte[] data = codec.encode(message);
        return codec.decode(data, 0, data.length);
    }

    private Message<?> decode(String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        return codec.decode(data, 0, data.length);
    }

    @Test
    void envelopeAndJsonContent() throws IOException {
        Message<Map<String, Object>> mess = new Message<>(Map.<String, Object>of("count", 3, "tags", List.of("a", "b")))
                .to("A:1")
                .to(Target.group("lab", "team"))
                .priority(Priority.HIGH)
                .expiresAfter(Duration.ofMinutes(1))
                .ttl(2)
                .header(Message.LANGUAGE, "fr")
                .sender("A:0");
        Message<?> received = roundTrip(mess);
        assertEquals(mess.getEnvelope(), received.getEnvelope());
        assertEquals(Map.of("count", 3, "tags", List.of("a", "b")), received.getContent());
    }

    @Test
    void aclMessage() throws IOException {
        ACLMessage received = (ACLMessage) roundTrip(new ACLMessage(Performative.QUERY_IF, "raining?"));
        assertEquals(Performative.QUERY_IF, received.getPerformative());
        assertEquals("raining?", received.getContent());
    }

    @Test
    void registeredContentType() throws IOException {
        assertThrows(IOException.class, () -> codec.encode(new Message<>(new Position(1, 2))));
        codec.registerType("position", Position.class);
        byte[] data = codec.encode(new Message<>(new Position(1, 2)));
        assertTrue(new String(data, StandardCharsets.UTF_8).contains("\"contentType\":\"position\""));
        assertEquals(new Position(1, 2), codec.decode(data, 0, data.length).getContent());
    }

    @Test
    void messageWrittenByAnotherLanguage() throws IOException {
        Message<?> m = decode("""
                {"format": "javamas/1", "id": "M:py-1", "created": "2026-10-08T12:00:00Z",
                 "conversation": "C:py", "priority": "low", "receivers": ["A:1"],
                 "content": {"text": "hello from python"}}
                """);
        assertEquals("M:py-1", m.getId());
        assertEquals(Priority.LOW, m.getPriority());
        assertEquals(List.of("A:1"), m.getReceivers());
        assertEquals(Message.DEFAULT_TTL, m.getTtl());
        assertNull(m.getSender());
        assertEquals(Map.of("text", "hello from python"), m.getContent());
    }

    @Test
    void invalidMessagesAreRejected() {
        assertThrows(IOException.class, () -> decode("not json"));
        assertThrows(IOException.class, () -> decode("{\"format\": \"other\"}"));
        assertThrows(IOException.class, () -> decode("{\"format\": \"javamas/1\", \"id\": \"M:1\"}"));
        assertThrows(IOException.class, () -> decode("""
                {"format": "javamas/1", "id": "M:1", "created": "2026-10-08T12:00:00Z", "conversation": "C:1",
                 "priority": "normal", "contentType": "java.io.File", "content": "/etc/passwd"}
                """));
    }
}
