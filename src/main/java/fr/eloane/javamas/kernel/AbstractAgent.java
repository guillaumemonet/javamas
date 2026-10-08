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

import fr.eloane.javamas.kernel.sensors.Sensor;
import java.io.Serial;
import java.io.Serializable;
import java.util.Observable;
import java.util.Observer;

/**
 * Life cycle of an agent : init, activate, live, end, kill.<br />
 * The agent is observable by its probes and observes its sensors.
 *
 * @author Guillaume Monet
 */
@SuppressWarnings("deprecation")
public abstract class AbstractAgent extends Observable implements Serializable, Observer, Runnable {

    @Serial
    private static final long serialVersionUID = -4353825708388962018L;

    private static final System.Logger LOGGER = System.getLogger(AbstractAgent.class.getName());

    protected transient boolean daemon = false;

    /**
     * Initialize of the Agent can be overridden
     */
    protected void init() {
    }

    /**
     * Activation of the agent : join organizations, add sensors...
     */
    protected abstract void activate();

    /**
     * life of the agent
     */
    protected abstract void live();

    /**
     * end of the agent
     */
    protected abstract void end();

    /**
     * Reset the agent if it needs to be reused
     */
    protected void reset() {
    }

    /**
     * Destroy the agent
     */
    protected void kill() {
    }

    /**
     * Start the life cycle in a new platform thread
     */
    public final void start() {
        Thread.ofPlatform().name(threadName()).daemon(daemon).start(this);
    }

    /**
     * Start the life cycle in a new virtual thread.<br />
     * Virtual threads are cheap and allow a huge number of agents, but they do
     * not keep the JVM alive.
     *
     * @return the thread running the agent
     */
    public final Thread startVirtual() {
        return Thread.ofVirtual().name(threadName()).start(this);
    }

    /**
     *
     * @return the name of the thread running the agent
     */
    protected String threadName() {
        return getClass().getSimpleName();
    }

    /**
     * Life of the agent destroy all objects from the agents
     */
    @Override
    public final void run() {
        try {
            this.init();
            this.activate();
            this.live();
            this.end();
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.ERROR, "Agent " + threadName() + " failed", e);
        } finally {
            this.kill();
        }
    }

    /**
     * Method to override if sensors want to be handled.<br />
     * Called each time the value of a sensor added to the agent changes.
     *
     * @param sensor sensor that trigger the event
     */
    protected void handleSensor(Sensor<?> sensor) {
        LOGGER.log(System.Logger.Level.DEBUG, () -> threadName() + " sensor " + sensor.getType() + " : " + sensor.getValue());
    }

    /**
     * Handle update for observer pattern
     *
     * @param o observable that trigger the update event
     * @param arg
     * @throws ClassCastException if the observable is not a sensor
     */
    @Override
    public final void update(Observable o, Object arg) throws ClassCastException {
        if (o instanceof Sensor<?> sensor) {
            handleSensor(sensor);
        } else {
            throw new ClassCastException("Can't cast " + o.getClass() + " to " + Sensor.class);
        }
    }

}
