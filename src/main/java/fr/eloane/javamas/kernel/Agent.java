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
import fr.eloane.javamas.kernel.probes.ProbesManager;
import fr.eloane.javamas.kernel.sensors.SensorsManager;
import java.io.Serial;
import java.io.Serializable;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author Guillaume Monet
 * @param <T> type of the values stored in the agent's database
 */
public abstract class Agent<T> extends AbstractAgent implements Serializable {

    @Serial
    private static final long serialVersionUID = -3591756155645176746L;

    /**
     * Description of the probe value published when the messages queue changes
     */
    public static final String MESSAGES_QUEUE_PROBE = "MESSAGES QUEUE";

    /**
     * Message waiting in the queue, ordered by priority then by arrival
     */
    private record QueuedMessage(Message<?> message, long sequence) implements Comparable<QueuedMessage>, Serializable {

        @Override
        public int compareTo(QueuedMessage other) {
            int priority = Integer.compare(other.message.getPriority(), this.message.getPriority());
            return priority != 0 ? priority : Long.compare(this.sequence, other.sequence);
        }
    }

    private final Address address;
    private final SensorsManager sensorsManager = new SensorsManager(this);
    private final ProbesManager probesManager = new ProbesManager(this);
    private final PriorityBlockingQueue<QueuedMessage> messages = new PriorityBlockingQueue<>();
    private final AtomicLong messageSequence = new AtomicLong();
    private final Organization organization = new Organization();
    private final Scheduler scheduler = new Scheduler();
    private Database<T> database = null;
    private String name = "";

    /**
     * Create new Agent
     */
    @SuppressWarnings("this-escape")
    public Agent() {
        this.address = Address.generate();
        this.register();
    }

    /**
     * Create new Agent
     *
     * @param name set the current public name for the agent
     */
    public Agent(String name) {
        this();
        this.name = name;
    }

    /**
     * Create new Agent
     *
     * @param daemon set if the agent is a daemon
     */
    public Agent(boolean daemon) {
        this();
        this.daemon = daemon;
    }

    /**
     * Create new Agent
     *
     * @param name set the current public name for the agent
     * @param daemon set if the agent is a daemon
     */
    public Agent(String name, boolean daemon) {
        this();
        this.name = name;
        this.daemon = daemon;
    }

    /**
     * Register the Agent to the current AgentNode
     */
    private void register() {
        Node.getHandle().register(this);
    }

    /**
     * Unregister the Agent from the current AgentNode
     */
    private void unregister() {
        Node.getHandle().unregister(this);
    }

    /**
     * Kill de agent
     */
    @Override
    protected void kill() {
        this.unregister();
        this.flushMessage();
        this.probesManager.flushProbes();
        this.sensorsManager.flushSensors();
        this.scheduler.stop();
    }

    @Override
    protected String threadName() {
        return (name.isEmpty() ? getClass().getSimpleName() : name) + "-" + address;
    }

    /**
     * Pause the current Agent's life cycle
     *
     * @see #nextStep()
     * @see #stop()
     * @see #resume()
     * @see #pause(long)
     */
    public final void pause() {
        this.scheduler.pause();
    }

    /**
     * Pause the current Agent's life cycle during time
     *
     * @see #nextStep()
     * @see #stop()
     * @see #resume()
     * @see #pause()
     * @param time pause duration in milliseconds
     */
    public final void pause(long time) {
        this.scheduler.pause(time);
    }

    /**
     * Resume the current Agent's life cycle
     *
     * @see #stop()
     * @see #nextStep()
     * @see #pause()
     * @see #pause(long)
     */
    public final void resume() {
        this.scheduler.resume();
    }

    /**
     * Stop the current Agent's life cycle
     *
     * @see #nextStep()
     * @see #pause()
     * @see #pause(long)
     * @see #resume()
     */
    public final void stop() {
        this.scheduler.stop();
    }

    /**
     * Set the current delay beetween each Agent's life cycle
     *
     * @see #nextStep()
     * @see #pause()
     * @see #pause(long)
     * @see #stop()
     * @see #resume()
     * @param delay delay in milliseconds
     */
    public final void setDelay(int delay) {
        this.scheduler.setDelay(delay);
    }

