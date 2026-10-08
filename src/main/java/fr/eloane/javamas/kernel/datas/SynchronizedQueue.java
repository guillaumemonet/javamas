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
package fr.eloane.javamas.kernel.datas;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * Store and to remove T No null elements allowed in this queue
 *
 * @author Guillaume Monet
 * @param <T>
 */
public class SynchronizedQueue<T> extends LinkedList<T> implements Serializable, Cloneable {

    /**
     *
     */
    public static final long serialVersionUID = 1L;

    /**
     *
     */
    public SynchronizedQueue() {
        super();
    }

    /**
     *
     * @param queue
     */
    public SynchronizedQueue(SynchronizedQueue<T> queue) {
        super(queue);
    }

    /**
     * push object in the list of data
     *
     * @param obj the object to store in the queue
     * @see #pop()
     * @see #pop(int)
     */
    @Override
    public synchronized void push(T obj) {
        super.add(obj);
        this.notify();
    }

    /**
     * return the fist of all object in the queue and remove it from the queue
     *
     * @return object
     * @see #pop(int)
     */
    @Override
    public synchronized T pop() {
        return this.pop(0);
    }

    /**
     *
     * @param wait
     * @return T or null if stack empty
     */
    public synchronized T pop(int wait) {
        try {
            long deadline = System.currentTimeMillis() + wait;
            while (this.isEmpty()) {
                if (wait == 0) {
                    this.wait();
                } else {
                    long remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0) {
                        break;
                    }
                    this.wait(remaining);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return this.poll();
    }

    /**
     *
     * @return
     */
    public synchronized T getNextElement() {
        return this.peek();
    }

    /**
     * remove all elements from the queue
     */
    public synchronized void flush() {
        this.clear();
    }

    /**
     * clone the current Stack
     *
     * @return new Queue
     */
    @Override
    public SynchronizedQueue<T> clone() {
        return new SynchronizedQueue<>(this);
    }
}
