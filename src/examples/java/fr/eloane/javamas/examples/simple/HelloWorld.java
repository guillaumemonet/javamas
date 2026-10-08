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
package fr.eloane.javamas.examples.simple;

import fr.eloane.javamas.kernel.Agent;
import fr.eloane.javamas.kernel.messages.Message;
import fr.eloane.javamas.kernel.organization.Target;
import java.time.Duration;

/**
 * Two agents of the same community : the sender says hello to the community,
 * the listener prints what it receives.
 */
public class HelloWorld {

    static class Listener extends Agent {

        @Override
        protected void activate() {
            getOrganization().joinCommunity("WORLD");
        }

        @Override
        protected void live() {
            Message<?> message = receive(Duration.ofSeconds(5));
            println(message == null ? "nobody said anything" : "received " + message.getContent());
        }
    }

    static class Sender extends Agent {

        @Override
        protected void live() {
            send(new Message<>("Hello World").to(Target.community("WORLD")));
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Thread listener = new Listener().start();
        Thread.sleep(100);
        new Sender().start();
        listener.join();
    }
}
