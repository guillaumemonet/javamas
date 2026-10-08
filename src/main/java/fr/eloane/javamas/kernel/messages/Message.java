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
package fr.eloane.javamas.kernel.messages;

import fr.eloane.javamas.kernel.Address;
import fr.eloane.javamas.kernel.organization.Target;
import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Message exchanged by agents.<br />
 * The envelope (id, sender, receivers, targets, priority, expiration...) is
 * typed, free headers can be added (see the FIPA header names) and the content
 * is typed.<br />
 * A message is delivered to its receivers (agent addresses) and to the agents
 * matching its targets (community, group, role).
 *
 * @param <T> type of the content
 * @author Guillaume Monet
 */
public class Message<T> implements Serializable, Cloneable {

    @Serial
    private static final long serialVersionUID = 2L;

    /**
     * Number of relays between transports allowed by default
     */
    public static final int DEFAULT_TTL = 4;

    // FIPA header names
    public static final String IN_REPLY_TO = "in-reply-to";
    public static final String REPLY_BY = "reply-by";
    public static final String REPLY_TO = "reply-to";
    public static final String REPLY_WITH = "reply-with";
    public static final String LANGUAGE = "language";
    public static final String ENCODING = "encoding";
    public static final String ONTOLOGY = "ontology";
    public static final String PROTOCOL = "protocol";

    private final String id;
    private final Instant created;
    private String conversationId;
    private String sender;
    private ArrayList<String> receivers = new ArrayList<>();
    private ArrayList<Target> targets = new ArrayList<>();
    private Priority priority = Priority.NORMAL;
    private Instant expiresAt;
    private int ttl = DEFAULT_TTL;
    private HashMap<String, Serializable> headers = new HashMap<>();
    private T content;

    /**
     * Message without content
     */
    public Message() {
        this(null);
    }

    /**
     *
     * @param content
     */
    public Message(T content) {
        this.id = "M:" + UUID.randomUUID();
        this.created = Instant.now();
        this.conversationId = "C:" + UUID.randomUUID();
        this.content = content;
    }

    /**
     * Rebuild a message from its envelope, e.g. received from the network
     *
     * @param envelope
     * @param content
     */
    protected Message(Envelope envelope, T content) {
        this.id = envelope.id();
        this.created = envelope.created();
        this.conversationId = envelope.conversationId();
        this.sender = envelope.sender();
        this.receivers.addAll(envelope.receivers());
        this.targets.addAll(envelope.targets());
        this.priority = envelope.priority();
        this.expiresAt = envelope.expiresAt();
        this.ttl = envelope.ttl();
        this.headers.putAll(envelope.headers());
        this.content = content;
    }

    /**
     * Rebuild a message from its envelope, e.g. received from the network
     *
     * @param <T> type of the content
     * @param envelope
     * @param content
     * @return the message
     */
    public static <T> Message<T> restore(Envelope envelope, T content) {
        return new Message<>(envelope, content);
    }

    /**
     *
     * @return everything but the content
     */
    public Envelope getEnvelope() {
        return new Envelope(id, created, conversationId, sender, receivers, targets, priority, expiresAt, ttl, headers);
    }

    /**
     * Copy of the message : same id, same envelope (copied), same content
     * instance
     *
     * @return the copy
     */
    @SuppressWarnings("unchecked")
    public Message<T> copy() {
        try {
            Message<T> copy = (Message<T>) super.clone();
            copy.receivers = new ArrayList<>(receivers);
            copy.targets = new ArrayList<>(targets);
            copy.headers = new HashMap<>(headers);
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Reply to this message : sent to the sender, same conversation
     *
     * @param <R> type of the content of the reply
     * @param content content of the reply
     * @return the reply
     */
    public <R> Message<R> reply(R content) {
        Message<R> reply = new Message<>(content);
        reply.conversationId = this.conversationId;
        reply.header(IN_REPLY_TO, this.id);
        if (this.sender != null) {
            reply.to(this.sender);
        }
        return reply;
    }

    /**
     * Add a receiver
     *
     * @param address address of the receiver
     * @return this
     */
    public Message<T> to(Address address) {
        return to(address.id());
    }

    /**
     * Add a receiver
     *
     * @param agentId id of the receiver
     * @return this
     */
    public Message<T> to(String agentId) {
        if (!receivers.contains(agentId)) {
            receivers.add(Objects.requireNonNull(agentId));
        }
        return this;
    }

    /**
     * Send the message to all the agents matching the target
     *
     * @param target community, group or role
     * @return this
     */
    public Message<T> to(Target target) {
        if (!targets.contains(target)) {
            targets.add(Objects.requireNonNull(target));
        }
        return this;
    }

    /**
     *
     * @param priority
     * @return this
     */
    public Message<T> priority(Priority priority) {
        this.priority = Objects.requireNonNull(priority);
        return this;
    }

    /**
     * The message is dropped if it is not delivered before the duration
     *
     * @param duration
     * @return this
     */
    public Message<T> expiresAfter(Duration duration) {
        return expiresAt(created.plus(duration));
    }

    /**
     *
     * @param instant expiration time, null for no expiration
     * @return this
     */
    public Message<T> expiresAt(Instant instant) {
        this.expiresAt = instant;
        return this;
    }

    /**
     *
     * @param ttl number of relays between transports allowed
     * @return this
     */
    public Message<T> ttl(int ttl) {
        this.ttl = ttl;
        return this;
    }

    /**
     *
     * @param name
     * @param value null to remove the header
     * @return this
     */
    public Message<T> header(String name, Serializable value) {
        if (value == null) {
            headers.remove(name);
        } else {
            headers.put(name, value);
        }
        return this;
    }

    /**
     *
     * @param content
     * @return this
     */
    public Message<T> content(T content) {
        this.content = content;
        return this;
    }

    /**
     *
     * @param conversationId
     * @return this
     */
    public Message<T> conversation(String conversationId) {
        this.conversationId = Objects.requireNonNull(conversationId);
        return this;
    }

    /**
     * Set by the agent sending the message
     *
     * @param sender id of the sender
     * @return this
     */
    public Message<T> sender(String sender) {
        this.sender = sender;
        return this;
    }

    public String getId() {
        return id;
    }

    public Instant getCreated() {
        return created;
    }

    public String getConversationId() {
        return conversationId;
    }

    /**
     *
     * @return the id of the sender, null if the message was not sent yet
     */
    public String getSender() {
        return sender;
    }

    public List<String> getReceivers() {
        return Collections.unmodifiableList(receivers);
    }

    public List<Target> getTargets() {
        return Collections.unmodifiableList(targets);
    }

    public Priority getPriority() {
        return priority;
    }

    /**
     *
     * @return the expiration time or null
     */
    public Instant getExpiresAt() {
        return expiresAt;
    }

    /**
     *
     * @param now
     * @return if the message is expired at this time
     */
    public boolean isExpired(Instant now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }

    public int getTtl() {
        return ttl;
    }

    /**
     *
     * @param name
     * @return the header value or null
     */
    public Serializable getHeader(String name) {
        return headers.get(name);
    }

    public Map<String, Serializable> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public T getContent() {
        return content;
    }

    /**
     * Two messages are equal if they have the same id
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof Message<?> m && id.equals(m.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[id=" + id + ", from=" + sender + ", to=" + receivers + targets
                + ", priority=" + priority + (headers.isEmpty() ? "" : ", headers=" + headers)
                + ", content=" + content + "]";
    }
}
