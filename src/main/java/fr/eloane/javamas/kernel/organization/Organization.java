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

import fr.eloane.javamas.kernel.datas.SynchronizedTree;
import java.io.Serial;

/**
 * Community Group Role manager
 *
 * @author Guillaume Monet
 */
public class Organization extends SynchronizedTree<String> {

    @Serial
    private static final long serialVersionUID = 3957756955695688576L;

    public Organization() {
        super("WORLD");
    }

    public Organization(String name) {
        super(name);
    }

    /**
     *
     * @param community
     * @return
     */
    public Organization joinCommunity(String community) {
        this.addNode(community);
        return this;
    }

    /**
     *
     * @param community
     */
    public void leaveCommunity(String community) {
        this.removeChild(community);
    }

    /**
     *
     * @param community
     * @return
     */
    public boolean isInCommunity(String community) {
        return this.contains(community);
    }

    /**
     *
     * @param community
     * @param group
     * @return
     */
    public Organization joinGroup(String community, String group) {
        this.addNode(community).addNode(group);
        return this;
    }

    /**
     *
     * @param community
     * @param group
     */
    public void leaveGroup(String community, String group) {
        SynchronizedTree<String> c = this.getChild(community);
        if (c != null) {
            c.removeChild(group);
        }
    }

    /**
     *
     * @param community
     * @param group
     * @return
     */
    public boolean isInGroup(String community, String group) {
        return this.getGroup(community, group) != null;
    }

    /**
     *
     * @param community
     * @param group
     * @param role
     * @return
     */
    public Organization addRole(String community, String group, String role) {
        this.addNode(community).addNode(group).addNode(role);
        return this;
    }

    /**
     *
     * @param community
     * @param group
     * @param role
     */
    public void removeRole(String community, String group, String role) {
        SynchronizedTree<String> g = this.getGroup(community, group);
        if (g != null) {
            g.removeChild(role);
        }
    }

    /**
     *
     * @param community
     * @param group
     * @param role
     * @return
     */
    public boolean hasRole(String community, String group, String role) {
        SynchronizedTree<String> g = this.getGroup(community, group);
        return g != null && g.contains(role);
    }

    private SynchronizedTree<String> getGroup(String community, String group) {
        SynchronizedTree<String> c = this.getChild(community);
        return c == null ? null : c.getChild(group);
    }

    public boolean compare(Organization organization) {
        return super.compare(organization);
    }
}
