package com.konrad.structured.examples.party;

import com.japplis.virtually.scope.EnhancedTaskScope;

import java.util.Set;
import java.util.concurrent.Callable;

public class WeEatPizza {

    private final static Set<String> NAMES =
            Set.of("Anna", "Piotr", "Katarzyna", "Marek", "Agnieszka", "Tomasz", "Ewa", "Paweł", "Zofia", "Andrzej", "Magdalena", "Wojciech", "Maria", "Jan", "Małgorzata", "Krzysztof", "Joanna", "Łukasz", "Dorota", "Michał");

    public static void main(String[] args) throws InterruptedException {
        long start = System.currentTimeMillis();
        try (EnhancedTaskScope<Void> pizzaScope = new EnhancedTaskScope<>()){
            pizzaScope.setMaxConcurrentTasks(8);

            NAMES.stream()
                    .map(WeEatPizza::eat)
                    .forEach(c -> pizzaScope.fork(c));

            System.out.println("I am unblocked");
            pizzaScope.join();
        }
        System.out.println(System.currentTimeMillis() - start);
    }

    private static Callable<Void> eat(String name) {
        return () -> {
            try {
                System.out.printf("%s starts pizza\n", name);
                Thread.sleep(1000);
                System.out.printf("%s eats pizza\n", name);
            } catch (InterruptedException iex) {

            }
            return null;
        };
    }
}
