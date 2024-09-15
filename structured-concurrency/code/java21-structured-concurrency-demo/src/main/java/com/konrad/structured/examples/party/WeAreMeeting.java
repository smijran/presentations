package com.konrad.structured.examples.party;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Collectors;

public class WeAreMeeting {

    record MeetingTime(DayOfWeek dayOfWeek, LocalTime time) {
        @Override
        public String toString() {
            return dayOfWeek + ":" + time;
        }
    }

    private static final Set<String> NAMES = Set.of("Wojtek", "Michał", "Max", "Maciej", "Konrad");

    private static final Set<MeetingTime> POSSIBILITIES = Set.of(
            new MeetingTime(DayOfWeek.FRIDAY, LocalTime.of(20, 0, 0)),
            new MeetingTime(DayOfWeek.WEDNESDAY, LocalTime.of(21, 0, 0)),
            new MeetingTime(DayOfWeek.MONDAY, LocalTime.of(20, 0, 0)));

    private static MeetingTime chooseTime(String name) {
        think();
        int pick = (int) (Math.random() * POSSIBILITIES.size());
        System.out.printf("%s found a time slot! %n", name);
        return List.copyOf(POSSIBILITIES).get(pick);

    }

    public static void main(String[] args) {
        int rounds = 0;
        while (true) {
            System.out.printf("Round %d %n", rounds++);

            try (var scope = new StructuredTaskScope<MeetingTime>()) {

                var tasks =
                        NAMES.stream()
                                .map(name -> scope.fork(() -> chooseTime(name)))
                                .collect(Collectors.toUnmodifiableSet());

                // Wait for everyone to finish
                scope.join();

                // Check all the times chosen
                Set<MeetingTime> meetingTimes =
                        tasks
                                .stream()
                                .map(StructuredTaskScope.Subtask::get)
                                .collect(Collectors.toUnmodifiableSet());

                System.out.println(meetingTimes);
                System.out.println("--------------------------");
                if (meetingTimes.size() == 1) {
                    System.out.println("Meeting time chosen. Ending.");
                    System.exit(0);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void think() {
        try {
            int thinkingTime = (int) (Math.random() * 3000);
            Thread.sleep(thinkingTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
