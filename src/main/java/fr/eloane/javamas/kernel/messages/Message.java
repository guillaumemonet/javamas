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

import fr.eloane.javamas.kernel.organization.Organization;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Project: JavaMAS: Java Multi-Agents System File: Message.java<br />
 * The envelope of the message is stored as map fields, the content is typed.
 *
 * @param <T> type of the content
 */
public class Message<T> extends HashMap<String, Object> implements Cloneable, Serializable, Comparable<Message<?>> {

    public static final String MESSAGE_ID = "message-id";
    public static final String IN_REPLY_TO = "in-reply-to";
    public static final String REPLY_BY = "reply_by";
    /**
     * Participant in communication
     */
    public static final String REPLY_TO = "reply-to";
    /**
     * Description of Content
     */
    public static final String LANGUAGE = "language";
    /**
     * Description of Content
     */
    public static final String ENCODING = "encoding";
    /**
     * Description of Content
     */
    public static final String ONTOLOGY = "ontology";
    /**
     * Control of conversation
     */
    public static final String PROTOCOL = "protocol";
    /**
     * Control of conversation
     */
    public static final String REPLY_WITH = "reply-with";
    public static final String CONVERSATION_ID = "conversation_id";
    public static final String PRIORITY = "priority";
    public static final String EXPIRE = "expire";
    /**
     * @deprecated misspelled, use {@link #EXPIRE}
     */
    @Deprecated
    public static final String EXPRIRE = EXPIRE;
    public static final String CREATED = "created";
    public static final String SENDER = "sender";
    public static final String RECEIVERS = "receivers";
    public static final String RECEIVERS_ORGANIZATIONS = "receivers-organization";

    public static final int HIGH_PRIORITY = 1;
    public static final int LOW_PRIORITY = -1;
    public static final int NORMAL_PRIORITY = 0;
    public static final int EXTREM_PRIORITY = 2;

    @Serial
    private static final long serialVersionUID = -1322293292088397862L;

    /**
     *
     */
    protected T content = null;

    /**
     *
     */
    public Message() {
        this.put(Message.PRIORITY, NORMAL_PRIORITY);
        this.put(Message.CREATED, System.currentTimeMillis());
        this.put(Message.MESSAGE_ID, "M:" + UUID.randomUUID());
        this.put(Message.CONVERSATION_ID, "C:" + UUID.randomUUID());
    }

    /**
     *
     * @param content
     */
    public Message(T content) {
        this();
        this.setContent(content);
    }

    /**
     *
     * @param content
     * @param expire
     */
    public Message(T content, long expire) {
        this();
        this.setContent(content);
        this.put(Message.EXPIRE, expire);
    }

    /**
     *
     * @param expire
     */
    public Message(long expire) {
        this();
        this.put(Message.EXPIRE, expire);
    }

    /**
     *
     * @return the unique id of the message
     */
    public final String getId() {
        return this.get(MESSAGE_ID).toString();
    }

    /**
     *
     * @return
     */
    public final T getContent() {
        return this.content;
    }

    /**
     *
     * @param content
     * @return
     */
    public final Message<T> setContent(T content) {
        this.content = content;
        return this;
    }

    /**
     * @param s
     * @return
     */
    public final Message<T> setSender(String s) {
        this.put(Message.SENDER, s);
        return this;
    }

    /**
     * Returns the original sender. This information can be trusted
     *
     * @return
     */
    public final String getSender() {
        return (String) this.get(Message.SENDER);
    }

    /**
     *
     * @param s
     * @return
     */
    public final Message<T> addReceiver(String s) {
        ArrayList<String> receivers = this.getReceivers();
        receivers.add(s);
        this.setReceivers(receivers);
        return this;
    }

    /**
     *
     * @param s
     * @return
     */
    public final Message<T> removeReceiver(String s) {
        ArrayList<String> receivers = this.getReceivers();
        receivers.remove(s);
        this.setReceivers(receivers);
        return this;
    }

    /**
     *
     * @param r
     * @return
     */
    public final Message<T> addReceivers(ArrayList<String> r) {
        ArrayList<String> receivers = this.getReceivers();
        receivers.addAll(r);
        this.setReceivers(receivers);
        return this;
    }

    /**
     *
     * @param r
     * @return
     */
    public final Message<T> setReceivers(ArrayList<String> r) {
        this.put(Message.RECEIVERS, new ArrayList<>(r));
        return this;
    }

    /**
     * @return a copy of the receivers ids
     */
    public final ArrayList<String> getReceivers() {
        return this.getList(Message.RECEIVERS);
    }

