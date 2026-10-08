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

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Perception of the environment : notifies its listeners each time its value
 * changes.<br />
 * A sensor added to an agent calls the agent's handleSensor method.
 *
 * @param <T> type of the value
 * @author Guillaume Monet
 */
public class Sensor<T> {

    private final List<SensorListener> listeners = new CopyOnWriteArrayList<>();
    private final SensorType type;
    private final String name;
    private volatile T value;

    /**
     *
     * @param type
     */
    public Sensor(SensorType type) {
        this(type, type.name().toLowerCase(Locale.ROOT));
    }

    /**
     *
     * @param type
     * @param name name of the sensor, to tell apart sensors of the same type
     */
    public Sensor(SensorType type, String name) {
        this.type = Objects.requireNonNull(type);
        this.name = Objects.requireNonNull(name);
    }

    public SensorType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    /**
     *
     * @return the current value, null until the first value is set
     */
    public T getValue() {
        return value;
    }

    /**
     * Change the value and notify the listeners
     *
     * @param value
     */
    public void setValue(T value) {
        this.value = value;
        listeners.forEach(l -> l.sensorChanged(this));
    }

    public void addListener(SensorListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SensorListener listener) {
        listeners.remove(listener);
    }

    @Override
    public String toString() {
        return name + "=" + value;
    }
}
