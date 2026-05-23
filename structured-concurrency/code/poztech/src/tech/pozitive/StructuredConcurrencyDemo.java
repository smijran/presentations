package tech.pozitive;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StructuredConcurrencyDemo {
    public static void main(String[] args) {
        System.out.println("As");
        try(ExecutorService executorService = Executors.newVirtualThreadExecutor()) {
            executorService.submit(() -> System.out.println("B"));
            executorService.submit(() -> System.out.println("C"));
            executorService.submit(() -> {
                System.out.println("Ds");
                try (ExecutorService executorService2 = Executors.newVirtualThreadExecutor()) {
                    executorService2.submit(() -> System.out.println("E"));
                    executorService2.submit(() -> System.out.println("F"));
                }
                System.out.println("De");
            });
        }

        System.out.println("Ae");
    }
}
