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
package fr.eloane.javamas.kernel.datas;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

/**
 * Tree Collection : the children of a node are unique.<br />
 * Safe for concurrent reads and writes.
 *
 * @author Guillaume Monet
 * @param <T>
 */
public class SynchronizedTree<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = -1075077374642499790L;

    private final T root;
    // ArrayList (guarded by this) keeps the serialized form of previous versions
    private final ArrayList<SynchronizedTree<T>> childs = new ArrayList<>();
    private volatile double weight = 0;

    /**
     *
     */
    public SynchronizedTree() {
        this(null);
    }

    /**
     *
     * @param element
     */
    public SynchronizedTree(T element) {
        this.root = element;
    }

    /**
     * Add a child, if a child with the same element already exists it is
     * returned
     *
     * @param element
     * @return the child tree
     */
    public SynchronizedTree<T> addNode(T element) {
        return this.addNode(element, 0);
    }

    /**
     * Add a child, if a child with the same element already exists it is
     * returned
     *
     * @param element
     * @param weight
     * @return the child tree
     */
    public synchronized SynchronizedTree<T> addNode(T element, double weight) {
        SynchronizedTree<T> existing = this.getChild(element);
        if (existing != null) {
            return existing;
        }
        SynchronizedTree<T> t = new SynchronizedTree<>(element);
        t.setWeight(weight);
        childs.add(t);
        return t;
    }

    /**
     *
     * @param weight
     */
    public void setWeight(double weight) {
        this.weight = weight;
    }

    /**
     *
     * @return
     */
    public double getWeight() {
        return this.weight;
    }

    /**
     *
     * @param t
     */
    public synchronized void addSubTree(SynchronizedTree<T> t) {
        if (!childs.contains(t)) {
            childs.add(t);
        }
    }

    /**
     * Remove all the nodes with this element in the tree
     *
     * @param element
     */
    public void removeNode(T element) {
        this.removeChild(element);
        this.getChilds().forEach(t -> t.removeNode(element));
    }

    /**
     * Remove the direct child with this element
     *
     * @param element
     */
    public synchronized void removeChild(T element) {
        childs.removeIf(t -> Objects.equals(t.getRoot(), element));
    }

    /**
     *
     * @param element
     * @return the direct child with this element or null
     */
    public synchronized SynchronizedTree<T> getChild(T element) {
        for (SynchronizedTree<T> t : childs) {
            if (Objects.equals(t.getRoot(), element)) {
                return t;
            }
        }
        return null;
    }

    /**
     *
     * @param element
     * @return the first sub tree (depth first) with this element or null
     */
    public SynchronizedTree<T> getTree(T element) {
        if (Objects.equals(root, element)) {
            return this;
        }
        for (SynchronizedTree<T> t : this.getChilds()) {
            SynchronizedTree<T> found = t.getTree(element);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     *
     * @return a copy of the direct children
     */
    public synchronized ArrayList<SynchronizedTree<T>> getChilds() {
        return new ArrayList<>(this.childs);
    }

    /**
     *
     * @return the elements of the direct children
     */
    public synchronized ArrayList<T> getChildsAsList() {
        ArrayList<T> ret = new ArrayList<>();
        this.childs.forEach(t -> ret.add(t.getRoot()));
        return ret;
    }

    /**
     * Only on direct childs
     *
     * @param element
     * @return
     */
    public boolean contains(T element) {
        return this.getChild(element) != null;
    }

    /**
     *
     * @param tree
     * @return if this tree has the same root and contains all the branches of
     * the other tree
     */
    public boolean compare(SynchronizedTree<T> tree) {
        if (!Objects.equals(this.getRoot(), tree.getRoot())) {
            return false;
        }
        for (SynchronizedTree<T> subtree : tree.getChilds()) {
            SynchronizedTree<T> child = this.getChild(subtree.getRoot());
            if (child == null || !child.compare(subtree)) {
                return false;
            }
        }
        return true;
    }

    /**
     *
     * @return
     */
    public T getRoot() {
        return this.root;
    }

    /**
     *
     * @param element
     * @return the elements from the root to the element, empty if the element
     * is not in the tree
     */
    public ArrayList<T> getPath(T element) {
        ArrayList<T> path = new ArrayList<>();
        if (this.buildPath(element, path)) {
            Collections.reverse(path);
        }
        return path;
    }

    private boolean buildPath(T element, ArrayList<T> path) {
        boolean found = Objects.equals(root, element);
        for (SynchronizedTree<T> t : this.getChilds()) {
            if (found) {
                break;
            }
            found = t.buildPath(element, path);
        }
        if (found) {
            path.add(root);
        }
        return found;
    }

    /**
     *
     * @return
     */
    public synchronized boolean isLeaf() {
        return this.childs.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SynchronizedTree<?> tree && Objects.equals(tree.root, this.root);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(root);
    }

    /**
     *
     * @param prepend
     * @return
     */
    public String print(String prepend) {
        String indent = " " + prepend;
        StringBuilder ret = new StringBuilder(String.valueOf(root));
        for (SynchronizedTree<T> t : this.getChilds()) {
            ret.append('\n').append(indent).append(t.print(indent));
        }
        return ret.toString();
    }

    @Override
    public String toString() {
        return print("-");
    }

    @Serial
    private synchronized void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }
}
