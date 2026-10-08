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
import fr.eloane.javamas.kernel.transport.Transport;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Local node : registry of the agents of the JVM, routes messages to local
 * agents and to the other nodes through the transports.
 *
 * @author Guillaume Monet
 */
@SuppressWarnings("deprecation")
public final class Node implements Observer {

    private static final System.Logger LOGGER = System.getLogger(Node.class.getName());

    /**
     * Number of message ids remembered to avoid delivering a message twice
     */
    private static final int MAX_SEEN_MESSAGES = 10000;

    private static Node comm = null;
    private final Map<String, Agent<?>> agents = new ConcurrentHashMap<>();
    private final List<Transport> transports = new CopyOnWriteArrayList<>();
    private final Set<String> seenMessageIds = Collections.synchronizedSet(Collections.newSetFromMap(new LinkedHashMap<String, Boolean>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > MAX_SEEN_MESSAGES;
        }
    }));

    private Node() {
    }

    /**
     *
     * @return the node of the JVM, created if needed
     */
    public static synchronized Node getHandle() {
        if (comm == null) {
            comm = new Node();
        }
        return comm;
    }

    /**
     * Add and start a transport to exchange messages with other nodes
     *
     * @param t
     */
    public void addTransport(Transport t) {
        transports.add(t);
        t.addObserver(this);
        t.start();
    }

    /**
     *
     * @param t
     */
    public void removeTransport(Transport t) {
        t.deleteObserver(this);
        transports.remove(t);
    }

    /**
     *
     * @param agt
     */
    public void register(Agent<?> agt) {
        agents.put(agt.getAddress().getId(), agt);
    }

    /**
     * Unregister an agent, the node is stopped when there is no more agent
     *
     * @param agt
     */
    public synchronized void unregister(Agent<?> agt) {
        agents.remove(agt.getAddress().getId());
        if (agents.isEmpty()) {
            stop();
            synchronized (Node.class) {
                if (Node.comm == this) {
                    Node.comm = null;
                }
            }
            LOGGER.log(System.Logger.Level.DEBUG, "Node killed");
        }
    }

    /**
     *
     */
    public void pauseAll() {
        agents.values().forEach(Agent::pause);
    }

    /**
     *
     */
    public void resumeAll() {
        agents.values().forEach(Agent::resume);
    }

    /**
     *
     */
    public void stopAll() {
        agents.values().forEach(Agent::stop);
    }

    /**
     *
     * @param delay
     */
    public void setDelayAll(int delay) {
        agents.values().forEach(a -> a.setDelay(delay));
    }

    /**
     *
     * @param id
     * @return the agent or null if no agent has this id on the node
     */
    public Agent<?> getAgent(String id) {
        return agents.get(id);
    }

    /**
     *
     * @return the agents registered on the node
     */
    public Collection<Agent<?>> getAgents() {
        return Collections.unmodifiableCollection(agents.values());
    }

    /**
     * Deliver a message to the local agents (receivers and members of the
     * organizations) and broadcast it to the other nodes
     *
     * @param mes
     */
    public void sendMessage(Message<?> mes) {
        // Remember our own messages so they are not delivered again when a
        // transport loops them back (multicast loopback, relay by other nodes)
        this.seenMessageIds.add(mes.getId());
        mes.getReceivers().forEach(id -> {
            Agent<?> agent = agents.get(id);
            if (agent != null) {
                agent.pushMessage(mes);
            }
        });
        mes.getOrganizations().forEach(organisation
                -> agents.values().stream()
                        .filter(agent -> agent.isInOrganization(organisation) && !agent.getAddress().getId().equals(mes.getSender()))
                        .forEach(agent -> agent.pushMessage(mes)));
        this.broadcastMessage(mes);
    }

    private void broadcastMessage(Message<?> mes) {
        transports.forEach(t -> t.sendMessage(mes));
    }

    /**
     * Nothing to do, the node is started on creation
     */
    public void start() {
    }

    /**
     * Close all the transports
     */
    public void stop() {
        transports.forEach(Transport::close);
    }

    /**
     * Message received by a transport
     *
     * @param o the transport
     * @param arg the message
     */
    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof Message<?> mes && this.seenMessageIds.add(mes.getId())) {
            this.sendMessage(mes);
        }
    }
}
