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

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

/**
 * Exchanges messages with all the nodes of a multicast group (local network)
 *
 * @author Guillaume Monet
 */
public final class MulticastTransport extends DatagramTransport {

    private final InetSocketAddress group;
    private final NetworkInterface networkInterface;
    private final int timeToLive;

    /**
     *
     * @param group multicast address and port, e.g. 239.255.80.84:7889
     */
    public MulticastTransport(InetSocketAddress group) {
        this(group, null, 1, new JavaSerializationCodec());
    }

    /**
     *
     * @param group multicast address and port
     * @param networkInterface interface to use, null for the default one
     * @param timeToLive number of routers the datagrams can cross, 1 for the
     * local network
     * @param codec
     */
    public MulticastTransport(InetSocketAddress group, NetworkInterface networkInterface, int timeToLive, MessageCodec codec) {
        super(group, codec);
        if (!group.getAddress().isMulticastAddress()) {
            throw new IllegalArgumentException(group + " is not a multicast address");
        }
        this.group = group;
        this.networkInterface = networkInterface;
        this.timeToLive = timeToLive;
    }

    @Override
    protected DatagramSocket createSocket() throws IOException {
        MulticastSocket socket = new MulticastSocket(group.getPort());
        socket.setTimeToLive(timeToLive);
        socket.joinGroup(group, networkInterface);
        return socket;
    }
}
