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

import fr.eloane.javamas.kernel.organization.Target;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Envelope of a message : everything but the content.<br />
 * Used by the codecs to rebuild a message received from the network, see
 * {@link Message#restore(Envelope, Object)}.
 *
 * @param id unique id of the message
 * @param created creation time
 * @param conversationId
 * @param sender id of the sender agent, null if not sent yet
 * @param receivers ids of the receiver agents
 * @param targets organization targets
 * @param priority
 * @param expiresAt expiration time, null for no expiration
 * @param ttl number of relays between transports still allowed
 * @param headers free headers
 * @author Guillaume Monet
 */
public record Envelope(String id, Instant created, String conversationId, String sender,
        List<String> receivers, List<Target> targets, Priority priority, Instant expiresAt, int ttl,
        Map<String, Serializable> headers) {

    public Envelope {
        Objects.requireNonNull(id);
        Objects.requireNonNull(created);
        Objects.requireNonNull(conversationId);
        Objects.requireNonNull(priority);
        receivers = List.copyOf(receivers);
        targets = List.copyOf(targets);
        headers = Map.copyOf(headers);
    }
}
