package fr.eloane.javamas.kernel.environment;

import fr.eloane.javamas.kernel.Node;
import fr.eloane.javamas.kernel.TestAgent;
import fr.eloane.javamas.kernel.sensors.Sensor;
import fr.eloane.javamas.kernel.sensors.SensorType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class EnvironmentTest {

    @Test
    void propertiesAndListeners() {
        Environment env = new Environment();
        List<String> changes = new ArrayList<>();
        env.addListener((property, value) -> changes.add(property + "=" + value));
        env.set("light", true);
        env.update("count", Integer.class, c -> c == null ? 1 : c + 1);
        env.update("count", Integer.class, c -> c + 1);
        env.set("light", null);
        assertEquals(2, env.get("count", Integer.class));
        assertNull(env.get("light", Boolean.class));
        assertEquals(false, env.get("light", Boolean.class, false));
        assertEquals(List.of("light=true", "count=1", "count=2", "light=null"), changes);
        assertThrows(ClassCastException.class, () -> env.get("count", String.class));
    }

    @Test
    void agentsPerceiveThePropertiesWithSensors() {
        try (Node node = new Node()) {
            Environment env = new Environment();
            env.set("temperature", 20.0);
            Sensor<Double> thermometer = env.sensor("temperature", Double.class, SensorType.THERMAL);
            assertEquals(20.0, thermometer.getValue());

            TestAgent agent = new TestAgent(node);
            agent.addSensor(thermometer);
            env.set("temperature", 21.5);
            assertEquals(21.5, thermometer.getValue());
            assertEquals(List.of(thermometer), agent.sensed);

            env.set("temperature", "wrong type");
            assertEquals(21.5, thermometer.getValue(), "values of another type are ignored");

            env.removeSensor(thermometer);
            env.set("temperature", 30.0);
            assertEquals(21.5, thermometer.getValue());
        }
    }

    @Test
    void dynamicsRunAtEachStep() {
        Environment env = new Environment();
        env.set("temperature", 20.0);
        env.addDynamics(e -> e.update("temperature", Double.class, t -> t - 0.5));
        env.step();
        env.step();
        assertEquals(19.0, env.get("temperature", Double.class));
        assertEquals(2, env.getSteps());
    }

    @Test
    void periodicSteps() throws InterruptedException {
        try (Environment env = new Environment()) {
            env.addDynamics(e -> e.update("ticks", Integer.class, t -> t == null ? 1 : t + 1));
            env.start(Duration.ofMillis(10));
            assertThrows(IllegalStateException.class, () -> env.start(Duration.ofMillis(10)));
            long end = System.nanoTime() + Duration.ofSeconds(5).toNanos();
            while (env.get("ticks", Integer.class, 0) < 5 && System.nanoTime() < end) {
                Thread.sleep(10);
            }
            assertTrue(env.get("ticks", Integer.class, 0) >= 5);
        }
    }
}
