package cds.demo;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Serializes/deserializes a modest object graph through Jackson -- enough
 * real framework class-loading to give CDS something meaningful to share,
 * unlike a toy app with a handful of classes -- then holds the process
 * resident for a few seconds so an external script (compare-cds.sh) can
 * sample its memory footprint while several instances run concurrently.
 */
public class App {

    private static final int SLEEP_SECONDS = Integer.getInteger("demo.sleepSeconds", 5);

    public record Address(String street, String city, String zip) {
    }

    public record Person(String name, int age, List<Address> addresses, Map<String, String> attributes) {
    }

    public static void main(String[] args) throws Exception {
        long start = System.nanoTime();

        ObjectMapper mapper = new ObjectMapper();
        List<Person> people = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            people.add(new Person(
                    "Person" + i,
                    20 + (i % 50),
                    List.of(new Address("Street " + i, "City " + (i % 10), "00-" + (i % 1000))),
                    Map.of("id", String.valueOf(i))));
        }

        String json = mapper.writeValueAsString(people);
        List<Person> roundTripped = mapper.readValue(json,
                mapper.getTypeFactory().constructCollectionType(List.class, Person.class));

        long startupNanos = System.nanoTime() - start;

        System.out.printf("PID %d up in %.1f ms, round-tripped %d people through Jackson (%d bytes JSON)%n",
                ProcessHandle.current().pid(), startupNanos / 1e6, roundTripped.size(), json.length());

        if (SLEEP_SECONDS > 0) {
            System.out.println("Sleeping " + SLEEP_SECONDS + "s so memory can be sampled externally...");
            Thread.sleep(SLEEP_SECONDS * 1000L);
        }
        System.out.println("Done.");
    }
}
