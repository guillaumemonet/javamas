package fr.eloane.javamas.kernel.knowledge;

import fr.eloane.javamas.kernel.messages.Message;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KnowledgeTest {

    @Test
    void databaseSaveThenLoad(@TempDir Path dir) {
        Path file = dir.resolve("agent.db");
        Database<Integer> db = new Database<>(file);
        assertTrue(db.isEmpty());
        db.put("a", 1);
        db.put("b", 2);
        db.save();
        assertEquals(db, new Database<Integer>(file));
    }

    @Test
    void historyKeepsTheLastMessages() {
        MessageHistory history = new MessageHistory(2);
        Message<String> first = new Message<>("1");
        history.record(first);
        history.record(first.reply("2"));
        history.record(new Message<>("3"));
        assertEquals(2, history.size());
        assertEquals(List.of("2"), history.conversation(first.getConversationId()).stream().map(Message::getContent).toList());
    }
}
