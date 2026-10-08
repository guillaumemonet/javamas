package fr.eloane.javamas.kernel;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class SchedulerTest {

    @Test
    void stopWakesUpAPausedAgent() throws Exception {
        Scheduler scheduler = new Scheduler();
        scheduler.pause();
        CompletableFuture<Boolean> step = CompletableFuture.supplyAsync(scheduler::nextStep);
        Thread.sleep(100);
        assertFalse(step.isDone());
        scheduler.stop();
        assertFalse(step.get(1, TimeUnit.SECONDS));
    }

    @Test
    void resumeWakesUpAPausedAgent() throws Exception {
        Scheduler scheduler = new Scheduler();
        scheduler.pause();
        CompletableFuture<Boolean> step = CompletableFuture.supplyAsync(scheduler::nextStep);
        Thread.sleep(100);
        scheduler.resume();
        assertTrue(step.get(1, TimeUnit.SECONDS));
    }

    @Test
    void timedPauseAndDelay() {
        Scheduler scheduler = new Scheduler();
        scheduler.pause(100);
        long start = System.nanoTime();
        assertTrue(scheduler.nextStep());
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) >= 90);

        scheduler.setDelay(100);
        start = System.nanoTime();
        assertTrue(scheduler.nextStep());
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) >= 90);
    }
}
