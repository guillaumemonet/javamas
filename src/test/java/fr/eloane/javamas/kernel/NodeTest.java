package fr.eloane.javamas.kernel;

import fr.eloane.javamas.kernel.messages.Message;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class NodeTest {

    private static class Inbox extends Agent<String> {

        @Override
        protected void activate() {
        }

        @Override
        protected void live() {
        }

        @Override
        protected void end() {
        }
    }

    @Test
    void messageLoopedBackByTransportIsNotDeliveredTwice() {
        Inbox inbox = new Inbox();
        try {
            Node node = Node.getHandle();
            Message<String> mess = new Message<>("hello");
            mess.addReceiver(inbox.getAddress().getId());

            node.sendMessage(mess);
            assertEquals(1, inbox.countMessages());

            // Same message coming back from the network (multicast loopback)
            node.update(null, mess);
            assertEquals(1, inbox.countMessages());

            // A new message from the network is delivered
            Message<String> remote = new Message<>("remote");
            remote.addReceiver(inbox.getAddress().getId());
            node.update(null, remote);
            assertEquals(2, inbox.countMessages());
        } finally {
            inbox.kill();
        }
    }
}
