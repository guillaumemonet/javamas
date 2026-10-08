package fr.eloane.javamas.kernel.organization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class OrganizationTest {

    @Test
    void compareUsesValueEqualityNotIdentity() {
        Organization agent = new Organization().joinGroup("Agent", "Convoyeur");
        Organization target = new Organization(new String("WORLD")).joinGroup(new String("Agent"), new String("Convoyeur"));
        assertTrue(agent.compare(target));
        assertFalse(agent.compare(new Organization().joinGroup("Agent", "Fourmis")));
    }

    @Test
    void compareWorksAfterSerialization() throws Exception {
        Organization target = new Organization().joinGroup("Agent", "Convoyeur");
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bout)) {
            out.writeObject(target);
        }
        Organization received;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bout.toByteArray()))) {
            received = (Organization) in.readObject();
        }
        assertTrue(new Organization().addRole("Agent", "Convoyeur", "Leader").compare(received));
    }

    @Test
    void leaveRemovesOnlyTheTargetedNode() {
        Organization org = new Organization()
                .addRole("WORLD", "BEEGROUP", "BEE")
                .addRole("WORLD", "QUEEN", "BEE");
        org.removeRole("WORLD", "QUEEN", "BEE");
        assertFalse(org.hasRole("WORLD", "QUEEN", "BEE"));
        assertTrue(org.hasRole("WORLD", "BEEGROUP", "BEE"));

        org.leaveGroup("WORLD", "BEEGROUP");
        assertFalse(org.isInGroup("WORLD", "BEEGROUP"));
        assertTrue(org.isInGroup("WORLD", "QUEEN"));

        org.leaveCommunity("WORLD");
        assertFalse(org.isInCommunity("WORLD"));
    }

    @Test
    void pathToAnElement() {
        Organization org = new Organization().addRole("Agent", "Convoyeur", "Leader");
        assertEquals(List.of("WORLD", "Agent", "Convoyeur", "Leader"), org.getPath("Leader"));
        assertEquals(List.of(), org.getPath("Unknown"));
    }
}
