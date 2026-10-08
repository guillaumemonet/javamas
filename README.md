# JavaMAS

## Java Multi-Agents System

JavaMAS was written in 2003 for a research paper at the LESTER laboratory, and
modernized in 2026 (Java 21, Gradle, new API).

The term agent that is most often used is undoubtedly the most difficult to define.
In the computer world, the concept of agent is still badly determined.

There are two main types of software agents: **cognitive agents** and **reactive agents**.

* A cognitive agent is described as highly intelligent: it can perform complex operations and process various information independently.
* Reactive agents are, on the contrary, "specialized agents": they can not process information that is too complex, they are already preprogrammed to perform a task or process information.

All characteristics of an agent can be described as follows:

* **Communication**: allows agents to communicate with each other and send each other information.
* **Perception**: agents perceive their environment, this can be modeled by various sensors (sensitive, thermal, electroluminescent).
* **Action**: Each agent acts with his environment. These actions can represent jacks.
* **Knowledge**: it is the "memory" of the agents, it is it that allows the agent to make decisions.
* **Decision**: allows agents to determine what actions to take or not, depending on their perceptions and knowledge.

Thanks to this organization, the agent can make decisions independently.
All the interactions between the different characteristics form what we can call "intelligence of the agents".

* **Learning** allows to increase the level of knowledge and thus increase autonomy compared to other agents. Learning is due to the interoperability of agents with their environment.
* **The cooperation** of the agents between them allows a bigger and faster learning.
* **The autonomy** of the agents represents the idea that they can work without direct intervention and within the limits defined by the user.

An agent is a reactive program, in the sense that it modifies its internal state according to its environment.
It is also endowed with a pro-activity, that is to say that it is able, by its own initiative to carry out actions to reach a goal.
In addition, an agent is characterized by its ability to communicate with other agents or human operators which gives it a social character.

## How the concepts map to the API

| Concept       | API |
|---------------|-----|
| Agent         | `Agent` : runs in its own thread (platform or virtual) |
| Communication | `send(Message)`, `receive()`, `receive(Duration)` ; `ACLMessage` with FIPA `Performative`s |
| Organization  | `getOrganization()` : communities, groups and roles ; messages sent `to(Target)` |
| Perception    | `Sensor` added with `addSensor`, handled in `handleSensor` |
| Knowledge     | `knowledge.Database` (persistent key/value store), `knowledge.MessageHistory` |
| Observation   | `Probe` added with `addProbe`, values published with `publish(name, value)` |
| Distribution  | `Node` with UDP, multicast, TCP (TLS) or in-memory `Transport`s |

## Life cycle

An agent goes through these states (`AgentState`), observable with a probe :

* **CREATED** the agent exists and can already receive messages.
* **ACTIVATING** `init()` then `activate()` : the agent joins its organization, adds its sensors.
* **LIVING** `live()`, the most important period : messages are sent and processed, usually in a
  `while (nextStep())` or `receive()` loop.
* **ENDING** `end()`, the agent can still send messages.
* **DEAD** the agent is killed : unregistered from its node, sensors and probes released.

Only `live()` must be implemented. The life cycle can be controlled from outside with
`setDelay`, `pause`, `resume` and `stop` ; `stop()` also wakes up an agent waiting for a message.

## Example

```java
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
```

More examples in [src/examples](src/examples/java/fr/eloane/javamas/examples), run them with
`./gradlew runExample -Pexample=<package.Class>` :

| Example | Shows |
|---------|-------|
| `simple.HelloWorld` | messages to a community |
| `priority.PriorityDemo` | message priorities |
| `scheduler.SchedulerDemo` | delay, pause, resume, stop |
| `sensors.ThermostatDemo` | a reactive agent with a sensor |
| `probes.ProbeDemo` | observing the life cycle and published values |
| `organization.HiveDemo` | roles and FIPA ACL request / agree / inform |
| `network.TcpPingPong` | two nodes connected with TCP |
| `network.MulticastChat` | chat on the local network (`--args=<name>`) |

## Nodes and transports

Agents live in a `Node` (the default node of the JVM unless another one is given to the constructor).
A node delivers each message once to its agents and exchanges messages with other nodes through
transports :

```java
Node node = Node.getDefault();
node.addTransport(new MulticastTransport(new InetSocketAddress("239.255.80.84", 7889)));
node.addTransport(new TcpTransport(7890, List.of(new InetSocketAddress("other-host", 7890))));
```

* A message received from a transport is forwarded to the other transports (and to the other peers
  of a TCP transport) while its time to live allows it (`Message.ttl`, 4 by default).
* Expired messages (`expiresAfter`, `expiresAt`) are never delivered.
* `TcpTransport` accepts `SSLServerSocketFactory` / `SSLSocketFactory` for TLS.
* `InMemoryTransport` connects nodes of the same JVM, for tests and simulations.

Messages are encoded with a `MessageCodec`. The default `JavaSerializationCodec` only accepts
JavaMAS classes and basic JDK types : the classes used as content must be allowed explicitly.

```java
JavaSerializationCodec codec = new JavaSerializationCodec().allowPackage("com.example.content");
node.addTransport(new TcpTransport(7890, peers, ServerSocketFactory.getDefault(), SocketFactory.getDefault(), codec));
```

## Logging

JavaMAS logs through `System.Logger` (java.util.logging by default, or SLF4J / Log4j when one of their
`System.LoggerFinder` bridges is on the classpath).

## Build

Requires **Java 21** (Gradle downloads a JDK 21 toolchain if needed).

```
./gradlew build                 # compile, test, build the jars in build/libs
./gradlew runExample -Pexample=simple.HelloWorld
./gradlew publishToMavenLocal
```

## License

MIT
