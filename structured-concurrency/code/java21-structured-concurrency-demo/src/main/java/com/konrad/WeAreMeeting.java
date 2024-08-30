package com.konrad;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Collectors;

public class WeAreMeeting {

    record MeetingTime(DayOfWeek dayOfWeek, LocalTime time) {
    }

    private static final Set<String> NAMES = Set.of("Wojtek", "Michał", "Max", "Maciej", "Konrad");

    private static final Set<MeetingTime> POSSIBILITIES = Set.of(
            new MeetingTime(DayOfWeek.FRIDAY, LocalTime.of(20, 0, 0)),
            new MeetingTime(DayOfWeek.WEDNESDAY, LocalTime.of(21, 0, 0)),
            new MeetingTime(DayOfWeek.MONDAY, LocalTime.of(20, 0, 0)));

    public static MeetingTime chooseTime(String name, Set<MeetingTime> possibilities) {
        try {
            int thinkingTime = (int) (Math.random() * 5000);
            System.out.printf("%s searches in calendar for %d ms %n", name, thinkingTime);
            Thread.sleep(thinkingTime);
            int pick = (int) (Math.random() * possibilities.size());
            System.out.printf("%s found a time slot! %n", name);
            return List.copyOf(possibilities).get(pick);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        int rounds = 0;
        while (true) {
            System.out.printf("Round %d %n", rounds++);
            try (var scope = new StructuredTaskScope<MeetingTime>()) {
                Set<StructuredTaskScope.Subtask<MeetingTime>> tasks =
                        NAMES.stream()
                                .map(name -> scope.fork(() -> chooseTime(name, POSSIBILITIES)))
                                .collect(Collectors.toUnmodifiableSet());
                scope.join();
                Set<MeetingTime> meetingTimes =
                        tasks
                                .stream()
                                .map(StructuredTaskScope.Subtask::get)
                                .collect(Collectors.toUnmodifiableSet());
                System.out.println(meetingTimes);
                if (meetingTimes.size() == 1){
                    System.out.println("Meeting time chosen. Ending.");
                    System.exit(0);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
