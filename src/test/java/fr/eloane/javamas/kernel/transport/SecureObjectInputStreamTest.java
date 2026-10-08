package fr.eloane.javamas.kernel.transport;

import fr.eloane.javamas.kernel.messages.Message;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class SecureObjectInputStreamTest {

    private static Object roundTrip(Object o) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bout)) {
            out.writeObject(o);
        }
        try (ObjectInputStream in = new SecureObjectInputStream(new ByteArrayInputStream(bout.toByteArray()))) {
            return in.readObject();
        }
    }

    @Test
    void acceptsMessageWithBasicContent() throws Exception {
        Message<String> mess = new Message<>("hello");
        mess.addReceiver("agent-1");
        Message<?> received = (Message<?>) roundTrip(mess);
        assertEquals("hello", received.getContent());
        assertEquals(mess.getId(), received.getId());
    }

    @Test
    void rejectsClassNotAllowed() {
        assertThrows(InvalidClassException.class, () -> roundTrip(new Message<>(new File("x"))));
    }

    @Test
    void acceptsExplicitlyAllowedClass() throws Exception {
        SecureObjectInputStream.allowClass(AtomicLong.class);
        Message<?> received = (Message<?>) roundTrip(new Message<>(new AtomicLong(42)));
        assertEquals(42, ((AtomicLong) received.getContent()).get());
    }

    @Test
    void checksArrayComponentType() {
        assertTrue(SecureObjectInputStream.isAllowed("[I"));
        assertTrue(SecureObjectInputStream.isAllowed("[[Ljava.lang.String;"));
        assertFalse(SecureObjectInputStream.isAllowed("[Ljava.io.File;"));
        assertFalse(SecureObjectInputStream.isAllowed("I"));
    }
}
