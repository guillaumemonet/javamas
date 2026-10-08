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
package fr.eloane.javamas.kernel.knowledge;

import fr.eloane.javamas.kernel.messages.Message;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;

/**
 * Knowledge of an agent : the last messages it received or sent
 *
 * @author Guillaume Monet
 */
public final class MessageHistory {

    private final int capacity;
    private final Deque<Message<?>> messages = new ArrayDeque<>();

    /**
     *
     * @param capacity number of messages kept, the oldest are forgotten
     */
    public MessageHistory(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    /**
     *
     * @param message message to remember (a copy is stored)
     */
    public synchronized void record(Message<?> message) {
        messages.addLast(message.copy());
        if (messages.size() > capacity) {
            messages.removeFirst();
        }
    }

    /**
     *
     * @param filter
     * @return the messages matching the filter, oldest first
     */
    public synchronized List<Message<?>> find(Predicate<? super Message<?>> filter) {
        return messages.stream().filter(filter).toList();
    }

    /**
     *
     * @param conversationId
     * @return the messages of the conversation, oldest first
     */
    public List<Message<?>> conversation(String conversationId) {
        return find(m -> m.getConversationId().equals(conversationId));
    }

    public synchronized int size() {
        return messages.size();
    }

    public synchronized void clear() {
        messages.clear();
    }
}
