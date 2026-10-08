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

/**
 * Exchanges messages with one other node using UDP
 *
 * @author Guillaume Monet
 */
public final class UdpTransport extends DatagramTransport {

    private final InetSocketAddress bind;

    /**
     *
     * @param bind local address and port receiving the messages
     * @param destination address and port of the other node
     */
    public UdpTransport(InetSocketAddress bind, InetSocketAddress destination) {
        this(bind, destination, new JavaSerializationCodec());
    }

    /**
     *
     * @param bind local address and port receiving the messages
     * @param destination address and port of the other node
     * @param codec
     */
    public UdpTransport(InetSocketAddress bind, InetSocketAddress destination, MessageCodec codec) {
        super(destination, codec);
        this.bind = bind;
    }

    @Override
    protected DatagramSocket createSocket() throws IOException {
        return new DatagramSocket(bind);
    }
}
