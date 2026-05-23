package geecon2023.example.vector;

import org.apache.commons.lang.time.StopWatch;

import java.util.Random;
import java.util.function.Supplier;

public final class Util {
    private Util() {
    }

    public static int[] randomIntArray(int length, int bound) {
        return new Random().ints(0, bound).limit(length).toArray();
    }

    public static double[] randomDoubleArray(int length, double bound) {
        return new Random().doubles().map(x -> bound * x).limit(length).toArray();     
    }

    public static <T> T runAndTime(String message, Supplier<T> operation, int repetitions) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        T result = null;
        for(int i = 0; i < repetitions; i++) {
          result = operation.get();
        }
        stopWatch.stop();
        System.out.println(message + ":" + result);
        System.out.println(((double)stopWatch.getTime()) / repetitions + " ms");
        return result;
    }
}
