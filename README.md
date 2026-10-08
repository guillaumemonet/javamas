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
| Perception    | `Sensor` added with `addSensor`, handled in `handleSensor` ; sensors bound to an `Environment` property |
| Action        | changing the properties of the `Environment` |
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
| `environment.ThermostatDemo` | perception, decision and action in a shared environment |
| `probes.ProbeDemo` | observing the life cycle and published values |
| `organization.HiveDemo` | roles and FIPA ACL request / agree / inform |
| `network.TcpPingPong` | two nodes connected with TCP |
| `network.MulticastChat` | chat on the local network (`--args=<name>`) |

## Environment

An `Environment` is shared by agents : a set of named properties. Agents perceive it with sensors
bound to properties and act on it by changing properties ; dynamics rules make it evolve at each
step, run manually (`step()`, for reproducible simulations) or periodically (`start(period)`).

```java
Environment room = new Environment("room");
room.set("temperature", 21.0);
room.addDynamics(env -> env.update("temperature", Double.class,
        t -> t - 0.15 + (env.get("heater", Boolean.class, false) ? 0.4 : 0)));

// in the agent
addSensor(room.sensor("temperature", Double.class, SensorType.THERMAL)); // perception
room.set("heater", true);                                                // action

room.start(Duration.ofMillis(50));
```

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

### JSON codec

`JsonCodec` exchanges messages with nodes written in other languages (format `javamas/1`) :

```json
{
  "format": "javamas/1",
  "id": "M:1", "created": "2026-10-08T12:00:00Z", "conversation": "C:1",
  "sender": "A:0", "receivers": ["A:1"],
  "targets": [{"community": "lab", "group": "team", "role": null}],
  "priority": "normal", "expiresAt": null, "ttl": 4,
  "headers": {"language": "fr"},
  "performative": "inform",
  "contentType": "position",
  "content": {"x": 1, "y": 2}
}
```

Only `format`, `id`, `created`, `conversation` and `priority` are required ; `performative` makes it an
`ACLMessage`. Strings, numbers, booleans, lists and maps are decoded as such ; other content types
must be registered with a name, no class is ever chosen from the received data :

```java
JsonCodec codec = new JsonCodec().registerType("position", Position.class);
```

## Logging

JavaMAS logs through `System.Logger` (java.util.logging by default, or SLF4J / Log4j when one of their
`System.LoggerFinder` bridges is on the classpath).

## Build

Requires **Java 21** (Gradle downloads a JDK 21 toolchain if needed).

```
./gradlew build                 # compile, test, build the jars in build/libs
./gradlew runExample -Pexample=simple.HelloWorld   # quote it in PowerShell : '-Pexample=simple.HelloWorld'
./gradlew publishToMavenLocal
```

## License

MIT
