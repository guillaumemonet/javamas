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
import fr.eloane.javamas.kernel.organization.Target;
import fr.eloane.javamas.kernel.transport.Transport;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A node hosts agents : it delivers the messages to its agents and exchanges
 * messages with the other nodes through its transports.<br />
 * Messages received from a transport are forwarded to the other transports
 * (and to the other peers of point to point transports) while their time to
 * live allows it. Each message is delivered only once to an agent.<br />
 * Agents use the {@link #getDefault() default node} unless another node is
 * given; several nodes can live in the same JVM.
 *
 * @author Guillaume Monet
 */
public final class Node implements AutoCloseable {

    private static final System.Logger LOGGER = System.getLogger(Node.class.getName());

    /**
     * Number of message ids remembered to avoid delivering a message twice
     */
    private static final int MAX_SEEN_MESSAGES = 10_000;

    private static Node defaultNode = null;

    private final String name;
    private final Map<String, Agent> agents = new ConcurrentHashMap<>();
    private final List<Transport> transports = new CopyOnWriteArrayList<>();
    private final Set<String> seen = Collections.synchronizedSet(Collections.newSetFromMap(new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > MAX_SEEN_MESSAGES;
        }
    }));
    private volatile boolean closed = false;

    public Node() {
        this("node");
    }

    /**
     *
     * @param name name of the node, for the logs
     */
    public Node(String name) {
        this.name = name;
    }

    /**
     *
     * @return the default node of the JVM, created if needed
     */
    public static synchronized Node getDefault() {
        if (defaultNode == null || defaultNode.closed) {
            defaultNode = new Node("default");
        }
        return defaultNode;
    }

    // ---------------------------------------------------------------- agents
    void register(Agent agent) {
        if (closed) {
            throw new IllegalStateException(this + " is closed");
        }
        agents.put(agent.getAddress().id(), agent);
    }

    void unregister(Agent agent) {
        agents.remove(agent.getAddress().id());
    }

    /**
     *
     * @param id
     * @return the agent with this id or null
     */
    public Agent getAgent(String id) {
        return agents.get(id);
    }

    public Collection<Agent> getAgents() {
        return Collections.unmodifiableCollection(agents.values());
    }

    public void pauseAll() {
        agents.values().forEach(Agent::pause);
    }

    public void resumeAll() {
        agents.values().forEach(Agent::resume);
    }

    public void stopAll() {
        agents.values().forEach(Agent::stop);
    }

    public void setDelayAll(Duration delay) {
        agents.values().forEach(a -> a.setDelay(delay));
    }

    // ------------------------------------------------------------ transports
    /**
     * Add and start a transport
     *
     * @param transport
     * @throws java.io.UncheckedIOException if the transport can't be opened
     */
    public void addTransport(Transport transport) {
        transports.add(transport);
        try {
            transport.start(this::received);
        } catch (RuntimeException e) {
            transports.remove(transport);
            throw e;
        }
    }

    /**
     * Remove and close a transport
     *
     * @param transport
     */
    public void removeTransport(Transport transport) {
        transports.remove(transport);
        transport.close();
    }

    public List<Transport> getTransports() {
        return Collections.unmodifiableList(transports);
    }

    // -------------------------------------------------------------- messages
    /**
     * Deliver a message to the local agents and send it to the other nodes
     *
     * @param message
     */
    public void send(Message<?> message) {
        seen.add(message.getId());
        deliver(message);
        for (Transport t : transports) {
            t.send(message);
        }
    }

    private void received(Message<?> message, Transport from) {
        if (!seen.add(message.getId())) {
            return;
        }
        deliver(message);
        if (message.getTtl() > 1) {
            Message<?> relay = message.copy().ttl(message.getTtl() - 1);
            for (Transport t : transports) {
                if (t != from || from.forwardsBetweenPeers()) {
                    t.send(relay);
                }
            }
        }
    }

    /**
     * Deliver a copy of the message to its receivers and to the agents of its
     * targets (except the sender)
     */
    private void deliver(Message<?> message) {
        if (message.isExpired(Instant.now())) {
            LOGGER.log(System.Logger.Level.DEBUG, () -> "Expired message dropped " + message.getId());
            return;
        }
        Set<Agent> recipients = new LinkedHashSet<>();
        for (String id : message.getReceivers()) {
            Agent agent = agents.get(id);
            if (agent != null) {
                recipients.add(agent);
            }
        }
        for (Target target : message.getTargets()) {
            for (Agent agent : agents.values()) {
                if (!agent.getAddress().id().equals(message.getSender()) && agent.getOrganization().matches(target)) {
                    recipients.add(agent);
                }
            }
        }
        recipients.forEach(agent -> agent.deliver(message.copy()));
    }

    /**
     * Stop all the agents and close the transports
     */
    @Override
    public void close() {
        closed = true;
        stopAll();
        transports.forEach(Transport::close);
        transports.clear();
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public String toString() {
        return "Node[" + name + "]";
    }
}
