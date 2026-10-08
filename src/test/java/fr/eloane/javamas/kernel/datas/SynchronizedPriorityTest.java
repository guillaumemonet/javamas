package fr.eloane.javamas.kernel.datas;

import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class SynchronizedPriorityTest {

    @Test
    void popWithTimeoutReturnsImmediatelyWhenNotEmpty() {
        SynchronizedPriority<Integer> queue = new SynchronizedPriority<>();
        queue.push(2);
        queue.push(1);
        long start = System.nanoTime();
        assertEquals(1, queue.pop(5000));
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 1000);
    }

    @Test
    void popWithTimeoutReturnsNullWhenEmpty() {
        assertNull(new SynchronizedPriority<Integer>().pop(50));
        assertNull(new SynchronizedQueue<Integer>().pop(50));
    }
}
