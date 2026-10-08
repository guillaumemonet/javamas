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

import java.time.Duration;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Controls the steps of an agent's life cycle : delay between steps, pause,
 * resume and stop.<br />
 * Uses a lock instead of synchronized so virtual threads are not pinned while
 * waiting.
 *
 * @author Guillaume Monet
 */
public final class Scheduler {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition changed = lock.newCondition();
    private boolean running = true;
    private boolean paused = false;
    private boolean timedPause = false;
    private long pauseUntil = 0;
    private long delayNanos = 0;

    /**
     * Wait for the next step : during the pause and the delay between steps
     *
     * @return true if the life cycle can proceed, false if it is stopped
     */
    public boolean nextStep() {
        lock.lock();
        try {
            long delay = delayNanos;
            while (running) {
                if (paused) {
                    changed.await();
                } else if (timedPause && pauseUntil - System.nanoTime() > 0) {
                    changed.awaitNanos(pauseUntil - System.nanoTime());
                } else if (delay > 0) {
                    timedPause = false;
                    delay = changed.awaitNanos(delay);
                } else {
                    timedPause = false;
                    return true;
                }
            }
            return false;
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
     * Pause during a time
     *
     * @param duration
     */
    public void pause(Duration duration) {
        update(() -> {
            timedPause = true;
            pauseUntil = System.nanoTime() + duration.toNanos();
        });
    }

    /**
     * Resume after a pause
     */
    public void resume() {
        update(() -> {
            paused = false;
            timedPause = false;
        });
    }

    /**
     * Stop the life cycle : {@link #nextStep()} returns false
     */
    public void stop() {
        update(() -> running = false);
    }

    /**
     *
     * @param delay delay between each step
     */
    public void setDelay(Duration delay) {
        update(() -> delayNanos = delay.toNanos());
    }

    /**
     *
     * @return false if the life cycle is stopped
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
