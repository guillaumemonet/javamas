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
package fr.eloane.javamas.kernel;

import fr.eloane.javamas.kernel.messages.Message;
import fr.eloane.javamas.kernel.organization.Organization;
import fr.eloane.javamas.kernel.probes.Probe;
import fr.eloane.javamas.kernel.probes.ProbeValue;
import fr.eloane.javamas.kernel.sensors.Sensor;
import fr.eloane.javamas.kernel.sensors.SensorListener;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An agent : it lives in its own thread, communicates with messages, perceives
 * its environment with sensors, is part of an organization (communities,
 * groups, roles) and can be observed with probes.<br />
 * Life cycle : {@link #init()}, {@link #activate()}, {@link #live()},
 * {@link #end()} then the agent is killed. Only {@link #live()} must be
 * implemented.
 *
 * @author Guillaume Monet
 */
public abstract class Agent implements Runnable {

    private static final System.Logger LOGGER = System.getLogger(Agent.class.getName());

    /**
     * Message waiting in the mailbox, ordered by priority then by arrival
     */
    private record Queued(Message<?> message, long sequence) implements Comparable<Queued> {

        @Override
        public int compareTo(Queued other) {
            int priority = other.message.getPriority().compareTo(this.message.getPriority());
            return priority != 0 ? priority : Long.compare(this.sequence, other.sequence);
        }
    }

    private final Node node;
    private final Address address = Address.generate();
    private final String name;
    private final Organization organization = new Organization();
    private final Scheduler scheduler = new Scheduler();
    private final PriorityBlockingQueue<Queued> mailbox = new PriorityBlockingQueue<>();
    private final AtomicLong arrivals = new AtomicLong();
    private final List<Sensor<?>> sensors = new CopyOnWriteArrayList<>();
    private final SensorListener sensorListener = this::handleSensor;
    private final List<Probe> probes = new CopyOnWriteArrayList<>();
    private final AtomicReference<AgentState> state = new AtomicReference<>(AgentState.CREATED);
    private volatile Thread thread;

    /**
     * Agent of the default node, named after its class
     */
    protected Agent() {
        this(null, Node.getDefault());
    }

    /**
     * Agent of the default node
     *
     * @param name public name of the agent
     */
    protected Agent(String name) {
        this(name, Node.getDefault());
    }

    /**
     *
     * @param name public name of the agent, null for the class name
     * @param node the node of the agent
     */
    protected Agent(String name, Node node) {
        this.name = name == null ? getClass().getSimpleName() : name;
        this.node = Objects.requireNonNull(node);
        node.register(this);
    }

    // ------------------------------------------------------------ life cycle
    /**
     * First step of the life cycle
     */
    protected void init() {
    }

    /**
     * Activation : join the organization, add sensors...
     */
    protected void activate() {
    }

    /**
     * Life of the agent, usually a loop on {@link #nextStep()} or
     * {@link #receive()}
     */
    protected abstract void live();

    /**
     * End of life, the agent can still send messages
     */
    protected void end() {
    }

    /**
     * Start the life cycle in a new platform thread
     *
     * @return the thread running the agent
     */
    public final Thread start() {
        return start(Thread.ofPlatform());
    }

    /**
     * Start the life cycle in a new virtual thread.<br />
     * Virtual threads allow a huge number of agents but don't keep the JVM
     * alive.
     *
     * @return the thread running the agent
     */
    public final Thread startVirtual() {
        return start(Thread.ofVirtual());
    }

    /**
     * Start the life cycle in a thread created by the builder
     *
     * @param builder e.g. Thread.ofPlatform().daemon()
     * @return the thread running the agent
     * @throws IllegalStateException if the agent was already started
     */
    public final Thread start(Thread.Builder builder) {
        if (state.get() != AgentState.CREATED || thread != null) {
            throw new IllegalStateException(this + " already started");
        }
        Thread t = builder.name(name).unstarted(this);
        thread = t;
        t.start();
        return t;
    }

    /**
     * Run the whole life cycle in the current thread
     *
     * @throws IllegalStateException if the agent was already started
     */
    @Override
    public final void run() {
        if (!setState(AgentState.CREATED, AgentState.ACTIVATING)) {
            throw new IllegalStateException(this + " already started");
        }
        if (thread == null) {
            thread = Thread.currentThread();
        }
        try {
            init();
            activate();
            if (setState(AgentState.ACTIVATING, AgentState.LIVING)) {
                live();
            }
            if (setState(AgentState.LIVING, AgentState.ENDING)) {
                end();
            }
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.ERROR, this + " failed", e);
        } finally {
            kill();
        }
    }

    /**
     * Kill the agent now : stop its life cycle, unregister it from its node
     * and release its sensors, probes and messages. Called at the end of the
     * life cycle.
     */
    public final void kill() {
        if (state.getAndSet(AgentState.DEAD) == AgentState.DEAD) {
            return;
        }
        publish(ProbeValue.STATE, AgentState.DEAD);
        stop();
        node.unregister(this);
        mailbox.clear();
        sensors.forEach(s -> s.removeListener(sensorListener));
        sensors.clear();
        probes.clear();
    }

    private boolean setState(AgentState expected, AgentState next) {
        if (state.compareAndSet(expected, next)) {
            publish(ProbeValue.STATE, next);
            return true;
        }
        return false;
    }

    public final AgentState getState() {
        return state.get();
    }

    // ------------------------------------------------------------- scheduling
    /**
     * Wait for the next step of the life cycle (delay, pause), to use in
     * {@link #live()}
     *
     * @return false when the agent is stopped
     */
    protected final boolean nextStep() {
        boolean running = scheduler.nextStep();
        if (!running) {
            // Clear the interruption used by stop() to wake the agent up
            Thread.interrupted();
        }
        return running;
    }

    /**
     * Pause until {@link #resume()}
     */
    public final void pause() {
        scheduler.pause();
    }

    /**
     * Pause during a time
     *
     * @param duration
     */
    public final void pause(Duration duration) {
        scheduler.pause(duration);
    }

    public final void resume() {
        scheduler.resume();
    }

    /**
     * Stop the life cycle : {@link #nextStep()} returns false and a blocked
     * {@link #receive()} returns null (the agent's thread is interrupted).
     */
    public final void stop() {
        scheduler.stop();
        Thread t = thread;
        if (t != null && t != Thread.currentThread()) {
            t.interrupt();
        }
    }

    /**
     *
     * @return false if the agent is stopped
     */
    public final boolean isRunning() {
        return scheduler.isRunning();
    }

    /**
     *
     * @param delay delay between each step
     */
    public final void setDelay(Duration delay) {
        scheduler.setDelay(delay);
    }

    // -------------------------------------------------------------- messages
    /**
     * Send a message : the sender is set on a copy of the message
     *
     * @param message
     */
    public final void send(Message<?> message) {
        node.send(message.copy().sender(address.id()));
    }

    /**
     * Wait for a message
     *
     * @return the message with the highest priority, or null if the agent is
     * stopped
     */
    protected final Message<?> receive() {
        return receive(null);
    }

    /**
     * Wait for a message during a time
     *
     * @param timeout null to wait without limit
     * @return the message with the highest priority, or null if no message
     * was received in time or the agent is stopped
     */
    protected final Message<?> receive(Duration timeout) {
        long deadline = timeout == null ? 0 : System.nanoTime() + timeout.toNanos();
        try {
            while (true) {
                Queued queued;
                if (!scheduler.isRunning()) {
                    queued = mailbox.poll();
                } else if (timeout == null) {
                    queued = mailbox.take();
                } else {
                    queued = mailbox.poll(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
                }
                if (queued == null) {
                    return null;
                }
                mailboxChanged();
                if (!queued.message().isExpired(Instant.now())) {
                    return queued.message();
                }
            }
        } catch (InterruptedException e) {
            if (scheduler.isRunning()) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    /**
     * Put a message in the mailbox, called by the node
     *
     * @param message
     */
    final void deliver(Message<?> message) {
        if (state.get() == AgentState.DEAD) {
            return;
        }
        mailbox.put(new Queued(message, arrivals.getAndIncrement()));
        mailboxChanged();
    }

    /**
     *
     * @return the number of messages waiting in the mailbox
     */
    public final int pendingMessages() {
        return mailbox.size();
    }

    /**
     * Remove all the messages waiting in the mailbox
     */
    public final void clearMailbox() {
        mailbox.clear();
        mailboxChanged();
    }

    private void mailboxChanged() {
        if (!probes.isEmpty()) {
            publish(ProbeValue.MAILBOX_SIZE, mailbox.size());
        }
    }

    // ----------------------------------------------------- sensors and probes
    /**
     * Add a sensor : {@link #handleSensor(Sensor)} is called when its value
     * changes
     *
     * @param sensor
     */
    public final void addSensor(Sensor<?> sensor) {
        sensors.add(sensor);
        sensor.addListener(sensorListener);
    }

    public final void removeSensor(Sensor<?> sensor) {
        sensor.removeListener(sensorListener);
        sensors.remove(sensor);
    }

    public final List<Sensor<?>> getSensors() {
        return Collections.unmodifiableList(sensors);
    }

    /**
     * Called in the thread changing the value of a sensor of the agent
     *
     * @param sensor
     */
    protected void handleSensor(Sensor<?> sensor) {
    }

    /**
     * Observe the values published by the agent
     *
     * @param probe
     */
    public final void addProbe(Probe probe) {
        probes.add(probe);
    }

    public final void removeProbe(Probe probe) {
        probes.remove(probe);
    }

    /**
     * Publish a value to the probes of the agent
     *
     * @param valueName
     * @param value
     */
    protected final void publish(String valueName, Object value) {
        if (probes.isEmpty()) {
            return;
        }
        ProbeValue probeValue = new ProbeValue(address, valueName, value, Instant.now());
        for (Probe probe : probes) {
            try {
                probe.handleProbe(probeValue);
            } catch (RuntimeException e) {
                LOGGER.log(System.Logger.Level.WARNING, "Probe of " + this + " failed", e);
            }
        }
    }

    // ------------------------------------------------------------------ info
    public final Address getAddress() {
        return address;
    }

    public final String getName() {
        return name;
    }

    public final Node getNode() {
        return node;
    }

    /**
     *
     * @return the communities, groups and roles of the agent
     */
    public final Organization getOrganization() {
        return organization;
    }

    /**
     * Print a line prefixed by the name of the agent
     *
     * @param o
     */
    protected final void println(Object o) {
        System.out.println(name + ": " + o);
    }

    @Override
    public String toString() {
        return name + "@" + address;
    }
}
