package jugtoberfest;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StructuredConcurrencyDemo {
    private final static Random random = new Random();

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            experiment();
        }
    }

    private static void experiment() {
        sleepAndPrint("A - s");
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            executorService.submit(() -> sleepAndPrint("B", 1000));
            executorService.submit(() -> sleepAndPrint("C", 1000));
            executorService.submit(() -> {
                sleepAndPrint("D - s");
                try (ExecutorService executorService2 = Executors.newVirtualThreadPerTaskExecutor()) {
                    executorService2.submit(() -> sleepAndPrint("E", 500));
                    executorService2.submit(() -> sleepAndPrint("F",500));
                }
                sleepAndPrint("D - z");
            });
        }
        sleepAndPrint("A - z");
        System.out.println("-----------------");
    }

    private static void sleepAndPrint(String out) {
        sleepAndPrint(out, 100);
    }

    private static void sleepAndPrint(String out, int mod) {
        try {
            Thread.sleep(Math.abs(random.nextInt()) % mod);
            System.out.println("Thread: " + Thread.currentThread().hashCode() + " : " + out);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
