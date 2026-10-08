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
package fr.eloane.javamas.examples.organization;

import fr.eloane.javamas.kernel.Agent;
import fr.eloane.javamas.kernel.messages.ACLMessage;
import fr.eloane.javamas.kernel.messages.Message;
import fr.eloane.javamas.kernel.messages.Performative;
import fr.eloane.javamas.kernel.organization.Target;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent Group Role : the queen asks the foragers of the hive to collect
 * nectar, each forager agrees and reports its harvest.
 */
public class HiveDemo {

    static final String HIVE = "hive";
    static final String WORKERS = "workers";
    static final String FORAGER = "forager";

    static class Queen extends Agent {

        Queen() {
            super("queen");
        }

        @Override
        protected void activate() {
            getOrganization().joinGroup(HIVE, "royalty");
        }

        @Override
        protected void live() {
            send(new ACLMessage(Performative.REQUEST, "collect nectar").to(Target.role(HIVE, WORKERS, FORAGER)));
            int total = 0;
            Message<?> message;
            while ((message = receive(Duration.ofSeconds(1))) != null) {
                if (message instanceof ACLMessage acl && acl.getPerformative() == Performative.INFORM) {
                    total += Integer.parseInt(acl.getContent());
                    println(acl.getHeader("bee") + " brought " + acl.getContent() + " mg");
                }
            }
            println("total harvest : " + total + " mg");
        }
    }

    static class Bee extends Agent {

        private final String role;

        Bee(String name, String role) {
            super(name);
            this.role = role;
        }

        @Override
        protected void activate() {
            getOrganization().addRole(HIVE, WORKERS, role);
        }

        @Override
        protected void live() {
            if (receive(Duration.ofSeconds(1)) instanceof ACLMessage request && request.getPerformative() == Performative.REQUEST) {
                send(request.reply(Performative.AGREE, "on my way"));
                int harvest = 20 + (int) (Math.random() * 30);
                send(request.reply(Performative.INFORM, Integer.toString(harvest)).header("bee", getName()));
            } else {
                println("nothing to do, I'm a " + role);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            threads.add(new Bee("forager-" + i, FORAGER).start());
        }
        threads.add(new Bee("nurse", "nurse").start());
        Thread.sleep(100);
        threads.add(new Queen().start());
        for (Thread t : threads) {
            t.join();
        }
    }
}
