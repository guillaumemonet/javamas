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
package fr.eloane.javamas.kernel;

import fr.eloane.javamas.kernel.utils.FileUtils;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Persistent key/value store of an agent, saved with java serialization
 *
 * @author Guillaume Monet
 * @param <T> type of the stored values
 */
public final class Database<T> extends HashMap<String, T> {

    private static final long serialVersionUID = 1L;

    private final transient Path db;

    /**
     *
     * @param filename name of the database file in the user's home (without
     * extension)
     * @param load load the existing content
     */
    public Database(String filename, boolean load) {
        this(FileUtils.HOME_PATH.resolve(filename + ".db"), load);
    }

    /**
     *
     * @param file the database file
     * @param load load the existing content
     */
    public Database(Path file, boolean load) {
        this.db = file;
        if (load) {
            this.load();
        }
    }

    /**
     * Remove all values and save
     */
    public synchronized void flush() {
        super.clear();
        this.save();
    }

    /**
     * Load the content of the file, if it exists
     *
     * @throws UncheckedIOException if the file can't be read
     */
    @SuppressWarnings("unchecked")
    public synchronized void load() {
        if (!Files.exists(db)) {
            return;
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(db))) {
            super.clear();
            super.putAll((Map<String, T>) in.readObject());
        } catch (IOException ex) {
            throw new UncheckedIOException("Can't load database " + db, ex);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Can't load database " + db, ex);
        }
    }

    /**
     * Save the content to the file
     *
     * @throws UncheckedIOException if the file can't be written
     */
    public synchronized void save() {
        try {
            Path parent = db.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            Path tmp = Files.createTempFile(parent, db.getFileName().toString(), ".tmp");
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tmp))) {
                out.writeObject(new HashMap<>(this));
            }
            Files.move(tmp, db, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new UncheckedIOException("Can't save database " + db, ex);
        }
    }

    /**
     *
     * @return the database file
     */
    public Path getFile() {
        return db;
    }
}
