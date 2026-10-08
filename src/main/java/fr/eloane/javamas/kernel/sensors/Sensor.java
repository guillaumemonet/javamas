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
import java.io.Serializable;
import java.util.Observable;

/**
 * Perception of the environment : notifies its observers each time its value
 * changes
 *
 * @param <T> type of the value
 * @author Guillaume Monet
 */
@SuppressWarnings("deprecation")
public class Sensor<T> extends Observable implements Serializable {

    public static final int SPEED = 1;
    public static final int PRESSURE = 2;
    public static final int THERMAL = 3;
    public static final int BRIGHTNESS = 4;
    public static final int CONTACT = 5;

    @Serial
    private static final long serialVersionUID = -5754532731305532805L;

    private final int type;
    private volatile T value;

    /**
     *
     * @param type one of the type constants, see also {@link SensorType}
     */
    public Sensor(int type) {
        this.type = type;
    }

    /**
     *
     * @param type
     */
    public Sensor(SensorType type) {
        this(type.getCode());
    }

    /**
     *
     * @return the type constant
     */
    public int getType() {
        return type;
    }

    /**
     *
     * @return the type, {@link SensorType#OTHER} for custom types
     */
    public SensorType getSensorType() {
        return SensorType.fromCode(type);
    }

    /**
     *
     * @return the current value
     */
    public T getValue() {
        return value;
    }

    /**
     * Change the value and notify the observers
     *
     * @param value
     */
    public void setValue(T value) {
        this.value = value;
        this.setChanged();
        this.notifyObservers();
    }

    @Override
    public String toString() {
        return String.valueOf(this.value);
    }
}
