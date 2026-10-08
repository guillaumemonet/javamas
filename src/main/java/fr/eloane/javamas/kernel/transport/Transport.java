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
package fr.eloane.javamas.kernel.transport;

import fr.eloane.javamas.kernel.messages.Message;
import java.io.IOException;
import java.util.Objects;

/**
 * Exchanges messages with other nodes.<br />
 * A transport is started by the node it is added to, receives messages in
 * its own threads and gives them to the node.
 *
 * @author Guillaume Monet
 */
public abstract class Transport implements AutoCloseable {

    private static final System.Logger LOGGER = System.getLogger(Transport.class.getName());

    protected final MessageCodec codec;
    private volatile MessageListener listener;
    private volatile boolean closed = false;

    /**
     *
     * @param codec converts messages to bytes and back
     */
    protected Transport(MessageCodec codec) {
        this.codec = Objects.requireNonNull(codec);
    }

    /**
     * Start receiving messages
     *
     * @param listener receives the messages
     * @throws IllegalStateException if the transport is already started or
     * closed
     * @throws java.io.UncheckedIOException if the transport can't be opened
     */
    public final synchronized void start(MessageListener listener) {
        if (this.listener != null || closed) {
            throw new IllegalStateException("Transport already started or closed");
        }
        this.listener = Objects.requireNonNull(listener);
        open();
    }

    /**
     * Open the sockets and start the receiving threads
     *
     * @throws java.io.UncheckedIOException if the transport can't be opened
     */
    protected abstract void open();

    /**
     * Send a message to the other nodes. Errors are logged, a message can be
     * lost.
     *
     * @param message
     */
    public abstract void send(Message<?> message);

    /**
     *
     * @return true if a message received from a peer must be forwarded to the
     * other peers of this transport (point to point links), false if all the
     * peers already received it (broadcast medium)
     */
    public boolean forwardsBetweenPeers() {
        return false;
    }

    /**
     * Stop receiving and release the resources
     */
    @Override
    public final void close() {
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
        }
        release();
    }

    /**
     * Release the resources of the transport
     */
    protected abstract void release();

    public final boolean isClosed() {
        return closed;
    }

    /**
     * To call by the implementations when data is received
     *
     * @param data
     * @param offset
     * @param length
     */
    protected final void received(byte[] data, int offset, int length) {
        Message<?> message;
        try {
            message = codec.decode(data, offset, length);
        } catch (IOException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Invalid message dropped by " + this + " : " + e);
            return;
        }
        MessageListener l = listener;
        if (l != null && !closed) {
            l.messageReceived(message, this);
        }
    }

    /**
     *
     * @param message
     * @return the encoded message or null if it can't be encoded (logged)
     */
    protected final byte[] encode(Message<?> message) {
        try {
            return codec.encode(message);
        } catch (IOException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Can't encode message " + message.getId() + " : " + e);
            return null;
        }
    }

    /**
     * Start a daemon thread
     *
     * @param name
     * @param task
     * @return the thread
     */
    protected static Thread startThread(String name, Runnable task) {
        return Thread.ofPlatform().name("javamas-" + name).daemon().start(task);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
