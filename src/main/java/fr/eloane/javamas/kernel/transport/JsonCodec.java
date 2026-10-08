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
package fr.eloane.javamas.kernel.transport;

import fr.eloane.javamas.kernel.messages.ACLMessage;
import fr.eloane.javamas.kernel.messages.Envelope;
import fr.eloane.javamas.kernel.messages.Message;
import fr.eloane.javamas.kernel.messages.Performative;
import fr.eloane.javamas.kernel.messages.Priority;
import fr.eloane.javamas.kernel.organization.Target;
import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * JSON codec, to exchange messages with nodes written in other languages.<br />
 * The content and the header values are JSON values : strings, numbers,
 * booleans, lists and maps are decoded as such. Other content types must be
 * registered with a name ({@link #registerType(String, Class)}) : the name is
 * sent in the "contentType" field and the content is mapped to and from the
 * class by Jackson. Unknown content types are rejected, no class is ever
 * chosen from the received data.
 *
 * <pre>
 * {
 *   "format": "javamas/1",
 *   "id": "M:...", "created": "2026-10-08T12:00:00Z", "conversation": "C:...",
 *   "sender": "A:...", "receivers": ["A:..."],
 *   "targets": [{"community": "lab", "group": "team", "role": null}],
 *   "priority": "normal", "expiresAt": null, "ttl": 4,
 *   "headers": {"language": "fr"},
 *   "performative": "inform",          (ACL messages only)
 *   "contentType": "position",         (registered types only)
 *   "content": ...
 * }
 * </pre>
 *
 * @author Guillaume Monet
 */
public final class JsonCodec implements MessageCodec {

    /**
     * Version of the JSON format
     */
    public static final String FORMAT = "javamas/1";

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final Map<String, Class<?>> typesByName = new ConcurrentHashMap<>();
    private final Map<Class<?>, String> namesByType = new ConcurrentHashMap<>();

    /**
     * Register a content type : its instances are sent with this name and
     * mapped by Jackson (records, beans...)
     *
     * @param name name of the type in the JSON messages
     * @param type the class
     * @return this
     */
    public JsonCodec registerType(String name, Class<?> type) {
        typesByName.put(name, type);
        namesByType.put(type, name);
        return this;
    }

    @Override
    public byte[] encode(Message<?> message) throws IOException {
        Envelope envelope = message.getEnvelope();
        ObjectNode root = mapper.createObjectNode();
        root.put("format", FORMAT);
        root.put("id", envelope.id());
        root.put("created", envelope.created().toString());
        root.put("conversation", envelope.conversationId());
        root.put("sender", envelope.sender());
        ArrayNode receivers = root.putArray("receivers");
        envelope.receivers().forEach(receivers::add);
        ArrayNode targets = root.putArray("targets");
        for (Target t : envelope.targets()) {
            targets.addObject().put("community", t.community()).put("group", t.group()).put("role", t.role());
        }
        root.put("priority", envelope.priority().name().toLowerCase(Locale.ROOT));
        root.put("expiresAt", envelope.expiresAt() == null ? null : envelope.expiresAt().toString());
        root.put("ttl", envelope.ttl());
        try {
            ObjectNode headers = root.putObject("headers");
            envelope.headers().forEach((name, value) -> headers.set(name, mapper.valueToTree(value)));
            if (message instanceof ACLMessage acl) {
                root.put("performative", acl.getPerformative().fipaName());
            }
            Object content = message.getContent();
            if (content != null) {
                if (!isJsonValue(content)) {
                    String name = namesByType.get(content.getClass());
                    if (name == null) {
                        throw new IOException("Content type not registered : " + content.getClass().getName());
                    }
                    root.put("contentType", name);
                }
                root.set("content", mapper.valueToTree(content));
            }
            return mapper.writeValueAsBytes(root);
        } catch (JacksonException e) {
            throw new IOException("Can't encode message " + envelope.id(), e);
        }
    }

    @Override
    public Message<?> decode(byte[] data, int offset, int length) throws IOException {
        try {
            JsonNode root = mapper.readTree(data, offset, length);
            if (root == null || !root.isObject() || !FORMAT.equals(root.path("format").asString(null))) {
                throw new IOException("Not a " + FORMAT + " message");
            }
            Envelope envelope = new Envelope(
                    required(root, "id"),
                    Instant.parse(required(root, "created")),
                    required(root, "conversation"),
                    root.path("sender").asString(null),
                    strings(root.path("receivers")),
                    targets(root.path("targets")),
                    Priority.valueOf(required(root, "priority").toUpperCase(Locale.ROOT)),
                    root.path("expiresAt").isString() ? Instant.parse(root.path("expiresAt").stringValue()) : null,
                    root.path("ttl").asInt(Message.DEFAULT_TTL),
                    headers(root.path("headers")));
            JsonNode performative = root.path("performative");
            if (performative.isString()) {
                JsonNode content = root.path("content");
                return ACLMessage.restore(envelope, Performative.fromFipaName(performative.stringValue()),
                        content.isString() ? content.stringValue() : null);
            }
            return Message.restore(envelope, content(root));
        } catch (JacksonException | DateTimeParseException | IllegalArgumentException | NullPointerException e) {
            throw new IOException("Invalid JSON message : " + e.getMessage(), e);
        }
    }

    private Object content(JsonNode root) throws IOException {
        JsonNode content = root.path("content");
        if (content.isMissingNode() || content.isNull()) {
            return null;
        }
        JsonNode contentType = root.path("contentType");
        if (contentType.isMissingNode() || contentType.isNull()) {
            return toJava(content);
        }
        Class<?> type = typesByName.get(contentType.asString());
        if (type == null) {
            throw new IOException("Unknown content type " + contentType.asString());
        }
        return mapper.treeToValue(content, type);
    }

    private static boolean isJsonValue(Object o) {
        return o instanceof String || o instanceof Number || o instanceof Boolean
                || o instanceof Map<?, ?> || o instanceof Collection<?>;
    }

    private static String required(JsonNode root, String field) throws IOException {
        JsonNode node = root.path(field);
        if (!node.isString()) {
            throw new IOException("Missing field " + field);
        }
        return node.stringValue();
    }

    private static List<String> strings(JsonNode array) {
        List<String> ret = new ArrayList<>();
        array.values().forEach(n -> ret.add(n.asString()));
        return ret;
    }

    private static List<Target> targets(JsonNode array) {
        List<Target> ret = new ArrayList<>();
        for (JsonNode t : array.values()) {
            ret.add(new Target(t.path("community").asString(null), t.path("group").asString(null), t.path("role").asString(null)));
        }
        return ret;
    }

    private static Map<String, Serializable> headers(JsonNode object) {
        Map<String, Serializable> ret = new HashMap<>();
        for (Map.Entry<String, JsonNode> e : object.properties()) {
            Serializable value = toJava(e.getValue());
            if (value != null) {
                ret.put(e.getKey(), value);
            }
        }
        return ret;
    }

    /**
     *
     * @param node
     * @return String, Number, Boolean, ArrayList or LinkedHashMap
     */
    private static Serializable toJava(JsonNode node) {
        if (node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isString()) {
            return node.stringValue();
        }
        if (node.isNumber()) {
            return (Serializable) node.numberValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isArray()) {
            ArrayList<Object> list = new ArrayList<>();
            node.values().forEach(n -> list.add(toJava(n)));
            return list;
        }
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> e : node.properties()) {
            map.put(e.getKey(), toJava(e.getValue()));
        }
        return map;
    }
}