    /**
     *
     * @return
     */
    public final Message<T> removeReceivers() {
        this.remove(Message.RECEIVERS);
        return this;
    }

    /**
     *
     * @param t
     * @return
     */
    public final Message<T> addOrganization(Organization t) {
        ArrayList<Organization> organizations = this.getOrganizations();
        organizations.add(t);
        this.setOrganizations(organizations);
        return this;
    }

    /**
     *
     * @param t
     * @return
     */
    public final Message<T> removeOrganization(Organization t) {
        ArrayList<Organization> organizations = this.getOrganizations();
        organizations.remove(t);
        this.setOrganizations(organizations);
        return this;
    }

    /**
     *
     * @param t
     * @return
     */
    public final Message<T> setOrganization(Organization t) {
        ArrayList<Organization> organizations = new ArrayList<>();
        organizations.add(t);
        return this.setOrganizations(organizations);
    }

    /**
     *
     * @param organizations
     * @return
     */
    public final Message<T> setOrganizations(ArrayList<Organization> organizations) {
        this.put(Message.RECEIVERS_ORGANIZATIONS, new ArrayList<>(organizations));
        return this;
    }

    /**
     *
     * @return
     */
    public final Message<T> removeOrganizations() {
        this.remove(RECEIVERS_ORGANIZATIONS);
        return this;
    }

    /**
     *
     * @return a copy of the receivers organizations
     */
    public final ArrayList<Organization> getOrganizations() {
        return this.getList(Message.RECEIVERS_ORGANIZATIONS);
    }

    @SuppressWarnings("unchecked")
    private <E> ArrayList<E> getList(String type) {
        Collection<E> array = (Collection<E>) this.get(type);
        return array == null ? new ArrayList<>() : new ArrayList<>(array);
    }

    private long getLong(String key, long defaultValue) {
        Object value = this.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value instanceof Number n ? n.longValue() : Long.parseLong(value.toString());
    }

    /**
     *
     * @return
     */
    public final int getPriority() {
        return (int) this.getLong(Message.PRIORITY, NORMAL_PRIORITY);
    }

    /**
     *
     * @param priority
     * @return
     */
    public final Message<T> setPriority(int priority) {
        this.put(Message.PRIORITY, priority);
        return this;
    }

    /**
     *
     * @return creation
     */
    public final long getTime() {
        return this.getLong(Message.CREATED, 0);
    }

    /**
     *
     * @return expire date or 0 if the message never expires
     */
    public final long getExpire() {
        return this.getLong(Message.EXPIRE, 0);
    }

    /**
     *
     * @param key
     * @param value
     * @return
     */
    public final Message<T> setField(String key, Object value) {
        if (!key.equals(Message.CREATED) && !key.equals(Message.MESSAGE_ID)) {
            this.put(key, value);
        }
        return this;
    }

    /**
     * Copy of the message (same id), the receivers and organizations lists are
     * copied, the content is shared
     *
     * @return
     */
    @Override
    @SuppressWarnings("unchecked")
    public Message<T> clone() {
        Message<T> clone = (Message<T>) super.clone();
        if (this.containsKey(RECEIVERS)) {
            clone.put(RECEIVERS, this.getReceivers());
        }
        if (this.containsKey(RECEIVERS_ORGANIZATIONS)) {
            clone.put(RECEIVERS_ORGANIZATIONS, this.getOrganizations());
        }
        return clone;
    }

    /**
     * Order by priority (highest first) then by time (oldest first)
     *
     * @param mess
     * @return
     */
    @Override
    public int compareTo(Message<?> mess) {
        int priority = Integer.compare(mess.getPriority(), this.getPriority());
        return priority != 0 ? priority : Long.compare(this.getTime(), mess.getTime());
    }

    /**
     *
     * @param o
     * @return if the messages have the same id
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof Message<?> m && Objects.equals(this.get(MESSAGE_ID), m.get(MESSAGE_ID));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.get(MESSAGE_ID));
    }

    /**
     * Reply this message
     *
     * @return a new message with sender id as receiver and conversation id and
     * same fields
     */
    public Message<T> reply() {
        Message<T> reply = new Message<>();
        reply.addReceiver(this.getSender());
        reply.setField(Message.CONVERSATION_ID, this.get(Message.CONVERSATION_ID));
        return reply;
    }

    /**
     * Returns a debug string with enveloppe and content for the message
     *
     * @return message's content + other info
     */
    @Override
    public String toString() {
        return this.entrySet().stream()
                .map(s -> s.getKey() + ":" + String.valueOf(s.getValue()).replace("\n", "") + "\n")
                .collect(Collectors.joining())
                + "Content:\n" + this.getContent() + "\n";
    }
}
