package fr.eloane.javamas.kernel.transport;

import fr.eloane.javamas.kernel.Node;
import fr.eloane.javamas.kernel.TestAgent;
import fr.eloane.javamas.kernel.messages.Message;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Transports on the loopback interface
 */
class NetworkTransportTest {

    private static final InetAddress LOCALHOST = InetAddress.getLoopbackAddress();

    private final Node a = new Node("a");
    private final Node b = new Node("b");
    private final Node c = new Node("c");

    @AfterEach
    void close() {
        a.close();
        b.close();
        c.close();
    }

    private static int freeUdpPort() throws IOException {
        try (DatagramSocket s = new DatagramSocket(0, LOCALHOST)) {
            return s.getLocalPort();
        }
    }

    @Test
    void udp() throws IOException {
        int portA = freeUdpPort();
        int portB = freeUdpPort();
        a.addTransport(new UdpTransport(new InetSocketAddress(LOCALHOST, portA), new InetSocketAddress(LOCALHOST, portB)));
        b.addTransport(new UdpTransport(new InetSocketAddress(LOCALHOST, portB), new InetSocketAddress(LOCALHOST, portA)));
        TestAgent sender = new TestAgent(a);
        TestAgent receiver = new TestAgent(b);

        sender.send(new Message<>("over udp").to(receiver.getAddress()));
        assertEquals("over udp", receiver.next(Duration.ofSeconds(5)).getContent());

        receiver.send(new Message<>("back").to(sender.getAddress()));
        assertEquals("back", sender.next(Duration.ofSeconds(5)).getContent());
    }

    @Test
    void tcpForwardsBetweenPeers() throws Exception {
        // a -> b <- c : a and c only know b
        TcpTransport hub = new TcpTransport(0, List.of());
        b.addTransport(hub);
        InetSocketAddress hubAddress = new InetSocketAddress(LOCALHOST, hub.getLocalPort());
        a.addTransport(new TcpTransport(-1, List.of(hubAddress)));
        c.addTransport(new TcpTransport(-1, List.of(hubAddress)));
        waitFor(() -> hub.getConnectionCount() == 2);

        TestAgent sender = new TestAgent(a);
        TestAgent receiver = new TestAgent(c);
        sender.send(new Message<>("over tcp").to(receiver.getAddress()));
        Message<?> received = receiver.next(Duration.ofSeconds(5));
        assertNotNull(received);
        assertEquals("over tcp", received.getContent());
        assertEquals(Message.DEFAULT_TTL - 1, received.getTtl(), "relayed once by b");
    }

    private static void waitFor(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long end = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > end) {
                throw new AssertionError("timeout");
            }
            Thread.sleep(10);
        }
    }
}
