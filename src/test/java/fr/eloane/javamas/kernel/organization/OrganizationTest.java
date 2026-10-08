package fr.eloane.javamas.kernel.organization;

import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class OrganizationTest {

    @Test
    void joiningARoleJoinsItsGroupAndCommunity() {
        Organization org = new Organization().addRole("lab", "team", "leader");
        assertTrue(org.isInCommunity("lab"));
        assertTrue(org.isInGroup("lab", "team"));
        assertTrue(org.hasRole("lab", "team", "leader"));
        assertEquals(Set.of(Target.community("lab"), Target.group("lab", "team"), Target.role("lab", "team", "leader")),
                org.getMemberships());
    }

    @Test
    void matching() {
        Organization org = new Organization().joinGroup("lab", "team");
        assertTrue(org.matches(Target.all()));
        assertTrue(org.matches(Target.community("lab")));
        assertTrue(org.matches(Target.group("lab", "team")));
        assertFalse(org.matches(Target.role("lab", "team", "leader")));
        assertFalse(org.matches(Target.group("lab", "other")));
        assertFalse(org.matches(Target.community("other")));
        assertTrue(new Organization().matches(Target.all()));
    }

    @Test
    void leavingRemovesTheInnerMemberships() {
        Organization org = new Organization()
                .addRole("lab", "team", "leader")
                .addRole("lab", "other", "member");
        org.removeRole("lab", "team", "leader");
        assertFalse(org.hasRole("lab", "team", "leader"));
        assertTrue(org.isInGroup("lab", "team"));

        org.leaveGroup("lab", "team");
        assertFalse(org.isInGroup("lab", "team"));
        assertTrue(org.hasRole("lab", "other", "member"));

        org.leaveCommunity("lab");
        assertTrue(org.getMemberships().isEmpty());
    }
}
