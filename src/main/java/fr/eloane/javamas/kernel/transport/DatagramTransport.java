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
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;

/**
 * Transport sending each message in one UDP datagram (max 65507 bytes)
 *
 * @author Guillaume Monet
 */
public abstract sealed class DatagramTransport extends Transport permits UdpTransport, MulticastTransport {

    private static final System.Logger LOGGER = System.getLogger(DatagramTransport.class.getName());

    /**
     * Max size of an UDP datagram
     */
    public static final int MAX_PACKET_SIZE = 65507;

    private final SocketAddress destination;
    private volatile DatagramSocket socket;

    /**
     *
     * @param destination where the messages are sent
     * @param codec
     */
    protected DatagramTransport(SocketAddress destination, MessageCodec codec) {
        super(codec);
        this.destination = destination;
    }

    /**
     *
     * @return the socket receiving the messages
     * @throws IOException
     */
    protected abstract DatagramSocket createSocket() throws IOException;

    @Override
    protected void open() {
        try {
            socket = createSocket();
        } catch (IOException e) {
            throw new UncheckedIOException("Can't open " + this, e);
        }
        startThread(toString(), this::receiveLoop);
    }

    private void receiveLoop() {
        byte[] buffer = new byte[MAX_PACKET_SIZE];
        while (!isClosed()) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                if (!isClosed()) {
                    LOGGER.log(System.Logger.Level.ERROR, this + " stopped", e);
                    close();
                }
                return;
            }
            received(packet.getData(), packet.getOffset(), packet.getLength());
        }
    }

    @Override
    public void send(Message<?> message) {
        byte[] data = encode(message);
        if (data == null) {
            return;
        }
        if (data.length > MAX_PACKET_SIZE) {
            LOGGER.log(System.Logger.Level.WARNING, "Message " + message.getId() + " too big for " + this + " : " + data.length + " bytes");
            return;
        }
        try {
            socket.send(new DatagramPacket(data, data.length, destination));
        } catch (IOException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Can't send message " + message.getId() + " with " + this + " : " + e);
        }
    }

    @Override
    protected void release() {
        DatagramSocket s = socket;
        if (s != null) {
            s.close();
        }
    }

    /**
     *
     * @return the local port, -1 if the transport is not started
     */
    public int getLocalPort() {
        DatagramSocket s = socket;
        return s == null ? -1 : s.getLocalPort();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + destination + "]";
    }
}
