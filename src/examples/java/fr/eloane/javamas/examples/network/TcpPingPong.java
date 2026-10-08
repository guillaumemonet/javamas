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
package fr.eloane.javamas.examples.network;

import fr.eloane.javamas.kernel.Agent;
import fr.eloane.javamas.kernel.Node;
import fr.eloane.javamas.kernel.messages.Message;
import fr.eloane.javamas.kernel.organization.Target;
import fr.eloane.javamas.kernel.transport.TcpTransport;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;

/**
 * Two nodes connected with TCP (in the same JVM for the demo, usually on two
 * machines) : an agent on each node plays ping pong.
 */
public class TcpPingPong {

    static class Player extends Agent {

        private final String word;
        private final boolean serve;

        Player(String word, Node node, boolean serve) {
            super(word, node);
            this.word = word;
            this.serve = serve;
        }

        @Override
        protected void activate() {
            getOrganization().addRole("game", "table", word);
        }

        @Override
        protected void live() {
            String other = word.equals("ping") ? "pong" : "ping";
            if (serve) {
                send(new Message<>(word + " 1").to(Target.role("game", "table", other)));
            }
            Message<?> ball;
            while ((ball = receive(Duration.ofSeconds(2))) != null) {
                println("received " + ball.getContent());
                int count = Integer.parseInt(ball.getContent().toString().split(" ")[1]);
                if (count >= 5) {
                    break;
                }
                send(ball.reply(word + " " + (count + 1)));
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        try (Node left = new Node("left"); Node right = new Node("right")) {
            TcpTransport server = new TcpTransport(0, List.of());
            left.addTransport(server);
            right.addTransport(new TcpTransport(-1, List.of(new InetSocketAddress(InetAddress.getLoopbackAddress(), server.getLocalPort()))));

            Thread pong = new Player("pong", right, false).start();
            Thread.sleep(200);
            Thread ping = new Player("ping", left, true).start();
            ping.join();
            pong.join();
        }
    }
}
