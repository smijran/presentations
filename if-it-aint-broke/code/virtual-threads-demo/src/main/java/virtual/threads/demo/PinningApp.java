package virtual.threads.demo;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Act 2: demonstrates JEP 491 (Synchronize Virtual Threads without Pinning,
 * Java 24) purely through timing. Identical source/bytecode, run on Java 21
 * (pre-491: entering a synchronized block and then blocking inside it pins
 * the virtual thread to its carrier thread for the duration) vs Java 24+
 * (post-491: no pinning) produces a dramatic wall-clock difference.
 *
 * Each task synchronizes on its OWN private lock object, so there is no
 * real lock contention between tasks — concurrency here is limited only
 * by carrier-thread pinning (pre-491) or not limited at all (post-491).
 */
public class PinningApp {

    private static final int TASK_COUNT = Integer.getInteger("demo.taskCount", 500);
    private static final int BLOCKING_MILLIS = Integer.getInteger("demo.blockingMillis", 200);

    public static void main(String[] args) throws InterruptedException {
        int carrierThreads = Runtime.getRuntime().availableProcessors();
        System.out.printf("Virtual thread pinning demo — java.version=%s%n", System.getProperty("java.version"));
        System.out.printf("Carrier threads available (CPU cores): %d%n", carrierThreads);
        System.out.printf(
                "Running %,d virtual threads, each entering `synchronized { Thread.sleep(%dms) }` on its own private lock%n%n",
                TASK_COUNT, BLOCKING_MILLIS);

        CountDownLatch latch = new CountDownLatch(TASK_COUNT);
        Instant start = Instant.now();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < TASK_COUNT; i++) {
                executor.submit(() -> {
                    Object privateLock = new Object();
                    synchronized (privateLock) {
                        try {
                            Thread.sleep(BLOCKING_MILLIS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    latch.countDown();
                });
            }
            latch.await();
        }
        Duration elapsed = Duration.between(start, Instant.now());

        long pinnedEstimateMillis = (long) Math.ceil((double) TASK_COUNT / carrierThreads) * BLOCKING_MILLIS;

        System.out.printf("Completed in %,d ms%n", elapsed.toMillis());
        System.out.printf("Theoretical minimum with NO pinning:            ~%d ms%n", BLOCKING_MILLIS);
        System.out.printf("Theoretical minimum WITH pinning (%d carriers): ~%,d ms%n",
                carrierThreads, pinnedEstimateMillis);
    }
}
