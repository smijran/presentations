package vector.demo;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Dot product of two large float arrays — a scalar loop vs an explicit
 * jdk.incubator.vector version of the same algorithm.
 *
 * A JIT warm-up phase runs before timing starts: an "unwarmed" run mostly
 * measures the interpreter/C1, not the steady-state throughput a real
 * long-running service would see.
 *
 * Requires --add-modules jdk.incubator.vector at compile and run time
 * (configured in build.gradle) — the Vector API is still incubating as of
 * Java 26/27 (12 incubator rounds and counting; see the talk's Value Types
 * section for why it's stuck there).
 */
public class App {

    private static final int ARRAY_LENGTH = Integer.getInteger("demo.arrayLength", 50_000_000);
    private static final int WARMUP_ITERATIONS = Integer.getInteger("demo.warmupIterations", 10);
    private static final int MEASURED_ITERATIONS = Integer.getInteger("demo.measuredIterations", 20);

    private static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;

    public static void main(String[] args) {
        System.out.printf("Vector API demo — dot product of two float[%,d] arrays%n", ARRAY_LENGTH);
        System.out.printf("Preferred species: %s (%d lanes per vector)%n%n", SPECIES, SPECIES.length());

        float[] a = randomArray(ARRAY_LENGTH);
        float[] b = randomArray(ARRAY_LENGTH);

        System.out.printf("Warming up JIT (%d iterations each)...%n", WARMUP_ITERATIONS);
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            dotProductScalar(a, b);
            dotProductVector(a, b);
        }

        System.out.printf("Measuring (%d iterations each)...%n%n", MEASURED_ITERATIONS);

        float scalarResult = 0;
        long scalarNanos = 0;
        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
            long start = System.nanoTime();
            scalarResult = dotProductScalar(a, b);
            scalarNanos += System.nanoTime() - start;
        }

        float vectorResult = 0;
        long vectorNanos = 0;
        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
            long start = System.nanoTime();
            vectorResult = dotProductVector(a, b);
            vectorNanos += System.nanoTime() - start;
        }

        double scalarAvgMs = scalarNanos / 1e6 / MEASURED_ITERATIONS;
        double vectorAvgMs = vectorNanos / 1e6 / MEASURED_ITERATIONS;

        System.out.printf("Scalar loop:  %8.2f ms/iteration  (result=%.3f)%n", scalarAvgMs, scalarResult);
        System.out.printf("Vector API:   %8.2f ms/iteration  (result=%.3f)%n", vectorAvgMs, vectorResult);
        System.out.printf("Speedup:      %8.2fx%n", scalarAvgMs / vectorAvgMs);

        double relativeDiffPct = 100.0 * Math.abs(scalarResult - vectorResult) / Math.abs(scalarResult);
        System.out.printf("Relative difference between results: %.6f%% (floating-point summation order differs)%n",
                relativeDiffPct);
    }

    private static float dotProductScalar(float[] a, float[] b) {
        float sum = 0f;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    private static float dotProductVector(float[] a, float[] b) {
        int length = a.length;
        int upperBound = SPECIES.loopBound(length);
        FloatVector sumVector = FloatVector.zero(SPECIES);

        int i = 0;
        for (; i < upperBound; i += SPECIES.length()) {
            FloatVector va = FloatVector.fromArray(SPECIES, a, i);
            FloatVector vb = FloatVector.fromArray(SPECIES, b, i);
            sumVector = va.fma(vb, sumVector); // sumVector += va * vb, one fused instruction per lane
        }
        float sum = sumVector.reduceLanes(VectorOperators.ADD);

        // Scalar tail for any remainder not divisible by the vector length.
        for (; i < length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    private static float[] randomArray(int length) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        float[] arr = new float[length];
        for (int i = 0; i < length; i++) {
            arr[i] = rnd.nextFloat() * 2f - 1f;
        }
        return arr;
    }
}
