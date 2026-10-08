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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Observable;

/**
 * Exchange messages with the other nodes.<br />
 * A transport runs a thread waiting for messages and notifies its observers
 * for each message received.
 *
 * @author Guillaume Monet
 */
@SuppressWarnings("deprecation")
public abstract class Transport extends Observable implements Runnable {

    private static final System.Logger LOGGER = System.getLogger(Transport.class.getName());

    private volatile boolean stop = false;
    protected HashMap<String, String> parameters;

    /**
     *
     * @param parameters configuration of the transport
     */
    public Transport(HashMap<String, String> parameters) {
        this.parameters = parameters;
    }

    /**
     * Start to wait for messages in a daemon thread
     */
    public final void start() {
        Thread.ofPlatform().name(getClass().getSimpleName()).daemon().start(this);
    }

    /**
     * Send a message to the other nodes
     *
     * @param mess
     */
    public abstract void sendMessage(Message<?> mess);

    /**
     * Stop waiting for messages and release the resources
     */
    public void close() {
        stop = true;
        kill();
    }

    /**
     *
     * @return if the transport is closed
     */
    public final boolean isClosed() {
        return stop;
    }

    /**
     * Release the resources of the transport
     */
    public abstract void kill();

    @Override
    public final void run() {
        while (!stop) {
            try {
                Message<?> message = waitMessage();
                if (message != null) {
                    this.setChanged();
                    this.notifyObservers(message);
                }
            } catch (UncheckedIOException e) {
                if (!stop && e.getCause() instanceof SocketException) {
                    LOGGER.log(System.Logger.Level.ERROR, getClass().getSimpleName() + " socket closed", e);
                    stop = true;
                } else if (!stop) {
                    LOGGER.log(System.Logger.Level.WARNING, "Invalid message received on " + getClass().getSimpleName(), e);
                }
            } catch (ClassCastException e) {
                if (!stop) {
                    LOGGER.log(System.Logger.Level.WARNING, "Invalid message received on " + getClass().getSimpleName(), e);
                }
            }
        }
    }

    /**
     * Block until a message is received
     *
     * @return the message received or null
     * @throws UncheckedIOException if the message can't be read
     */
    public abstract Message<?> waitMessage();

    /**
     *
     * @param mess
     * @return the serialized message
     */
    protected static byte[] serialize(Message<?> mess) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bout)) {
            out.writeObject(mess);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bout.toByteArray();
    }

    /**
     * Deserialize a message received from the network, only allowed classes
     * are accepted
     *
     * @param data
     * @param length
     * @return the message
     * @throws UncheckedIOException if the message can't be read or contains a
     * class not allowed
     * @see SecureObjectInputStream
     */
    protected static Message<?> deserialize(byte[] data, int length) {
        try (ObjectInputStream in = new SecureObjectInputStream(new ByteArrayInputStream(data, 0, length))) {
            return (Message<?>) in.readObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ClassNotFoundException e) {
            throw new UncheckedIOException(new InvalidClassException(e.getMessage()));
        }
    }
}
