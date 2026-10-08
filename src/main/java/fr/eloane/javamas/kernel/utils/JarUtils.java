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
package fr.eloane.javamas.kernel.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 *
 * @author guillaume
 * @version 2.0.0
 */
public final class JarUtils {

    private JarUtils() {
    }

    /**
     * Read a text resource
     *
     * @param cls class used to locate the resource
     * @param filename name of the resource, relative to the class
     * @return the lines of the resource (UTF-8) concatenated without line
     * separators
     * @throws IllegalArgumentException if the resource doesn't exist
     * @throws UncheckedIOException if the resource can't be read
     */
    public static String loadContentFile(Class<?> cls, String filename) {
        InputStream in = cls.getResourceAsStream(filename);
        if (in == null) {
            throw new IllegalArgumentException("Resource not found : " + filename);
        }
        try (BufferedReader rd = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return rd.lines().collect(Collectors.joining());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
