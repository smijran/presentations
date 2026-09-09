package virtual.threads.demo;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Act 1: fire a large number of IO-bound tasks (simulated via Thread.sleep)
 * at a fixed-size platform thread pool vs a virtual-thread-per-task
 * executor. The platform pool queues and stalls once task count exceeds
 * pool size; virtual threads scale to the task count directly since a
 * blocked (sleeping) virtual thread doesn't occupy a carrier thread.
 */
public class ThroughputApp {

    private static final int TASK_COUNT = Integer.getInteger("demo.taskCount", 50_000);
    private static final int BLOCKING_MILLIS = Integer.getInteger("demo.blockingMillis", 50);
    private static final int PLATFORM_POOL_SIZE = Integer.getInteger("demo.platformPoolSize", 200);

    public static void main(String[] args) throws InterruptedException {
        System.out.printf("Virtual Threads throughput demo — %,d blocking tasks, %dms each%n%n",
                TASK_COUNT, BLOCKING_MILLIS);

        Duration platformDuration = runWith(
                "Platform threads (fixed pool of " + PLATFORM_POOL_SIZE + ")",
                Executors.newFixedThreadPool(PLATFORM_POOL_SIZE));

        Duration virtualDuration = runWith(
                "Virtual threads (one per task)",
                Executors.newVirtualThreadPerTaskExecutor());

        System.out.println();
        System.out.printf("Platform pool total time:   %,d ms%n", platformDuration.toMillis());
        System.out.printf("Virtual threads total time: %,d ms%n", virtualDuration.toMillis());
        System.out.printf("Speedup:                     %.1fx%n",
                (double) platformDuration.toMillis() / Math.max(1, virtualDuration.toMillis()));
        System.out.printf("Theoretical minimum (unlimited concurrency): ~%d ms%n", BLOCKING_MILLIS);
    }

    private static Duration runWith(String label, ExecutorService executor) throws InterruptedException {
        System.out.println("Running: " + label);
        CountDownLatch latch = new CountDownLatch(TASK_COUNT);
        Instant start = Instant.now();
        try (executor) {
            for (int i = 0; i < TASK_COUNT; i++) {
                executor.submit(() -> {
                    try {
                        Thread.sleep(BLOCKING_MILLIS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
        }
        Duration elapsed = Duration.between(start, Instant.now());
        System.out.printf("  -> completed %,d tasks in %,d ms%n%n", TASK_COUNT, elapsed.toMillis());
        return elapsed;
    }
}
