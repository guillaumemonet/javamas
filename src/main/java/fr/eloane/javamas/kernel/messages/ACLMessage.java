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

import java.io.Serial;
import java.util.Objects;

/**
 * FIPA ACL message : a performative and a textual content.<br />
 * Original Work by the MaDKit Team.
 *
 * @author Guillaume Monet
 */
public final class ACLMessage extends Message<String> {

    @Serial
    private static final long serialVersionUID = 2L;

    private final Performative performative;

    /**
     *
     * @param performative
     * @param content
     */
    public ACLMessage(Performative performative, String content) {
        super(content);
        this.performative = Objects.requireNonNull(performative);
    }

    private ACLMessage(Envelope envelope, Performative performative, String content) {
        super(envelope, content);
        this.performative = Objects.requireNonNull(performative);
    }

    /**
     * Rebuild a message from its envelope, e.g. received from the network
     *
     * @param envelope
     * @param performative
     * @param content
     * @return the message
     */
    public static ACLMessage restore(Envelope envelope, Performative performative, String content) {
        return new ACLMessage(envelope, performative, content);
    }

    @Override
    public ACLMessage copy() {
        return (ACLMessage) super.copy();
    }

    /**
     * Reply with a performative, same conversation
     *
     * @param performative
     * @param content
     * @return the reply
     */
    public ACLMessage reply(Performative performative, String content) {
        ACLMessage reply = new ACLMessage(performative, content);
        reply.conversation(this.getConversationId());
        reply.header(IN_REPLY_TO, this.getId());
        if (this.getSender() != null) {
            reply.to(this.getSender());
        }
        return reply;
    }

    public Performative getPerformative() {
        return performative;
    }

    @Override
    public String toString() {
        return "(" + performative.fipaName() + " :sender " + getSender() + " :content \"" + getContent() + "\")";
    }
}
