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
package fr.eloane.javamas.kernel.organization;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Memberships of an agent : communities, groups in a community and roles in a
 * group (Agent Group Role model).<br />
 * Joining a group also joins its community, playing a role also joins its
 * group. Thread safe.
 *
 * @author Guillaume Monet
 */
public final class Organization {

    private final Set<Target> memberships = ConcurrentHashMap.newKeySet();

    /**
     *
     * @param community
     * @return this
     */
    public Organization joinCommunity(String community) {
        memberships.add(Target.community(community));
        return this;
    }

    /**
     * Leave the community and all its groups and roles
     *
     * @param community
     */
    public void leaveCommunity(String community) {
        memberships.removeIf(t -> t.isWithin(Target.community(community)));
    }

    /**
     *
     * @param community
     * @param group
     * @return this
     */
    public Organization joinGroup(String community, String group) {
        joinCommunity(community);
        memberships.add(Target.group(community, group));
        return this;
    }

    /**
     * Leave the group and all its roles
     *
     * @param community
     * @param group
     */
    public void leaveGroup(String community, String group) {
        memberships.removeIf(t -> t.isWithin(Target.group(community, group)));
    }

    /**
     *
     * @param community
     * @param group
     * @param role
     * @return this
     */
    public Organization addRole(String community, String group, String role) {
        joinGroup(community, group);
        memberships.add(Target.role(community, group, role));
        return this;
    }

    /**
     *
     * @param community
     * @param group
     * @param role
     */
    public void removeRole(String community, String group, String role) {
        memberships.remove(Target.role(community, group, role));
    }

    public boolean isInCommunity(String community) {
        return memberships.contains(Target.community(community));
    }

    public boolean isInGroup(String community, String group) {
        return memberships.contains(Target.group(community, group));
    }

    public boolean hasRole(String community, String group, String role) {
        return memberships.contains(Target.role(community, group, role));
    }

    /**
     *
     * @param target
     * @return if the agent is part of the target
     */
    public boolean matches(Target target) {
        return target.community() == null || memberships.contains(target);
    }

    /**
     *
     * @return a copy of the memberships
     */
    public Set<Target> getMemberships() {
        return Set.copyOf(memberships);
    }

    /**
     * Leave everything
     */
    public void clear() {
        memberships.clear();
    }

    @Override
    public String toString() {
        return memberships.toString();
    }
}