    /**
     * Wait, Stop , Continue the Agent's life cycle<br />
     * Must be use in live() method
     *
     * @see #setDelay(int)
     * @see #pause()
     * @see #stop()
     * @see #resume()
     * @see #live()
     * @return if life cycle can proceed
     */
    public final boolean nextStep() {
        return this.scheduler.nextStep();
    }

    /**
     * launch an other agent synchroneous
     *
     * @param agt an agent from AgentLibrary
     */
    public final void launchAgent(Agent<?> agt) {
        launchAgent(agt, false);
    }

    /**
     * launch an other agent
     *
     * @param agt the Agent to launch
     * @param async if it's asynchroneous or not
     */
    public final void launchAgent(Agent<?> agt, boolean async) {
        if (async) {
            agt.start();
        } else {
            agt.run();
        }
    }

    /**
     * Send message to one agent
     *
     * @param mess
     */
    public final void sendMessage(Message<?> mess) {
        Message<?> m = mess.clone();
        m.setSender(this.address.getId());
        Node.getHandle().sendMessage(m);
    }

    /**
     * wait until receive a message
     *
     * @return a message or null if the agent's thread is interrupted
     */
    public final Message<?> waitNextMessage() {
        return this.waitNextMessage(0);
    }

    /**
     * wait until receive a message or until time
     *
     * @param until time to wait in milliseconds, 0 to wait forever
     * @return a message or null if no message was received in time
     */
    public final Message<?> waitNextMessage(int until) {
        try {
            QueuedMessage queued = until <= 0 ? messages.take() : messages.poll(until, TimeUnit.MILLISECONDS);
            this.notifyMessagesChanged();
            return queued == null ? null : queued.message();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Add message to inner queue
     *
     * @param mes
     */
    public final void pushMessage(Message<?> mes) {
        messages.put(new QueuedMessage(mes, messageSequence.getAndIncrement()));
        this.notifyMessagesChanged();
    }

    /**
     * Remove all messages from queue
     */
    public final void flushMessage() {
        messages.clear();
        this.notifyMessagesChanged();
    }

    private void notifyMessagesChanged() {
        this.probe(MESSAGES_QUEUE_PROBE, this.countMessages());
    }

    /**
     * Publish a value to all the probes of the agent
     *
     * @param description description of the value
     * @param value the value
     */
    protected final void probe(String description, Object value) {
        this.setChanged();
        this.notifyObservers(new AgentProbeValue<>(description, value));
    }

    /**
     * Return the messages queue's size
     *
     * @return messages queue's size
     */
    public final int countMessages() {
        return messages.size();
    }

    /**
     * The agent's persistent database, stored in the user's home and named
     * after the agent's name (or class name if no name is set).
     *
     * @return the database, created and loaded on first call
     */
    public synchronized Database<T> getDatabase() {
        if (this.database == null) {
            this.database = new Database<>(name.isEmpty() ? getClass().getName() : name, true);
        }
        return this.database;
    }

    /**
     * print something in current output
     *
     * @param str something to print
     */
    public final void println(Object str) {
        System.out.println(this.name + this.address + ":" + str);
    }

    /**
     *
     * @return the organization (communities, groups, roles) of the agent
     */
    public final Organization getOrganization() {
        return this.organization;
    }

    /**
     *
     * @param organization
     * @return if the agent is part of the organization
     */
    public final boolean isInOrganization(Organization organization) {
        return this.organization.compare(organization);
    }

    /**
     * Get the current AgentAddress
     *
     * @return
     */
    public Address getAddress() {
        return this.address;
    }

    /**
     *
     * @return
     */
    public ProbesManager getProbesManager() {
        return this.probesManager;
    }

    /**
     *
     * @return
     */
    public SensorsManager getSensorsManager() {
        return this.sensorsManager;
    }

    /**
     *
     * @return
     */
    public Scheduler getScheduler() {
        return this.scheduler;
    }

}
