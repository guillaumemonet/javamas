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
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

/**
 * Exchanges messages with other nodes over TCP connections.<br />
 * The transport listens on a port and connects to a list of peers (connections
 * are opened again when a message is sent after a failure). Messages are sent
 * on all the open connections, received messages are forwarded by the node to
 * the other peers. Use {@link javax.net.ssl.SSLServerSocketFactory} and
 * {@link javax.net.ssl.SSLSocketFactory} for TLS.
 *
 * @author Guillaume Monet
 */
public final class TcpTransport extends Transport {

    private static final System.Logger LOGGER = System.getLogger(TcpTransport.class.getName());

    /**
     * Max size of a message
     */
    public static final int MAX_FRAME_SIZE = 16 * 1024 * 1024;

    private static final int CONNECT_TIMEOUT_MS = 2000;
    private static final long RETRY_DELAY_NANOS = 5_000_000_000L;

    private final int port;
    private final List<InetSocketAddress> peers;
    private final ServerSocketFactory serverSocketFactory;
    private final SocketFactory socketFactory;
    private final Set<Connection> connections = ConcurrentHashMap.newKeySet();
    private final Map<InetSocketAddress, Connection> outgoing = new ConcurrentHashMap<>();
    private final Map<InetSocketAddress, Long> lastFailure = new ConcurrentHashMap<>();
    private volatile ServerSocket server;

    /**
     *
     * @param port port to listen to, 0 for any free port (see
     * {@link #getLocalPort()}), -1 to not accept connections
     * @param peers nodes to connect to
     */
    public TcpTransport(int port, List<InetSocketAddress> peers) {
        this(port, peers, ServerSocketFactory.getDefault(), SocketFactory.getDefault(), new JavaSerializationCodec());
    }

    /**
     *
     * @param port port to listen to, 0 for any free port, -1 to not accept
     * connections
     * @param peers nodes to connect to
     * @param serverSocketFactory e.g. SSLServerSocketFactory for TLS
     * @param socketFactory e.g. SSLSocketFactory for TLS
     * @param codec
     */
    public TcpTransport(int port, List<InetSocketAddress> peers, ServerSocketFactory serverSocketFactory, SocketFactory socketFactory, MessageCodec codec) {
        super(codec);
        this.port = port;
        this.peers = List.copyOf(peers);
        this.serverSocketFactory = serverSocketFactory;
        this.socketFactory = socketFactory;
    }

    @Override
    protected void open() {
        if (port >= 0) {
            try {
                server = serverSocketFactory.createServerSocket(port);
            } catch (IOException e) {
                throw new UncheckedIOException("Can't listen on port " + port, e);
            }
            startThread(this + "-accept", this::acceptLoop);
        }
        peers.forEach(this::connect);
    }

    private void acceptLoop() {
        while (!isClosed()) {
            try {
                addConnection(server.accept());
            } catch (IOException e) {
                if (!isClosed()) {
                    LOGGER.log(System.Logger.Level.ERROR, this + " stopped accepting connections", e);
                }
                return;
            }
        }
    }

    private Connection connect(InetSocketAddress peer) {
        Connection existing = outgoing.get(peer);
        if (existing != null && existing.isOpen()) {
            return existing;
        }
        Long failure = lastFailure.get(peer);
        if (failure != null && System.nanoTime() - failure < RETRY_DELAY_NANOS) {
            return null;
        }
        try {
            Socket socket = socketFactory.createSocket();
            socket.connect(peer, CONNECT_TIMEOUT_MS);
            Connection connection = addConnection(socket);
            outgoing.put(peer, connection);
            lastFailure.remove(peer);
            return connection;
        } catch (IOException e) {
            lastFailure.put(peer, System.nanoTime());
            LOGGER.log(System.Logger.Level.DEBUG, () -> "Can't connect to " + peer + " : " + e);
            return null;
        }
    }

    private Connection addConnection(Socket socket) throws IOException {
        socket.setTcpNoDelay(true);
        Connection connection = new Connection(socket);
        connections.add(connection);
        if (isClosed()) {
            connection.close();
        } else {
            startThread(this + "-" + socket.getRemoteSocketAddress(), connection::readLoop);
        }
        return connection;
    }

    @Override
    public void send(Message<?> message) {
        byte[] data = encode(message);
        if (data == null || isClosed()) {
            return;
        }
        if (data.length > MAX_FRAME_SIZE) {
            LOGGER.log(System.Logger.Level.WARNING, "Message " + message.getId() + " too big : " + data.length + " bytes");
            return;
        }
        peers.forEach(this::connect);
        for (Connection connection : connections) {
            connection.write(data);
        }
    }

    @Override
    public boolean forwardsBetweenPeers() {
        return true;
    }

    @Override
    protected void release() {
        ServerSocket s = server;
        if (s != null) {
            try {
                s.close();
            } catch (IOException e) {
                LOGGER.log(System.Logger.Level.DEBUG, "Error closing server socket", e);
            }
        }
        connections.forEach(Connection::close);
    }

    /**
     *
     * @return the port accepting connections, -1 if not listening
     */
    public int getLocalPort() {
        ServerSocket s = server;
        return s == null ? -1 : s.getLocalPort();
    }

    /**
     *
     * @return the number of open connections
     */
    public int getConnectionCount() {
        return connections.size();
    }

    @Override
    public String toString() {
        return "TcpTransport[" + port + "]";
    }

    /**
     * One connection with a peer : frames are an int length followed by the
     * encoded message
     */
    private final class Connection {

        private final Socket socket;
        private final DataOutputStream out;
        private volatile boolean open = true;

        Connection(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        }

        boolean isOpen() {
            return open;
        }

        void readLoop() {
            try (DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {
                while (open) {
                    int length = in.readInt();
                    if (length < 0 || length > MAX_FRAME_SIZE) {
                        throw new IOException("Invalid frame size " + length);
                    }
                    byte[] data = in.readNBytes(length);
                    if (data.length < length) {
                        throw new EOFException();
                    }
                    received(data, 0, length);
                }
            } catch (IOException e) {
                if (open && !isClosed() && !(e instanceof EOFException)) {
                    LOGGER.log(System.Logger.Level.DEBUG, () -> "Connection with " + socket.getRemoteSocketAddress() + " lost : " + e);
                }
            } finally {
                close();
            }
        }

        synchronized void write(byte[] data) {
            if (!open) {
                return;
            }
            try {
                out.writeInt(data.length);
                out.write(data);
                out.flush();
            } catch (IOException e) {
                LOGGER.log(System.Logger.Level.DEBUG, () -> "Can't write to " + socket.getRemoteSocketAddress() + " : " + e);
                close();
            }
        }

        void close() {
            open = false;
            connections.remove(this);
            try {
                socket.close();
            } catch (IOException e) {
                // already closed
            }
        }
    }
}
