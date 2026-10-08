package fr.eloane.javamas.kernel;

import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseTest {

    @Test
    void saveThenLoad(@TempDir Path dir) {
        Path file = dir.resolve("agent.db");
        Database<Integer> db = new Database<>(file, true);
        assertTrue(db.isEmpty());
        db.put("a", 1);
        db.put("b", 2);
        db.save();

        Database<Integer> loaded = new Database<>(file, true);
        assertEquals(db, loaded);

        loaded.flush();
        assertTrue(new Database<Integer>(file, true).isEmpty());
    }
}
