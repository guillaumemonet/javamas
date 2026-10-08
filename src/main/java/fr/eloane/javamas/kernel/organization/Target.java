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

import java.io.Serializable;

/**
 * Part of an organization : everybody, a community, a group of a community or
 * a role in a group.<br />
 * Used as the destination of a message and as a membership of an agent.
 *
 * @param community null for everybody
 * @param group null for the whole community
 * @param role null for the whole group
 * @author Guillaume Monet
 */
public record Target(String community, String group, String role) implements Serializable {

    private static final Target ALL = new Target(null, null, null);

    public Target {
        if (group != null && community == null) {
            throw new IllegalArgumentException("A group needs a community");
        }
        if (role != null && group == null) {
            throw new IllegalArgumentException("A role needs a group");
        }
    }

    /**
     *
     * @return all the agents
     */
    public static Target all() {
        return ALL;
    }

    /**
     *
     * @param community
     * @return the members of the community
     */
    public static Target community(String community) {
        return new Target(community, null, null);
    }

    /**
     *
     * @param community
     * @param group
     * @return the members of the group
     */
    public static Target group(String community, String group) {
        return new Target(community, group, null);
    }

    /**
     *
     * @param community
     * @param group
     * @param role
     * @return the agents playing the role
     */
    public static Target role(String community, String group, String role) {
        return new Target(community, group, role);
    }

    /**
     *
     * @param other
     * @return if this target is the other one or is inside it (a group is
     * inside its community)
     */
    public boolean isWithin(Target other) {
        return (other.community == null || other.community.equals(community))
                && (other.group == null || other.group.equals(group))
                && (other.role == null || other.role.equals(role));
    }

    @Override
    public String toString() {
        if (community == null) {
            return "*";
        }
        return community + (group == null ? "" : "/" + group) + (role == null ? "" : "/" + role);
    }
}
