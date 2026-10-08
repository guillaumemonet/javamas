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

import fr.eloane.javamas.kernel.exception.UnknownTransport;
import java.util.HashMap;
import java.util.Map;

/**
 * Create transports
 *
 * @author Guillaume Monet
 */
public abstract class TransportFactory {

    public static final int TRANSPORT_TCP = 1;
    public static final int TRANSPORT_UDP = 2;
    public static final int TRANSPORT_MULTICAST = 3;

    /**
     * Available transports
     */
    public enum TransportType {
        TCP, UDP, MULTICAST
    }

    private TransportFactory() {
    }

    /**
     *
     * @param transport_type one of the TRANSPORT_ constants
     * @param parameters configuration of the transport, see the constants of
     * each transport
     * @return a new transport, not started
     * @throws UnknownTransport if the type is unknown
     */
    public static Transport getTransport(int transport_type, HashMap<String, String> parameters) throws UnknownTransport {
        return switch (transport_type) {
            case TRANSPORT_TCP ->
                getTransport(TransportType.TCP, parameters);
            case TRANSPORT_UDP ->
                getTransport(TransportType.UDP, parameters);
            case TRANSPORT_MULTICAST ->
                getTransport(TransportType.MULTICAST, parameters);
            default ->
                throw new UnknownTransport();
        };
    }

    /**
     *
     * @param type the kind of transport
     * @param parameters configuration of the transport, see the constants of
     * each transport
     * @return a new transport, not started
     */
    public static Transport getTransport(TransportType type, Map<String, String> parameters) {
        HashMap<String, String> params = new HashMap<>(parameters);
        return switch (type) {
            case TCP ->
                new TransportTCP(params);
            case UDP ->
                new TransportUDP(params);
            case MULTICAST ->
                new TransportMulticast(params);
        };
    }

}
