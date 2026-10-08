package fr.eloane.javamas.kernel;

import fr.eloane.javamas.kernel.messages.Message;
import fr.eloane.javamas.kernel.probes.Probe;
import fr.eloane.javamas.kernel.sensors.Sensor;
import fr.eloane.javamas.kernel.sensors.SensorType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AgentTest {

    private static class TestAgent extends Agent<String> {

        final List<Sensor<?>> sensed = new ArrayList<>();
        final CountDownLatch done = new CountDownLatch(1);

        @Override
        protected void activate() {
        }

        @Override
        protected void live() {
        }

        @Override
        protected void end() {
            done.countDown();
        }

        @Override
        protected void handleSensor(Sensor<?> sensor) {
            sensed.add(sensor);
        }

        void publishRaw(Object value) {
            setChanged();
            notifyObservers(value);
        }
    }

    private final List<TestAgent> agents = new ArrayList<>();

    private TestAgent newAgent() {
        TestAgent agent = new TestAgent();
        agents.add(agent);
        return agent;
    }

    @AfterEach
    void killAgents() {
        agents.forEach(Agent::kill);
    }

    private static Message<String> message(String content, int priority) {
        return new Message<>(content).setPriority(priority);
    }

    @Test
    void messagesAreOrderedByPriorityThenArrival() {
        TestAgent agent = newAgent();
        agent.pushMessage(message("normal 1", Message.NORMAL_PRIORITY));
        agent.pushMessage(message("low", Message.LOW_PRIORITY));
        agent.pushMessage(message("normal 2", Message.NORMAL_PRIORITY));
        agent.pushMessage(message("extrem", Message.EXTREM_PRIORITY));
        agent.pushMessage(message("normal 3", Message.NORMAL_PRIORITY));

        List<Object> received = new ArrayList<>();
        while (agent.countMessages() > 0) {
            received.add(agent.waitNextMessage().getContent());
        }
        assertEquals(List.of("extrem", "normal 1", "normal 2", "normal 3", "low"), received);
    }

    @Test
    void waitWithTimeoutReturnsImmediatelyWhenAMessageIsQueued() {
        TestAgent agent = newAgent();
        agent.pushMessage(message("hello", Message.NORMAL_PRIORITY));
        long start = System.nanoTime();
        assertEquals("hello", agent.waitNextMessage(5000).getContent());
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 1000);
    }

    @Test
    void waitWithTimeoutReturnsNullWithoutMessage() {
        assertNull(newAgent().waitNextMessage(50));
    }

    @Test
    void sendMessageReachesReceiver() {
        TestAgent sender = newAgent();
        TestAgent receiver = newAgent();
        sender.sendMessage(new Message<>("hi").addReceiver(receiver.getAddress().getId()));
        Message<?> received = receiver.waitNextMessage(1000);
        assertEquals("hi", received.getContent());
        assertEquals(sender.getAddress().getId(), received.getSender());
    }

    @Test
    void probesReceiveQueueSize() {
        TestAgent agent = newAgent();
        List<AgentProbeValue<?>> values = new ArrayList<>();
        agent.getProbesManager().addProbe(Probe.of(AgentProbeValue.class, values::add));
        agent.pushMessage(message("hello", Message.NORMAL_PRIORITY));
        assertEquals(new AgentProbeValue<>(Agent.MESSAGES_QUEUE_PROBE, 1), values.get(0));
    }

    @Test
    void typedProbeIgnoresOtherValues() {
        TestAgent agent = newAgent();
        List<Integer> values = new ArrayList<>();
        agent.getProbesManager().addProbe(Probe.of(Integer.class, values::add));
        agent.pushMessage(message("hello", Message.NORMAL_PRIORITY));
        agent.probe("COUNT", 2);
        agent.publishRaw(3);
        assertEquals(List.of(3), values);
    }

    @Test
    void sensorsNotifyTheAgent() {
        TestAgent agent = newAgent();
        Sensor<Integer> sensor = new Sensor<>(SensorType.THERMAL);
        agent.getSensorsManager().addSensor(sensor);
        sensor.setValue(21);
        assertEquals(List.of(sensor), agent.sensed);

        agent.getSensorsManager().removeSensor(sensor);
        sensor.setValue(22);
        assertEquals(1, agent.sensed.size());
    }

    @Test
    void runsInVirtualThread() throws InterruptedException {
        TestAgent agent = newAgent();
        AtomicReference<Thread> thread = new AtomicReference<>(agent.startVirtual());
        assertTrue(agent.done.await(5, TimeUnit.SECONDS));
        assertTrue(thread.get().isVirtual());
    }
}
