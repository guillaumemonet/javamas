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
package fr.eloane.javamas.kernel;

import fr.eloane.javamas.kernel.messages.Message;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * Knowledge of the agent : Messages history
 *
 * @author Guillaume Monet
 */
public class AgentHistory extends ArrayList<Message<?>> {

    @Serial
    private static final long serialVersionUID = 968563573046293L;

    /**
     *
     * @param conditions fields values the messages must have
     * @return copies of the messages matching all the conditions
     */
    @SuppressWarnings("rawtypes")
    public synchronized ArrayList<Message> getMessages(HashMap<String, String> conditions) {
        ArrayList<Message> ret = new ArrayList<>();
        for (Message<?> mes : this) {
            if (conditions.entrySet().stream().allMatch(ent -> Objects.equals(mes.get(ent.getKey()), ent.getValue()))) {
                ret.add(mes.clone());
            }
        }
        return ret;
    }

    /**
     *
     * @param mes message to store in the history
     */
    public synchronized void putMessage(Message<?> mes) {
        this.add(mes.clone());
    }
}
