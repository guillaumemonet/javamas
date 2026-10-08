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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Connects nodes of the same JVM through a {@link Hub}, for tests and
 * simulations.<br />
 * Messages are encoded and decoded with the codec like on a network, and
 * delivered by the thread sending them.
 *
 * @author Guillaume Monet
 */
public final class InMemoryTransport extends Transport {

    /**
     * Shared medium : a message sent by a transport is received by all the
     * other transports of the hub
     */
    public static final class Hub {

        private final Set<InMemoryTransport> transports = ConcurrentHashMap.newKeySet();
    }

    private final Hub hub;

    /**
     *
     * @param hub
     */
    public InMemoryTransport(Hub hub) {
        this(hub, new JavaSerializationCodec());
    }

    /**
     *
     * @param hub
     * @param codec
     */
    public InMemoryTransport(Hub hub, MessageCodec codec) {
        super(codec);
        this.hub = hub;
    }

    @Override
    protected void open() {
        hub.transports.add(this);
    }

    @Override
    public void send(Message<?> message) {
        byte[] data = encode(message);
        if (data == null || isClosed()) {
            return;
        }
        for (InMemoryTransport t : hub.transports) {
            if (t != this) {
                t.received(data, 0, data.length);
            }
        }
    }

    @Override
    protected void release() {
        hub.transports.remove(this);
    }
}
