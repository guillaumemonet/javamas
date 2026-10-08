# Javamas

## Java Multi-Agents System


[![Maintainability](https://api.codeclimate.com/v1/badges/782876893a84c694b0a1/maintainability)](https://codeclimate.com/github/guillaumemonet/javamas/maintainability)

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


* **Learning** allows to increase the level of knowledge and thus increase autonomy compared to other agents. As we have seen before, learning is due to the interoperability of agents with their environment.

* **The cooperation** of the agents between them allows a bigger and faster learning.

* **The autonomy** of the agents represents the idea that they can work without direct intervention and within the limits defined by the user.

An agent is a reactive program, in the sense that it modifies its internal state according to its environment.
It is also endowed with a pro-activity, that is to say that it is able, by its own initiative to carry out actions to reach a goal.

In addition, an agent is characterized by its ability to communicate with other agents or human operators which gives it a social character.

## Internal State

In order to dissociate the internal state of each agent, it has a five-part arrangement.

* **Activation**, the agent starts, activates its functions and dialog methods.

* **Live** The live cycle is the most important period for the agent. Here all messages are sent and processed.

* **End** The end of life of the agent, it can still process certain information.

* **Kill**, the agent has finished his activity and is permanently destroyed.

* **Reset**, the agent is reset (if it needs to be reused).



## Build

The project is built with Gradle (wrapper included) and requires **Java 21**. Gradle downloads a JDK 21 toolchain if none is installed.

```
./gradlew build        # compile, run tests, build jars in build/libs
./gradlew publishToMavenLocal
```

## How to use it


### Simple example : Agents Dialog one the same node
```java

import fr.eloane.javamas.kernel.Agent;
import fr.eloane.javamas.kernel.messages.Message;

/**
 *
 * @author Guillaume Monet
 */
public class Listener extends Agent<String>{

    @Override
    protected void activate() {
        this.getOrganization().joinCommunity("WORLD");
    }

    @Override
    protected void live() {
        Message mess = this.waitNextMessage();
        System.out.println(mess);
    }

    @Override
    protected void end() {
        
    }
    
}

```

```java
import fr.eloane.javamas.kernel.Agent;
import fr.eloane.javamas.kernel.messages.Message;
import fr.eloane.javamas.kernel.organization.Organization;

/**
 *
 * @author Guillaume Monet
 */
public class Sender extends Agent<String> {
    
    @Override
    protected void activate() {
        this.getOrganization().joinCommunity("WORLD");
    }
    
    @Override
    protected void live() {
        Message mess = new Message("Hello World");
        mess.addOrganization(new Organization("WORLD"));
        this.sendMessage(mess);
    }
    
    @Override
    protected void end() {
        
    }
    
}

```

```java

/**
 *
 * @author Guillaume Monet
 */
public class Launch {
    public static void main(String[] args){
        (new Listener()).start();
        (new Sender()).start();
    }
}

```



### Waiting for message for 5 seconds
```java

import fr.eloane.javamas.kernel.Agent;

public class Waiting extends Agent {

    @Override
    public void activate() {
        this.getOrganization().joinGroup("TEST", "WAITER");
    }

    @Override
    public void live() {
        this.println("WAITING FOR MESSAGE");
        this.waitNextMessage(5000);
        this.println("NO MORE WAITING");
    }

    @Override
    public void end() {
    }

    public static void main(String[] args) {
        (new Waiting()).start();
    }
}

```


### Messages between nodes (network transports)

Messages received through the UDP / Multicast transports are deserialized with a
class allowlist (JavaMAS classes, `String`, boxed primitives, basic collections...).
Classes used as message content must be allowed explicitly before receiving them:

```java
SecureObjectInputStream.allowClass(MyContent.class);
// or
SecureObjectInputStream.allowPackage("com.example.content");
```


### Sensors and probes

```java
// Sensor : the agent's handleSensor(Sensor<?>) is called each time the value changes
Sensor<Integer> thermal = new Sensor<>(SensorType.THERMAL);
agent.getSensorsManager().addSensor(thermal);
thermal.setValue(21);

// Probe : observe the values published by an agent with probe(description, value)
agent.getProbesManager().addProbe(Probe.of(AgentProbeValue.class, value -> System.out.println(value)));
```

Sensor types can also be given as `int` constants (`Sensor.THERMAL`...).

### Transports

```java
Map<String, String> params = Map.of(TransportMulticast.IP, "239.255.80.84", TransportMulticast.PORT, "7889");
Node.getHandle().addTransport(TransportFactory.getTransport(TransportFactory.TransportType.MULTICAST, params));
```

### Threads

`agent.start()` runs the agent in a platform thread (daemon or not, see the constructors).
`agent.startVirtual()` runs it in a virtual thread : thousands of agents can wait for
messages at a low cost, but virtual threads don't keep the JVM alive.

### Logging

JavaMAS logs through `System.Logger` (java.util.logging by default, or SLF4J / Log4j
when one of their `System.LoggerFinder` bridges is on the classpath).
