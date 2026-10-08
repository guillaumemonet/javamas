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

import java.io.Serial;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Control the steps of an agent's life cycle : delay between steps, pause,
 * resume and stop.<br />
 * Uses a lock instead of synchronized so virtual threads are not pinned while
 * waiting.
 *
 * @author Guillaume Monet
 */
public class Scheduler implements Serializable {

    @Serial
    private static final long serialVersionUID = 992501499415467420L;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition changed = lock.newCondition();
    private boolean paused = false;
    private boolean running = true;
    private long delayNanos = 0;
    private long pauseUntil = 0;

    /**
     * Wait for the next step of the life cycle
     *
     * @return if life cycle can proceed
     */
    public boolean nextStep() {
        lock.lock();
        try {
            while (running && (paused || pauseUntil - System.nanoTime() > 0)) {
                if (paused) {
                    changed.await();
                } else {
                    changed.awaitNanos(pauseUntil - System.nanoTime());
                }
            }
            pauseUntil = 0;
            long remaining = delayNanos;
            while (running && !paused && remaining > 0) {
                remaining = changed.awaitNanos(remaining);
            }
            return running;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            running = false;
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Pause until {@link #resume()}
     */
    public void pause() {
        update(() -> paused = true);
    }

    /**
     * Pause during time
     *
     * @param time pause duration in milliseconds
     */
    public void pause(long time) {
        update(() -> pauseUntil = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(time));
    }

    /**
     * Resume after a pause
     */
    public void resume() {
        update(() -> {
            paused = false;
            pauseUntil = 0;
        });
    }

    /**
     * Stop the life cycle, {@link #nextStep()} will return false
     */
    public void stop() {
        update(() -> running = false);
    }

    /**
     *
     * @param delay delay between each step in milliseconds
     */
    public void setDelay(long delay) {
        update(() -> delayNanos = TimeUnit.MILLISECONDS.toNanos(delay));
    }

    /**
     *
     * @return if the life cycle is not stopped
     */
    public boolean isRunning() {
        lock.lock();
        try {
            return running;
        } finally {
            lock.unlock();
        }
    }

    private void update(Runnable change) {
        lock.lock();
        try {
            change.run();
            changed.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
