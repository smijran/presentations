package com.konrad.structured.examples.party;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WeArePaintingRooms {
    enum Paint {
        BLUE, GREEN, RED, YELLOW, WHITE, BLACK
    }

    private final static Random RANDOM = new Random();

    private final static ScopedValue<Paint> PAINT = ScopedValue.newInstance();

    public static void main(String[] args) {
        List<String> address = List.of("1");
        ScopedValue.where(PAINT, Paint.WHITE)
                .run(() -> paintRoomStep(address));
    }

    private static void paintRoomStep(List<String> address) {
        if (shallWeEnd(address)) return;
        paintRoom(address);
        if (shouldWeChangePaint()) {
            ScopedValue
                    .where(PAINT, pickRandomPaint())
                    .run(() -> {
                        printPaintChange(address);
                        paintRoomsDownlane(address);
                    });
        } else {
            paintRoomsDownlane(address);
        }
    }

    private static void printPaintChange(List<String> address) {
        System.out.println("Paint CHANGE at " + String.join("-", address) + " " + PAINT.get());
    }

    private static boolean shouldWeChangePaint() {
        return RANDOM.nextDouble() > 0.7;
    }

    private static boolean shallWeEnd(List<String> address) {
        if (address.size() == 5) {
            return true;
        }
        return false;
    }

    private static void paintRoomsDownlane(List<String> address) {
        try (StructuredTaskScope<Void> taskScope = new StructuredTaskScope<>()) {
            taskScope.fork(asCallable(() -> paintRoomStep(append(address, "1"))));
            taskScope.fork(asCallable(() -> paintRoomStep(append(address, "2"))));
            try {
                taskScope.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static Callable<Void> asCallable(Runnable runnable) {
        return () ->
        {
            runnable.run();
            return null;
        };
    }

    private static List<String> append(List<String> list, String toAppend) {
        return Stream.concat(
                list.stream(),
                Stream.of(toAppend)
        ).toList();
    }


    private static void paintRoom(List<String> roomAddress) {
        System.out.println(String.join("-", roomAddress) +
                " painted " + PAINT.get());
    }


    private static Paint pickRandomPaint() {
        Paint paint =  Paint.values()[RANDOM.nextInt(Paint.values().length)];
        while (paint == PAINT.get()) {
            paint = Paint.values()[RANDOM.nextInt(Paint.values().length)];
        }
        return paint;
    }
}
