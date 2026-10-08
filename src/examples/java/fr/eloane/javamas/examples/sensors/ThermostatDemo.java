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
package fr.eloane.javamas.examples.sensors;

import fr.eloane.javamas.kernel.Agent;
import fr.eloane.javamas.kernel.sensors.Sensor;
import fr.eloane.javamas.kernel.sensors.SensorType;
import java.time.Duration;

/**
 * A reactive agent : a thermostat perceiving the temperature with a sensor
 */
public class ThermostatDemo {

    static class Thermostat extends Agent {

        private final Sensor<Double> thermometer;
        private Boolean heating = null;

        Thermostat(Sensor<Double> thermometer) {
            this.thermometer = thermometer;
        }

        @Override
        protected void activate() {
            addSensor(thermometer);
        }

        @Override
        protected void handleSensor(Sensor<?> sensor) {
            double temperature = (Double) sensor.getValue();
            boolean heat = temperature < 19;
            if (heating == null || heat != heating) {
                heating = heat;
                println(String.format("%.1f C, heating %s", temperature, heat ? "on" : "off"));
            }
        }

        @Override
        protected void live() {
            while (nextStep()) {
                // reacts in handleSensor
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Sensor<Double> thermometer = new Sensor<>(SensorType.THERMAL, "living room");
        Thermostat thermostat = new Thermostat(thermometer);
        thermostat.setDelay(Duration.ofMillis(100));
        Thread thread = thermostat.start();
        Thread.sleep(100);
        for (int i = 0; i < 40; i++) {
            thermometer.setValue(19 + 2 * Math.sin(i / 3.0));
            Thread.sleep(50);
        }
        thermostat.stop();
        thread.join();
    }
}
