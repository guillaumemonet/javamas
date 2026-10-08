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
package fr.eloane.javamas.examples.scheduler;

import fr.eloane.javamas.kernel.Agent;
import java.time.Duration;

/**
 * Control the life cycle of an agent from outside : delay, pause, resume,
 * stop
 */
public class SchedulerDemo {

    static class Counter extends Agent {

        @Override
        protected void activate() {
            println("hello");
        }

        @Override
        protected void live() {
            int count = 0;
            while (nextStep()) {
                println("step " + count++);
            }
        }

        @Override
        protected void end() {
            println("bye");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Counter counter = new Counter();
        counter.setDelay(Duration.ofMillis(500));
        Thread thread = counter.start();
        Thread.sleep(1600);
        System.out.println("-- pause");
        counter.pause();
        Thread.sleep(1500);
        System.out.println("-- resume");
        counter.resume();
        Thread.sleep(1100);
        System.out.println("-- pause 2 seconds");
        counter.pause(Duration.ofSeconds(2));
        Thread.sleep(3000);
        counter.stop();
        thread.join();
    }
}
