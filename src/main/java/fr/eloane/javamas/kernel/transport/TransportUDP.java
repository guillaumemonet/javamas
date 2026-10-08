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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;

/**
 * Exchange messages with one other node using UDP
 *
 * @author Guillaume Monet
 */
public class TransportUDP extends Transport {

    private static final System.Logger LOGGER = System.getLogger(TransportUDP.class.getName());

    public static final String SERVERIP = "SERVERIP";
    public static final String SERVERPORT = "SERVERPORT";
    public static final String DESTINATIONIP = "DESTINATIONIP";
    public static final String DESTINATIONPORT = "DESTINATIONPORT";

    /**
     * Max size of an UDP datagram
     */
    private static final int MAX_PACKET_SIZE = 65507;

    private final byte[] buffer = new byte[MAX_PACKET_SIZE];
    private final DatagramSocket datagramSocket;
    private final InetSocketAddress destination;

    /**
     *
     * @param parameters SERVERIP, SERVERPORT, DESTINATIONIP, DESTINATIONPORT
     * @throws UncheckedIOException if the socket can't be opened
     */
    public TransportUDP(HashMap<String, String> parameters) {
        super(parameters);
        try {
            InetAddress serverAddress = InetAddress.getByName(parameters.get(SERVERIP));
            int serverPort = Integer.parseInt(parameters.get(SERVERPORT));
            this.destination = new InetSocketAddress(InetAddress.getByName(parameters.get(DESTINATIONIP)), Integer.parseInt(parameters.get(DESTINATIONPORT)));
            this.datagramSocket = new DatagramSocket(serverPort, serverAddress);
        } catch (IOException ex) {
            throw new UncheckedIOException("Can't open UDP transport", ex);
        }
    }

    @Override
    public void sendMessage(Message<?> mess) {
        byte[] msg = serialize(mess);
        try {
            datagramSocket.send(new DatagramPacket(msg, msg.length, destination));
        } catch (IOException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Can't send message " + mess.getId(), e);
        }
    }

    @Override
    public void kill() {
        this.datagramSocket.close();
    }

    @Override
    public Message<?> waitMessage() {
        DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
        try {
            datagramSocket.receive(datagramPacket);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return deserialize(datagramPacket.getData(), datagramPacket.getLength());
    }
}
