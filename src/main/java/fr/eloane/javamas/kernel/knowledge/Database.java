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
package fr.eloane.javamas.kernel.knowledge;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Knowledge of an agent : a thread safe key/value store that can be saved to
 * a file with java serialization
 *
 * @param <V> type of the stored values
 * @author Guillaume Monet
 */
public final class Database<V> extends ConcurrentHashMap<String, V> {

    private final transient Path file;

    /**
     *
     * @param file the file of the database, loaded if it exists
     * @throws UncheckedIOException if the file exists but can't be read
     */
    public Database(Path file) {
        this.file = file;
        this.load();
    }

    /**
     * Replace the content by the content of the file, if it exists
     *
     * @throws UncheckedIOException if the file can't be read
     */
    @SuppressWarnings("unchecked")
    public synchronized void load() {
        if (!Files.exists(file)) {
            return;
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            Map<String, V> content = (Map<String, V>) in.readObject();
            super.clear();
            super.putAll(content);
        } catch (IOException ex) {
            throw new UncheckedIOException("Can't load database " + file, ex);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Can't load database " + file, ex);
        }
    }

    /**
     * Save the content to the file (atomically)
     *
     * @throws UncheckedIOException if the file can't be written
     */
    public synchronized void save() {
        try {
            Path parent = file.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            Path tmp = Files.createTempFile(parent, file.getFileName().toString(), ".tmp");
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tmp))) {
                out.writeObject(new HashMap<>(this));
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            throw new UncheckedIOException("Can't save database " + file, ex);
        }
    }

    public Path getFile() {
        return file;
    }
}
