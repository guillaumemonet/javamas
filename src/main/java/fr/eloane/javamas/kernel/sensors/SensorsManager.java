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
package fr.eloane.javamas.kernel.sensors;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Observer;

/**
 * Sensors of an agent
 *
 * @author Guillaume Monet
 */
@SuppressWarnings({"rawtypes", "deprecation"})
public class SensorsManager extends ArrayList<Sensor> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Observer observer;

    /**
     *
     * @param observer notified when the value of a sensor changes
     */
    public SensorsManager(Observer observer) {
        this.observer = observer;
    }

    /**
     * Add a sensor
     *
     * @param sensor
     */
    public final synchronized void addSensor(Sensor<?> sensor) {
        sensor.addObserver(observer);
        this.add(sensor);
    }

    /**
     * Remove a sensor
     *
     * @param sensor
     */
    public final synchronized void removeSensor(Sensor<?> sensor) {
        sensor.deleteObserver(observer);
        this.remove(sensor);
    }

    /**
     * Remove all the sensors
     */
    public final synchronized void flushSensors() {
        this.forEach(sens -> sens.deleteObserver(observer));
        this.clear();
    }
}
