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
package fr.eloane.javamas.kernel.probes;

import fr.eloane.javamas.kernel.Address;
import java.io.Serializable;
import java.time.Instant;

/**
 * Value published by an agent to its probes
 *
 * @param agent the agent publishing the value
 * @param name name of the value, e.g. {@link #STATE}
 * @param value the value
 * @param time when the value was published
 * @author Guillaume Monet
 */
public record ProbeValue(Address agent, String name, Object value, Instant time) implements Serializable {

    /**
     * Published by every agent when its life cycle state changes, the value is
     * an {@link fr.eloane.javamas.kernel.AgentState}
     */
    public static final String STATE = "state";

    /**
     * Published by every agent when the size of its mailbox changes, the value
     * is an Integer
     */
    public static final String MAILBOX_SIZE = "mailbox.size";

    @Override
    public String toString() {
        return agent + " " + name + "=" + value;
    }
}
