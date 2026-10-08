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
import java.util.ArrayList;
import java.util.Observable;

/**
 * Probes of an agent
 *
 * @author Guillaume Monet
 */
@SuppressWarnings({"rawtypes", "deprecation"})
public class ProbesManager extends ArrayList<Probe> implements Serializable {

    @Serial
    private static final long serialVersionUID = -5675257949050206130L;

    private final Observable observable;

    /**
     *
     * @param observable the observed agent
     */
    public ProbesManager(Observable observable) {
        this.observable = observable;
    }

    /**
     * Add probe in the agent
     *
     * @param obs
     */
    public final synchronized void addProbe(Probe obs) {
        this.observable.addObserver(obs);
        this.add(obs);
    }

    /**
     * Remove probe in the agent
     *
     * @param probe
     */
    public final synchronized void removeProbe(Probe probe) {
        this.observable.deleteObserver(probe);
        this.remove(probe);
    }

    /**
     * Remove all probes
     */
    public final synchronized void flushProbes() {
        this.observable.deleteObservers();
        this.clear();
    }

    /**
     * Return all probes
     *
     * @return
     */
    public final ArrayList<Probe> getProbes() {
        return this;
    }
}
