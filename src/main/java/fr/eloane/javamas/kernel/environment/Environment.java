/* 
 * The MIT License
 *
 * Copyright 2018 Guillaume Monet.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package fr.eloane.javamas.kernel.environment;

import fr.eloane.javamas.kernel.sensors.Sensor;
import fr.eloane.javamas.kernel.sensors.SensorType;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Environment shared by agents : a set of named properties (temperature,
 * light, position of an object...).<br />
 * Agents <b>perceive</b> it with sensors bound to properties
 * ({@link #sensor(String, Class, SensorType)}), <b>act</b> on it by changing
 * properties ({@link #set(String, Object)}, {@link #update}), and it
 * <b>evolves</b> by itself with dynamics rules run at each {@link #step()},
 * manually for reproducible simulations or periodically with
 * {@link #start(Duration)}.<br />
 * Thread safe. Sensors and listeners are notified in the thread changing the
 * property.
 *
 * @author Guillaume Monet
 */
public final class Environment implements AutoCloseable {

    private static final System.Logger LOGGER = System.getLogger(Environment.class.getName());

    private record Binding<T>(Sensor<T> sensor, Class<T> type) {

        void notify(Object value) {
            if (value == null || type.isInstance(value)) {
                sensor.setValue(type.cast(value));
            } else {
                LOGGER.log(System.Logger.Level.WARNING, "Sensor " + sensor.getName() + " expects " + type.getName()
                        + " but the property is a " + value.getClass().getName());
            }
        }
    }

    private final String name;
    private final Map<String, Object> properties = new ConcurrentHashMap<>();
    private final Map<String, List<Binding<?>>> bindings = new ConcurrentHashMap<>();
    private final List<EnvironmentListener> listeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<Environment>> dynamics = new CopyOnWriteArrayList<>();
    private final AtomicLong steps = new AtomicLong();
    private ScheduledExecutorService clock;

    public Environment() {
        this("environment");
    }

    /**
     *
     * @param name name of the environment, for the logs and threads
     */
    public Environment(String name) {
        this.name = name;
    }

    // ------------------------------------------------------------ properties
    /**
     * Change a property and notify its sensors
     *
     * @param property
     * @param value null to remove the property
     */
    public void set(String property, Object value) {
        if (value == null) {
            properties.remove(property);
        } else {
            properties.put(property, value);
        }
        changed(property, value);
    }

    /**
     * Change a property atomically
     *
     * @param <T> type of the property
     * @param property
     * @param type type of the property
     * @param change receives the current value (null if absent), returns the
     * new value (null to remove the property)
     * @return the new value
     * @throws ClassCastException if the property has another type
     */
    public <T> T update(String property, Class<T> type, UnaryOperator<T> change) {
        Object value = properties.compute(property, (k, old) -> change.apply(old == null ? null : type.cast(old)));
        changed(property, value);
        return type.cast(value);
    }

    /**
     *
     * @param <T>
     * @param property
     * @param type
     * @return the value or null if the property doesn't exist
     * @throws ClassCastException if the property has another type
     */
    public <T> T get(String property, Class<T> type) {
        return type.cast(properties.get(property));
    }

    /**
     *
     * @param <T>
     * @param property
     * @param type
     * @param defaultValue
     * @return the value or the default value if the property doesn't exist
     */
    public <T> T get(String property, Class<T> type, T defaultValue) {
        T value = get(property, type);
        return value == null ? defaultValue : value;
    }

    /**
     *
     * @return a copy of all the properties
     */
    public Map<String, Object> snapshot() {
        return Map.copyOf(properties);
    }

    private void changed(String property, Object value) {
        List<Binding<?>> bound = bindings.get(property);
        if (bound != null) {
            bound.forEach(b -> b.notify(value));
        }
        listeners.forEach(l -> l.propertyChanged(property, value));
    }

    // ----------------------------------------------------------- perception
    /**
     * Create a sensor perceiving a property : its value follows the property.
     * Add it to an agent with {@code agent.addSensor(sensor)}.
     *
     * @param <T> type of the property
     * @param property name of the property, also the name of the sensor
     * @param type type of the property
     * @param sensorType kind of sensor
     * @return the sensor, initialized with the current value
     */
    public <T> Sensor<T> sensor(String property, Class<T> type, SensorType sensorType) {
        Sensor<T> sensor = new Sensor<>(sensorType, property);
        Object current = properties.get(property);
        if (type.isInstance(current)) {
            sensor.setValue(type.cast(current));
        }
        bindings.computeIfAbsent(property, k -> new CopyOnWriteArrayList<>()).add(new Binding<>(sensor, type));
        return sensor;
    }

    /**
     * The sensor stops following its property
     *
     * @param sensor
     */
    public void removeSensor(Sensor<?> sensor) {
        bindings.values().forEach(list -> list.removeIf(b -> b.sensor() == sensor));
    }

    /**
     * Observe all the changes, e.g. to display or record the environment
     *
     * @param listener
     */
    public void addListener(EnvironmentListener listener) {
        listeners.add(listener);
    }

    public void removeListener(EnvironmentListener listener) {
        listeners.remove(listener);
    }

    // -------------------------------------------------------------- dynamics
    /**
     * Add a rule run at each step, e.g. the room cools down
     *
     * @param rule
     */
    public void addDynamics(Consumer<Environment> rule) {
        dynamics.add(rule);
    }

    /**
     * Run all the dynamics rules once
     *
     * @return the number of steps run so far
     */
    public long step() {
        for (Consumer<Environment> rule : dynamics) {
            rule.accept(this);
        }
        return steps.incrementAndGet();
    }

    public long getSteps() {
        return steps.get();
    }

    /**
     * Run a step periodically in a daemon thread
     *
     * @param period
     * @throws IllegalStateException if already started
     */
    public synchronized void start(Duration period) {
        if (clock != null) {
            throw new IllegalStateException(this + " already started");
        }
        clock = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon().name("javamas-" + name).factory());
        clock.scheduleAtFixedRate(() -> {
            try {
                step();
            } catch (RuntimeException e) {
                LOGGER.log(System.Logger.Level.ERROR, "Dynamics of " + this + " failed", e);
            }
        }, period.toNanos(), period.toNanos(), TimeUnit.NANOSECONDS);
    }

    /**
     * Stop the periodic steps
     */
    @Override
    public synchronized void close() {
        if (clock != null) {
            clock.shutdownNow();
            clock = null;
        }
    }

    @Override
    public String toString() {
        return "Environment[" + name + "]";
    }
}
