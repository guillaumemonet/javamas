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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * ObjectInputStream that only accepts allowed classes.<br />
 * Messages received from the network must never instantiate arbitrary classes
 * (deserialization gadgets), so only JavaMAS classes and a few basic JDK types
 * are accepted by default. Classes used as message content must be allowed with
 * {@link #allowClass(Class)} or {@link #allowPackage(String)}.<br />
 * The size, depth and number of references of the stream are also limited.
 *
 * @author Guillaume Monet
 */
public class SecureObjectInputStream extends ObjectInputStream {

    private static final long MAX_BYTES = 1024 * 1024;
    private static final long MAX_DEPTH = 32;
    private static final long MAX_REFERENCES = 100_000;
    private static final long MAX_ARRAY_LENGTH = 100_000;

    private static final Set<String> ALLOWED_CLASSES = new CopyOnWriteArraySet<>(Set.of(
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
            "java.math.BigInteger",
            "java.math.BigDecimal",
            "java.util.ArrayList",
            "java.util.LinkedList",
            "java.util.HashMap",
            "java.util.LinkedHashMap",
            "java.util.HashSet",
            "java.util.LinkedHashSet",
            "java.util.Date",
            "java.util.UUID",
            "java.util.concurrent.CopyOnWriteArrayList"
    ));

    private static final Set<String> ALLOWED_PACKAGES = new CopyOnWriteArraySet<>(Set.of(
            "fr.eloane.javamas."
    ));

    private static final ObjectInputFilter FILTER = SecureObjectInputStream::check;

    /**
     *
     * @param in
     * @throws IOException
     */
    public SecureObjectInputStream(InputStream in) throws IOException {
        super(in);
        setObjectInputFilter(FILTER);
    }

    /**
     * Allow a class to be received from the network
     *
     * @param clazz the class to allow
     */
    public static void allowClass(Class<?> clazz) {
        ALLOWED_CLASSES.add(clazz.getName());
    }

    /**
     * Allow all classes of a package (and its sub packages) to be received from
     * the network
     *
     * @param packageName the package name, e.g. "com.example.content"
     */
    public static void allowPackage(String packageName) {
        ALLOWED_PACKAGES.add(packageName.endsWith(".") ? packageName : packageName + ".");
    }

    /**
     *
     * @param className a class name as returned by {@link Class#getName()}
     * @return if the class can be deserialized
     */
    public static boolean isAllowed(String className) {
        String name = className;
        while (name.startsWith("[")) {
            name = name.substring(1);
        }
        if (name.length() == 1 && !name.equals(className)) {
            // Array of primitives
            return true;
        }
        if (name.startsWith("L") && name.endsWith(";")) {
            name = name.substring(1, name.length() - 1);
        }
        if (ALLOWED_CLASSES.contains(name)) {
            return true;
        }
        for (String pkg : ALLOWED_PACKAGES) {
            if (name.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }

    private static ObjectInputFilter.Status check(ObjectInputFilter.FilterInfo info) {
        if (info.streamBytes() > MAX_BYTES || info.depth() > MAX_DEPTH
                || info.references() > MAX_REFERENCES || info.arrayLength() > MAX_ARRAY_LENGTH) {
            return ObjectInputFilter.Status.REJECTED;
        }
        Class<?> clazz = info.serialClass();
        if (clazz == null) {
            return ObjectInputFilter.Status.UNDECIDED;
        }
        return isAllowed(clazz.getName()) ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
    }
}
