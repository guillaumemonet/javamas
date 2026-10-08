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
package fr.eloane.javamas.examples.priority;

import fr.eloane.javamas.kernel.Agent;
import fr.eloane.javamas.kernel.messages.Message;
import fr.eloane.javamas.kernel.messages.Priority;
import java.time.Duration;

/**
 * Messages are read by priority, then in arrival order
 */
public class PriorityDemo {

    static class Reader extends Agent {

        @Override
        protected void live() {
            // Let the messages arrive before reading them
            pause(Duration.ofMillis(500));
            nextStep();
            Message<?> message;
            while ((message = receive(Duration.ofMillis(100))) != null) {
                println(message.getPriority() + " " + message.getContent());
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Reader reader = new Reader();
        Thread thread = reader.start();
        Agent writer = new Agent("writer") {
            @Override
            protected void live() {
                send(new Message<>("low").priority(Priority.LOW).to(reader.getAddress()));
                send(new Message<>("first normal").to(reader.getAddress()));
                send(new Message<>("high").priority(Priority.HIGH).to(reader.getAddress()));
                send(new Message<>("second normal").to(reader.getAddress()));
                send(new Message<>("extreme").priority(Priority.EXTREME).to(reader.getAddress()));
            }
        };
        writer.start();
        thread.join();
    }
}
