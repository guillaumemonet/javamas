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
import fr.eloane.javamas.kernel.transport.MulticastTransport;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;

/**
 * Chat between the nodes of the local network : run it on several machines
 * (or several terminals) with a different name, type lines to talk.<br />
 * ./gradlew runExample -Pexample=network.MulticastChat --args=alice
 */
public class MulticastChat {

    static class Chatter extends Agent {

        Chatter(String name) {
            super(name);
        }

        @Override
        protected void activate() {
            getOrganization().joinCommunity("chat");
        }

        @Override
        protected void live() {
            Message<?> message;
            while ((message = receive()) != null) {
                System.out.println(message.getHeader("nickname") + "> " + message.getContent());
            }
        }

        void say(String line) {
            send(new Message<>(line).to(Target.community("chat")).header("nickname", getName()));
        }
    }

    public static void main(String[] args) throws IOException {
        String name = args.length > 0 ? args[0] : System.getProperty("user.name");
        Node.getDefault().addTransport(new MulticastTransport(new InetSocketAddress("239.255.80.84", 7889)));
        Chatter chatter = new Chatter(name);
        chatter.start(Thread.ofPlatform().daemon());
        System.out.println("Connected as " + name + ", type a message (empty line to quit)");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            chatter.say(line);
        }
        Node.getDefault().close();
    }
}
