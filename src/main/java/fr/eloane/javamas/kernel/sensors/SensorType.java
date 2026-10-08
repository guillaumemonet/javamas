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

/**
 * Kind of sensor, typed version of the {@link Sensor} constants
 *
 * @author Guillaume Monet
 */
public enum SensorType {
    SPEED(Sensor.SPEED),
    PRESSURE(Sensor.PRESSURE),
    THERMAL(Sensor.THERMAL),
    BRIGHTNESS(Sensor.BRIGHTNESS),
    CONTACT(Sensor.CONTACT),
    OTHER(0);

    private final int code;

    SensorType(int code) {
        this.code = code;
    }

    /**
     *
     * @return the matching {@link Sensor} constant
     */
    public int getCode() {
        return code;
    }

    /**
     *
     * @param code a {@link Sensor} constant
     * @return the matching type or OTHER
     */
    public static SensorType fromCode(int code) {
        for (SensorType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        return OTHER;
    }
}
