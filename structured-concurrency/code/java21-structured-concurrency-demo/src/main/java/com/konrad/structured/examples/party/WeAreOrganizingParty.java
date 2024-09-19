package com.konrad.structured.examples.party;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WeAreOrganizingParty {

    private static final Set<String> NAMES = Set.of("Wojtek", "Michał", "Max", "Maciej", "Konrad");

    private static final Set<ThingToDo> THINGS_TO_DO = Set.of(
            new ThingToDo("buying chips"),
            new ThingToDo("buying drinks"),
            new ThingToDo("setting up PlayStation"),
            new ThingToDo("buying sausages"),
            new ThingToDo("lighting the barbecue")
    );

    private static Boolean doSomething(String name, ThingToDo toDo) {
        arrangeThings();
        if (haveFailed()) {
            throw new RuntimeException(name + " failed!! " + name + " is not " + toDo.thing());
        }
        System.out.println(name + " is " + toDo.thing());
        return true;
    }

    public static void main(String[] ignoredArgs) throws InterruptedException {
        while (true) {
            Map<String, ThingToDo> tasksPicked = assignThingsToDo();

            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                tasksPicked
                        .forEach((key, value) -> scope.fork(() -> doSomething(key, value)));

                scope.join();
                scope.throwIfFailed();
            } catch (ExecutionException e) {
                System.out.println("Party has failed.... Because " + e.getMessage());
                break;
            }
            System.out.println("Lets get party started!!!");
            System.out.println("--------------");
        }
    }

    private static Map<String, ThingToDo> assignThingsToDo() {
        final LinkedList<ThingToDo> randomized = new LinkedList<>(THINGS_TO_DO);
        Collections.shuffle(randomized);
        return NAMES.stream()
                .collect(Collectors.toMap(Function.identity(), _ -> randomized.pop()));
    }

    private static boolean haveFailed() {
        return Math.random() < 0.1;
    }

    private static void arrangeThings() {
        try {
            Thread.sleep(1000 + (int) (Math.random() * 2000));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

record ThingToDo(String thing) {
}
