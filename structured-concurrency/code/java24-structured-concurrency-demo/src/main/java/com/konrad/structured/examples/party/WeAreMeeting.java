package com.konrad.structured.examples.party;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Collectors;

import static com.konrad.structured.examples.party.DemoThreadFactory.VT_THREAD_FACTORY;

public class WeAreMeeting {

    private static final Logger log = LogManager.getLogger(WeAreMeeting.class);

    private static final Set<String> NAMES =
            Set.of("Wojtek", "Michał", "Max", "Maciej", "Konrad");

    private static final Set<MeetingTime> POSSIBILITIES = Set.of(
            new MeetingTime(DayOfWeek.FRIDAY, LocalTime.of(20, 0, 0)),
            new MeetingTime(DayOfWeek.WEDNESDAY, LocalTime.of(21, 0, 0)),
            new MeetingTime(DayOfWeek.MONDAY, LocalTime.of(20, 0, 0)));

    private static MeetingTime chooseTime(String name) {
        think();
        MeetingTime meetingTime = pick(POSSIBILITIES);
        log.info("{} found a time slot : {} !", name, meetingTime);
        return meetingTime;
    }

    void main() throws InterruptedException {
        while (true) {
            try (var scope = new StructuredTaskScope<MeetingTime>("MeetingChase", VT_THREAD_FACTORY)) {

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
                                .filter(t -> t.state() == StructuredTaskScope.Subtask.State.SUCCESS)
                                .map(StructuredTaskScope.Subtask::get)
                                .collect(Collectors.toUnmodifiableSet());

                log.info(meetingTimes);
                log.info("-----");
                if (meetingTimes.size() == 1) {
                    break;
                }
            }
        }
        log.info("Meeting time chosen. Ending.");
    }

    private static void think() {
        try {
            int thinkingTime = (int) (Math.random() * 3000);
            Thread.sleep(thinkingTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T pick(Set<T> set) {
        return List.copyOf(set).get(new Random().nextInt(set.size()));
    }
}

record MeetingTime(DayOfWeek dayOfWeek, LocalTime time) {
    @Override
    public String toString() {
        return dayOfWeek + ":" + time;
    }
}