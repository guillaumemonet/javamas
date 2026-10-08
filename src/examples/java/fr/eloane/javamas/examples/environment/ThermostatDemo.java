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
package fr.eloane.javamas.examples.environment;

import fr.eloane.javamas.kernel.Agent;
import fr.eloane.javamas.kernel.environment.Environment;
import fr.eloane.javamas.kernel.sensors.Sensor;
import fr.eloane.javamas.kernel.sensors.SensorType;
import java.time.Duration;

/**
 * Perception, decision, action : a thermostat agent perceives the temperature
 * of a room with a sensor and acts on the heater. The room cools down by itself
 * and warms up when the heater is on.
 */
public class ThermostatDemo {

    static final String TEMPERATURE = "temperature";
    static final String HEATER = "heater";

    static class Thermostat extends Agent {

        private final Environment room;
        private final double target;

        Thermostat(Environment room, double target) {
            super("thermostat");
            this.room = room;
            this.target = target;
        }

        @Override
        protected void activate() {
            addSensor(room.sensor(TEMPERATURE, Double.class, SensorType.THERMAL));
        }

        @Override
        protected void handleSensor(Sensor<?> sensor) {
            // Decision with hysteresis, action on the environment
            double temperature = (Double) sensor.getValue();
            boolean heating = room.get(HEATER, Boolean.class, false);
            if (!heating && temperature < target - 0.5) {
                room.set(HEATER, true);
                println(String.format("%.1f C : heater on", temperature));
            } else if (heating && temperature > target + 0.5) {
                room.set(HEATER, false);
                println(String.format("%.1f C : heater off", temperature));
            }
        }

        @Override
        protected void live() {
            while (nextStep()) {
                // the thermostat reacts in handleSensor
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        try (Environment room = new Environment("room")) {
            room.set(TEMPERATURE, 21.0);
            room.set(HEATER, false);
            // The room loses heat, the heater brings some
            room.addDynamics(env -> env.update(TEMPERATURE, Double.class,
                    t -> t - 0.15 + (env.get(HEATER, Boolean.class, false) ? 0.4 : 0)));

            Thermostat thermostat = new Thermostat(room, 19);
            Thread thread = thermostat.start();
            room.start(Duration.ofMillis(50));
            Thread.sleep(6000);
            thermostat.stop();
            thread.join();
            System.out.printf("%d steps, final temperature %.1f C%n", room.getSteps(), room.get(TEMPERATURE, Double.class));
        }
    }
}
