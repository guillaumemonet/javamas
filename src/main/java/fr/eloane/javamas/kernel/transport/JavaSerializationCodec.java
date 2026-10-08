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

import fr.eloane.javamas.kernel.messages.Message;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Java serialization codec.<br />
 * Data received from the network must never instantiate arbitrary classes
 * (deserialization gadgets) : only JavaMAS classes and basic JDK types are
 * accepted. The classes used as message content or header values must be
 * allowed with {@link #allowClass(Class)} or {@link #allowPackage(String)}.
 * The size, depth and number of references of a message are also limited.
 *
 * @author Guillaume Monet
 */
public final class JavaSerializationCodec implements MessageCodec {

    private static final long MAX_BYTES = 16 * 1024 * 1024;
    private static final long MAX_DEPTH = 32;
    private static final long MAX_REFERENCES = 100_000;
    private static final long MAX_ARRAY_LENGTH = 1_000_000;

    private static final Set<String> DEFAULT_CLASSES = Set.of(
            // Checked by collections for their internal arrays, never instantiated
            "java.lang.Object",
            "java.util.Map$Entry",
            "java.lang.String",
            "java.lang.Boolean",
            "java.lang.Character",
            "java.lang.Number",
            "java.lang.Byte",
            "java.lang.Short",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Float",
            "java.lang.Double",
            "java.lang.Enum",
            "java.lang.Record",
            "java.math.BigInteger",
            "java.math.BigDecimal",
            "java.time.Ser",
            "java.time.Instant",
            "java.time.Duration",
            "java.util.ArrayList",
            "java.util.LinkedList",
            "java.util.HashMap",
            "java.util.LinkedHashMap",
            "java.util.HashSet",
            "java.util.LinkedHashSet",
            "java.util.Date",
            "java.util.UUID"
    );

    private final Set<String> allowedClasses = new CopyOnWriteArraySet<>(DEFAULT_CLASSES);
    private final Set<String> allowedPackages = new CopyOnWriteArraySet<>(Set.of("fr.eloane.javamas."));
    private final ObjectInputFilter filter = this::check;

    /**
     * Allow a class to be received
     *
     * @param clazz
     * @return this
     */
    public JavaSerializationCodec allowClass(Class<?> clazz) {
        allowedClasses.add(clazz.getName());
        return this;
    }

    /**
     * Allow all the classes of a package and its sub packages to be received
     *
     * @param packageName e.g. "com.example.content"
     * @return this
     */
    public JavaSerializationCodec allowPackage(String packageName) {
        allowedPackages.add(packageName.endsWith(".") ? packageName : packageName + ".");
        return this;
    }

    /**
     *
     * @param clazz
     * @return if the class can be received
     */
    public boolean isAllowed(Class<?> clazz) {
        Class<?> c = clazz;
        while (c.isArray()) {
            c = c.getComponentType();
        }
        if (c.isPrimitive() || allowedClasses.contains(c.getName())) {
            return true;
        }
        return allowedPackages.stream().anyMatch(c.getName()::startsWith);
    }

    @Override
    public byte[] encode(Message<?> message) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bout)) {
            out.writeObject(message);
        }
        return bout.toByteArray();
    }

    @Override
    public Message<?> decode(byte[] data, int offset, int length) throws IOException {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data, offset, length))) {
            in.setObjectInputFilter(filter);
            Object o = in.readObject();
            if (o instanceof Message<?> m) {
                return m;
            }
            throw new InvalidClassException(o == null ? "null" : o.getClass().getName(), "not a message");
        } catch (ClassNotFoundException e) {
            throw new InvalidClassException(e.getMessage());
        }
    }

    private ObjectInputFilter.Status check(ObjectInputFilter.FilterInfo info) {
        if (info.streamBytes() > MAX_BYTES || info.depth() > MAX_DEPTH
                || info.references() > MAX_REFERENCES || info.arrayLength() > MAX_ARRAY_LENGTH) {
            return ObjectInputFilter.Status.REJECTED;
        }
        Class<?> clazz = info.serialClass();
        if (clazz == null) {
            return ObjectInputFilter.Status.UNDECIDED;
        }
        return isAllowed(clazz) ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
    }
}
