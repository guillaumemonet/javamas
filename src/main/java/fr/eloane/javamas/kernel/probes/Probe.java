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

import java.io.Serial;
import java.io.Serializable;
import java.util.Observable;
import java.util.Observer;
import java.util.function.Consumer;

/**
 * Observe the values published by an agent : the values of the
 * {@link #handleProbe(Object) handled type} are received, the others are
 * ignored.
 *
 * @author Guillaume Monet
 * @param <T> type of the values handled by the probe
 */
@SuppressWarnings("deprecation")
public abstract class Probe<T> implements Serializable, Observer {

    @Serial
    private static final long serialVersionUID = 7523662972689640778L;

    /**
     * Create a probe from a lambda
     *
     * @param <T> type of the values handled by the probe
     * @param type class of the values handled by the probe, the others are
     * ignored
     * @param handler called for each value
     * @return the probe
     */
    public static <T> Probe<T> of(Class<T> type, Consumer<? super T> handler) {
        return new Probe<>() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void handleProbe(T value) {
                handler.accept(value);
            }

            @Override
            protected boolean accept(Object value) {
                return type.isInstance(value);
            }
        };
    }

    /**
     *
     * @param o
     * @param arg
     */
    @Override
    @SuppressWarnings("unchecked")
    public final void update(Observable o, Object arg) {
        if (!accept(arg)) {
            return;
        }
        try {
            this.handleProbe((T) arg);
        } catch (ClassCastException e) {
            // The value is not of the handled type
        }
    }

    /**
     *
     * @param value value published by the agent
     * @return if the value must be handled
     */
    protected boolean accept(Object value) {
        return true;
    }

    /**
     *
     * @param value
     */
    protected abstract void handleProbe(T value);
}
