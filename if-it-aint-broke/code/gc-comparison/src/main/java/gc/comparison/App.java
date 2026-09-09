package gc.comparison;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 * A tiny "request latency under GC pressure" demo.
 *
 * One thread keeps allocating garbage in the background (short-lived churn
 * plus a growing retained set, to look like a real service's heap) while the
 * main thread simulates fixed-rate request processing and records how long
 * each "request" took. GC pauses show up directly as latency spikes in the
 * report at the end — no external tooling required, though -Xlog:gc output
 * is also written to build/gc-logs/ for a deeper look.
 *
 * Run via the Gradle tasks in build.gradle (runG1, runG1Numa, runZGC,
 * runShenandoah) to compare GCs on the exact same workload.
 */
public class App {

    private static final int DURATION_SECONDS = Integer.getInteger("demo.durationSeconds", 30);
    private static final int RETAINED_HEAP_MB = Integer.getInteger("demo.retainedHeapMb", 300);
    private static final int REQUEST_INTERVAL_MICROS = Integer.getInteger("demo.requestIntervalMicros", 200);

    public static void main(String[] args) throws InterruptedException {
        System.out.printf(
                "GC comparison demo — %ds run, ~%dMB retained, one \"request\" every %dµs%n",
                DURATION_SECONDS, RETAINED_HEAP_MB, REQUEST_INTERVAL_MICROS);
        System.out.println("Active GC(s): " + activeGcNames());
        System.out.println();

        AtomicBoolean stop = new AtomicBoolean(false);
        Thread churner = new Thread(() -> churnGarbage(stop), "garbage-churner");
        churner.setDaemon(true);
        churner.start();

        List<Long> latenciesNanos = new ArrayList<>(1_000_000);
        long endAt = System.nanoTime() + DURATION_SECONDS * 1_000_000_000L;
        long intervalNanos = REQUEST_INTERVAL_MICROS * 1_000L;

        while (System.nanoTime() < endAt) {
            long start = System.nanoTime();
            simulateRequest();
            latenciesNanos.add(System.nanoTime() - start);
            LockSupport.parkNanos(intervalNanos);
        }

        stop.set(true);
        churner.join(2_000);

        report(latenciesNanos);
    }

    /** Background allocation pressure: short-lived churn + a growing retained set. */
    private static void churnGarbage(AtomicBoolean stop) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        List<byte[]> retained = new ArrayList<>();
        long retainedTargetBytes = RETAINED_HEAP_MB * 1024L * 1024L;
        long retainedBytes = 0;

        while (!stop.get()) {
            byte[] junk = new byte[rnd.nextInt(512, 8192)];
            junk[0] = 1; // touch it so the allocation can't be optimized away
            if (retainedBytes < retainedTargetBytes) {
                byte[] keep = new byte[16 * 1024];
                retained.add(keep);
                retainedBytes += keep.length;
            }
        }
    }

    /** Stand-in for request processing — trivial CPU work, timed by the caller. */
    private static long simulateRequest() {
        long acc = 0;
        for (int i = 0; i < 1_000; i++) {
            acc += (long) i * i;
        }
        return acc;
    }

    private static void report(List<Long> latenciesNanos) {
        long[] sorted = latenciesNanos.stream().mapToLong(Long::longValue).sorted().toArray();
        System.out.printf("Requests processed: %d%n", sorted.length);
        System.out.printf("  min:     %8.3f ms%n", toMillis(sorted[0]));
        System.out.printf("  p50:     %8.3f ms%n", toMillis(percentile(sorted, 50)));
        System.out.printf("  p99:     %8.3f ms%n", toMillis(percentile(sorted, 99)));
        System.out.printf("  p99.9:   %8.3f ms%n", toMillis(percentile(sorted, 99.9)));
        System.out.printf("  max:     %8.3f ms   <- worst pause a real request would have felt%n",
                toMillis(sorted[sorted.length - 1]));
    }

    private static double toMillis(long nanos) {
        return nanos / 1_000_000.0;
    }

    private static long percentile(long[] sorted, double p) {
        int idx = (int) Math.ceil(p / 100.0 * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(idx, sorted.length - 1))];
    }

    private static String activeGcNames() {
        StringBuilder sb = new StringBuilder();
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(bean.getName());
        }
        return sb.toString();
    }
}
