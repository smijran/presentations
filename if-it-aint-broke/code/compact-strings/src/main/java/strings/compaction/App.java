package strings.compaction;

import com.sun.management.HotSpotDiagnosticMXBean;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.List;

/**
 * Reserve a lot of heap for ASCII strings, then measure exactly how much
 * memory they cost — with Compact Strings (JEP 254) on vs off.
 *
 * Compact Strings is on by default since Java 9: any String whose content
 * fits Latin-1 (which plain ASCII always does) is backed by a byte[]
 * instead of a char[], halving the array's memory. The flag lets us force
 * the old behaviour back on to make the saving visible: same workload,
 * same heap size, only -XX:+CompactStrings vs -XX:-CompactStrings changes.
 *
 * Run via the Gradle tasks in build.gradle (runWithCompactStrings,
 * runWithoutCompactStrings) to compare.
 */
public class App {

    private static final int STRING_COUNT = Integer.getInteger("demo.stringCount", 5_000_000);
    private static final int STRING_LENGTH = Integer.getInteger("demo.stringLength", 40);

    public static void main(String[] args) throws InterruptedException {
        System.out.printf("Compact Strings demo — CompactStrings=%s%n", compactStringsFlag());
        System.out.printf("Allocating %,d ASCII strings of length %d, retained in a List...%n%n",
                STRING_COUNT, STRING_LENGTH);

        long before = usedHeapAfterGc();

        List<String> strings = new ArrayList<>(STRING_COUNT);
        for (int i = 0; i < STRING_COUNT; i++) {
            strings.add(buildAsciiString(i, STRING_LENGTH));
        }

        long after = usedHeapAfterGc();
        long delta = after - before;

        System.out.printf("Heap used before: %s%n", humanBytes(before));
        System.out.printf("Heap used after:  %s%n", humanBytes(after));
        System.out.printf("Delta:            %s for %,d strings%n", humanBytes(delta), STRING_COUNT);
        System.out.printf("Average per string: %.1f bytes (content length %d chars)%n",
                (double) delta / STRING_COUNT, STRING_LENGTH);

        // Keep the list reachable until after measurement so it can't be
        // collected before we read heap usage.
        System.out.println("(retained " + strings.size() + " strings)");
    }

    /** Deterministic, unique, pure-ASCII content — no interning, no surprises. */
    private static String buildAsciiString(int index, int length) {
        StringBuilder sb = new StringBuilder(length);
        sb.append(index);
        while (sb.length() < length) {
            sb.append((char) ('a' + (sb.length() % 26)));
        }
        return sb.substring(0, length);
    }

    private static long usedHeapAfterGc() throws InterruptedException {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        for (int i = 0; i < 3; i++) {
            System.gc();
            Thread.sleep(200);
        }
        return memoryMXBean.getHeapMemoryUsage().getUsed();
    }

    private static String humanBytes(long bytes) {
        double mb = bytes / (1024.0 * 1024.0);
        return String.format("%.2f MB (%,d bytes)", mb, bytes);
    }

    private static String compactStringsFlag() {
        try {
            HotSpotDiagnosticMXBean bean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
            return bean.getVMOption("CompactStrings").getValue();
        } catch (Exception e) {
            return "unknown (" + e.getMessage() + ")";
        }
    }
}
