package com.konrad;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Collectors;

public class WeArePlayingCS {


    private static final Set<String> NAMES = Set.of("Wojtek", "Michał", "Max", "Maciej", "Konrad");

    public static class Player implements Callable<String> {
        private final String name;
        private final List<String> inGame;

        public Player(String name, List<String> inGame) {
            this.name = name;
            this.inGame = inGame;
        }

        @Override
        public String call() throws Exception {
            try {
                while (true) {
                    int huntTime = (int) (1000 + Math.random() * 2000);
                    Thread.sleep(huntTime);
                    if (inGame.contains(name)) {
                        String victim = huntSomeone(name, inGame);
                        if (victim == null) {
                            return name;
                        }
                        System.out.println(name + " fragged " + victim);
                        if (Math.random() < 0.1) {
                            throw new WifeCallingException();
                        }
                    } else if (inGame.size() == 1) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return name;
        }
    }


    public static String huntSomeone(String name, List<String> inGame) {
        while (true) {
            if (inGame.size() == 1) {
                return null;
            }
            final String victim = inGame.remove((int) (Math.random() * inGame.size()));
            if (victim.equals(name)) {
                inGame.add(victim);
            } else {
                return victim;
            }
        }
    }

    public static void main(String[] args) {
        int rounds = 0;
        while (true) {
            System.out.printf("Round %d %n", rounds++);
            List<String> inGame = new CopyOnWriteArrayList<>(NAMES);
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                Set<StructuredTaskScope.Subtask<String>> tasks =
                        NAMES.stream()
                                .map(name -> new Player(name, inGame))
                                .map(scope::fork)
                                .collect(Collectors.toUnmodifiableSet());
                scope.join();
                tasks.stream()
                        .filter(t -> t.state() == StructuredTaskScope.Subtask.State.FAILED)
                        .map(StructuredTaskScope.Subtask::task)
                        .filter(Player.class::isInstance)
                        .map(Player.class::cast)
                        .forEach(p -> System.out.println(p.name + " WIFE CALLINNNGGG!!!"));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class WifeCallingException extends RuntimeException {
    }
}
