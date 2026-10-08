package fr.eloane.javamas.kernel;

import fr.eloane.javamas.kernel.messages.Message;
import fr.eloane.javamas.kernel.sensors.Sensor;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Agent driven by the tests : not started, its mailbox is read by the test
 */
public class TestAgent extends Agent {

    public final List<Sensor<?>> sensed = new CopyOnWriteArrayList<>();

    public TestAgent(Node node) {
        super("test", node);
    }

    @Override
    protected void live() {
    }

    @Override
    protected void handleSensor(Sensor<?> sensor) {
        sensed.add(sensor);
    }

    /**
     *
     * @return the next message or null after one second
     */
    public Message<?> next() {
        return receive(Duration.ofSeconds(1));
    }

    public Message<?> next(Duration timeout) {
        return receive(timeout);
    }

    public void publishValue(String name, Object value) {
        publish(name, value);
    }
}
