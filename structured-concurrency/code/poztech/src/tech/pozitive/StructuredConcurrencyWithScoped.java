package tech.pozitive;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StructuredConcurrencyWithScoped {
    public static Scoped<String> test = Scoped.forType(String.class);
    public static Scoped<Integer> test2 = Scoped.forType(Integer.class);

    public static void main(String[] args) {
        ScopedBinding sc = test.bind("A");
        ScopedBinding sc2 = test2.bind(12);
        Thread
    }
}
