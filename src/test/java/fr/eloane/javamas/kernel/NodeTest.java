package fr.eloane.javamas.kernel;

import fr.eloane.javamas.kernel.messages.Message;
import fr.eloane.javamas.kernel.organization.Target;
import fr.eloane.javamas.kernel.transport.InMemoryTransport;
import java.time.Duration;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class NodeTest {

    private final Node a = new Node("a");
    private final Node b = new Node("b");
    private final Node c = new Node("c");

    @AfterEach
    void close() {
        a.close();
        b.close();
        c.close();
    }

    @Test
    void messagesCrossNodes() {
        InMemoryTransport.Hub hub = new InMemoryTransport.Hub();
        a.addTransport(new InMemoryTransport(hub));
        b.addTransport(new InMemoryTransport(hub));
        TestAgent sender = new TestAgent(a);
        TestAgent receiver = new TestAgent(b);
        receiver.getOrganization().joinCommunity("lab");

        sender.send(new Message<>("by address").to(receiver.getAddress()));
        sender.send(new Message<>("by target").to(Target.community("lab")));
        assertEquals("by address", receiver.next().getContent());
        assertEquals("by target", receiver.next().getContent());
        assertNull(receiver.next(Duration.ofMillis(50)), "each message is delivered once");
    }

    @Test
    void messagesAreRelayedBetweenTransportsWithinTtl() {
        // a <-> b <-> c : b bridges two hubs
        InMemoryTransport.Hub ab = new InMemoryTransport.Hub();
        InMemoryTransport.Hub bc = new InMemoryTransport.Hub();
        a.addTransport(new InMemoryTransport(ab));
        b.addTransport(new InMemoryTransport(ab));
        b.addTransport(new InMemoryTransport(bc));
        c.addTransport(new InMemoryTransport(bc));
        TestAgent sender = new TestAgent(a);
        TestAgent receiver = new TestAgent(c);

        sender.send(new Message<>("relayed").to(receiver.getAddress()));
        assertEquals("relayed", receiver.next().getContent());

        sender.send(new Message<>("not relayed").to(receiver.getAddress()).ttl(1));
        assertNull(receiver.next(Duration.ofMillis(50)));
    }

    @Test
    void messageLoopedBackIsNotDeliveredTwice() {
        InMemoryTransport.Hub hub = new InMemoryTransport.Hub();
        a.addTransport(new InMemoryTransport(hub));
        b.addTransport(new InMemoryTransport(hub));
        // b sends everything back to the hub (ttl 4) : a must ignore its own messages
        TestAgent sender = new TestAgent(a);
        TestAgent local = new TestAgent(a);
        local.getOrganization().joinCommunity("lab");
        sender.send(new Message<>("once").to(Target.community("lab")));
        assertEquals("once", local.next().getContent());
        assertNull(local.next(Duration.ofMillis(50)));
    }

    @Test
    void expiredMessagesAreNotDelivered() {
        TestAgent sender = new TestAgent(a);
        TestAgent receiver = new TestAgent(a);
        sender.send(new Message<>("late").to(receiver.getAddress()).expiresAt(Instant.now().minusSeconds(1)));
        assertEquals(0, receiver.pendingMessages());
    }

    @Test
    void closeStopsAgentsAndRefusesNewOnes() throws Exception {
        TestAgent agent = new TestAgent(a);
        a.close();
        assertTrue(!agent.isRunning());
        assertThrows(IllegalStateException.class, () -> new TestAgent(a));
    }

    @Test
    void defaultNodeIsRecreatedAfterClose() {
        Node first = Node.getDefault();
        assertSame(first, Node.getDefault());
        first.close();
        assertTrue(Node.getDefault() != first);
    }
}
