package com.konrad.structured.examples.party;

import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

@Log4j2
public class WeArePaintingRooms {
    enum Paint {
        BLUE, GREEN, RED, YELLOW, WHITE, BLACK
    }

    private final static Random RANDOM = new Random();

    private final static ScopedValue<Paint> PAINT = ScopedValue.newInstance();

    public static void main(String[] ignoredArgs) {
        List<String> address = List.of("1");
        ScopedValue.where(PAINT, Paint.WHITE)
                .run(() -> paintRoomStep(address));
    }

    private static void paintRoomStep(List<String> address) {
        if (shallWeEnd(address)) return;

        paintRoom(address);

        if (shouldWeChangePaint()) {
            ScopedValue
                    .runWhere(PAINT, pickRandomPaint(),
                            () -> {
                                printPaintChange(address);
                                paintRoomsDownlane(address);
                            });
        } else {
            paintRoomsDownlane(address);
        }
    }

    private static void paintRoomsDownlane(List<String> address) {
        paintRoomStep(append(address, "1"));
        paintRoomStep(append(address, "2"));
    }

    private static void printPaintChange(List<String> address) {
        log.info("{} --> {}", String.format("%-10s", String.join("-", address)), PAINT.get());
    }

    private static boolean shouldWeChangePaint() {
        return RANDOM.nextDouble() > 0.7;
    }

    private static boolean shallWeEnd(List<String> address) {
        return address.size() == 5;
    }


    private static List<String> append(List<String> list, String toAppend) {
        return Stream.concat(
                list.stream(),
                Stream.of(toAppend)
        ).toList();
    }


    private static void paintRoom(List<String> roomAddress) {
        log.info("{} painted {}" , String.format("%-10s", String.join("-", roomAddress)), PAINT.get());
    }


    private static Paint pickRandomPaint() {
        Paint paint = Paint.values()[RANDOM.nextInt(Paint.values().length)];
        while (paint == PAINT.get()) {
            paint = Paint.values()[RANDOM.nextInt(Paint.values().length)];
        }
        return paint;
    }
}
