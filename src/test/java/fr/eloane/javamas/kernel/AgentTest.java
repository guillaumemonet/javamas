package fr.eloane.javamas.kernel;

import fr.eloane.javamas.kernel.messages.Message;
import fr.eloane.javamas.kernel.messages.Priority;
import fr.eloane.javamas.kernel.organization.Target;
import fr.eloane.javamas.kernel.probes.Probe;
import fr.eloane.javamas.kernel.probes.ProbeValue;
import fr.eloane.javamas.kernel.sensors.Sensor;
import fr.eloane.javamas.kernel.sensors.SensorType;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AgentTest {

    private final Node node = new Node("test");

    @AfterEach
    void close() {
        node.close();
    }

    private static Message<String> message(String content, Priority priority) {
        return new Message<>(content).priority(priority);
    }

    @Test
    void lifeCycleStatesArePublished() {
        List<AgentState> states = new CopyOnWriteArrayList<>();
        Agent agent = new Agent("cycle", node) {
            @Override
            protected void live() {
            }
        };
        agent.addProbe(Probe.of(ProbeValue.STATE, AgentState.class, states::add));
        agent.run();
        assertEquals(List.of(AgentState.ACTIVATING, AgentState.LIVING, AgentState.ENDING, AgentState.DEAD), states);
        assertEquals(AgentState.DEAD, agent.getState());
        assertNull(node.getAgent(agent.getAddress().id()));
        assertThrows(IllegalStateException.class, agent::run);
    }

    @Test
    void messagesAreOrderedByPriorityThenArrival() {
        TestAgent agent = new TestAgent(node);
        agent.deliver(message("normal 1", Priority.NORMAL));
        agent.deliver(message("low", Priority.LOW));
        agent.deliver(message("normal 2", Priority.NORMAL));
        agent.deliver(message("extreme", Priority.EXTREME));
        agent.deliver(message("normal 3", Priority.NORMAL));

        List<Object> received = new ArrayList<>();
        while (agent.pendingMessages() > 0) {
            received.add(agent.next().getContent());
        }
        assertEquals(List.of("extreme", "normal 1", "normal 2", "normal 3", "low"), received);
    }

    @Test
    void receiveWithTimeout() {
        TestAgent agent = new TestAgent(node);
        assertNull(agent.next(Duration.ofMillis(50)));
        agent.deliver(message("hello", Priority.NORMAL));
        long start = System.nanoTime();
        assertEquals("hello", agent.next(Duration.ofSeconds(5)).getContent());
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 1000);
    }

    @Test
    void expiredMessagesAreDropped() {
        TestAgent agent = new TestAgent(node);
        agent.deliver(new Message<>("old").expiresAt(Instant.now().minusSeconds(1)));
        agent.deliver(new Message<>("fresh").expiresAfter(Duration.ofMinutes(1)));
        assertEquals("fresh", agent.next().getContent());
        assertNull(agent.next(Duration.ofMillis(10)));
    }

    @Test
    void stopUnblocksReceive() throws Exception {
        CompletableFuture<Message<?>> received = new CompletableFuture<>();
        Agent agent = new Agent("waiting", node) {
            @Override
            protected void live() {
                received.complete(receive());
            }
        };
        agent.start();
        Thread.sleep(100);
        agent.stop();
        assertNull(received.get(1, TimeUnit.SECONDS));
    }

    @Test
    void sendToAddressAndTarget() {
        TestAgent sender = new TestAgent(node);
        TestAgent receiver = new TestAgent(node);
        TestAgent member = new TestAgent(node);
        sender.getOrganization().joinGroup("lab", "team");
        member.getOrganization().joinGroup("lab", "team");

        sender.send(new Message<>("direct").to(receiver.getAddress()));
        Message<?> direct = receiver.next();
        assertEquals("direct", direct.getContent());
        assertEquals(sender.getAddress().id(), direct.getSender());

        sender.send(new Message<>("team").to(Target.group("lab", "team")));
        assertEquals("team", member.next().getContent());
        assertEquals(0, sender.pendingMessages(), "the sender doesn't receive its own group message");
        assertEquals(0, receiver.pendingMessages());
    }

    @Test
    void eachRecipientGetsItsOwnCopy() {
        TestAgent sender = new TestAgent(node);
        TestAgent a = new TestAgent(node);
        TestAgent b = new TestAgent(node);
        a.getOrganization().joinCommunity("c");
        b.getOrganization().joinCommunity("c");
        sender.send(new Message<>("x").to(Target.community("c")).to(a.getAddress()));
        Message<?> ma = a.next();
        Message<?> mb = b.next();
        assertEquals(ma, mb);
        assertFalse(ma == mb);
        assertEquals(0, a.pendingMessages(), "receiver and member at the same time : delivered once");
    }

    @Test
    void sensorsNotifyTheAgent() {
        TestAgent agent = new TestAgent(node);
        Sensor<Integer> sensor = new Sensor<>(SensorType.THERMAL);
        agent.addSensor(sensor);
        sensor.setValue(21);
        assertEquals(List.of(sensor), agent.sensed);
        agent.removeSensor(sensor);
        sensor.setValue(22);
        assertEquals(1, agent.sensed.size());
    }

    @Test
    void probesFilterByNameAndType() {
        TestAgent agent = new TestAgent(node);
        List<Integer> counts = new ArrayList<>();
        List<Integer> sizes = new ArrayList<>();
        agent.addProbe(Probe.of("count", Integer.class, counts::add));
        agent.addProbe(Probe.of(ProbeValue.MAILBOX_SIZE, Integer.class, sizes::add));
        agent.publishValue("count", 3);
        agent.publishValue("count", "not an integer");
        agent.deliver(new Message<>("x"));
        assertEquals(List.of(3), counts);
        assertEquals(List.of(1), sizes);
    }

    @Test
    void runsInVirtualThread() throws Exception {
        CompletableFuture<Boolean> virtual = new CompletableFuture<>();
        Agent agent = new Agent("virtual", node) {
            @Override
            protected void live() {
                virtual.complete(Thread.currentThread().isVirtual());
            }
        };
        agent.startVirtual();
        assertTrue(virtual.get(5, TimeUnit.SECONDS));
        assertThrows(IllegalStateException.class, agent::start);
    }
}
