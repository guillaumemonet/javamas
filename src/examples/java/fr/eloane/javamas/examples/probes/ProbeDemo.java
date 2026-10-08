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
package fr.eloane.javamas.examples.probes;

import fr.eloane.javamas.kernel.Agent;
import fr.eloane.javamas.kernel.AgentState;
import fr.eloane.javamas.kernel.probes.Probe;
import fr.eloane.javamas.kernel.probes.ProbeValue;
import java.time.Duration;

/**
 * Observe an agent from outside : its life cycle and the values it publishes
 */
public class ProbeDemo {

    static class Worker extends Agent {

        @Override
        protected void live() {
            for (int done = 1; done <= 5 && nextStep(); done++) {
                publish("progress", done * 20);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Worker worker = new Worker();
        worker.setDelay(Duration.ofMillis(300));
        worker.addProbe(Probe.of(ProbeValue.STATE, AgentState.class, state -> System.out.println("state: " + state)));
        worker.addProbe(Probe.of("progress", Integer.class, percent -> System.out.println("progress: " + percent + "%")));
        worker.start().join();
    }
}
