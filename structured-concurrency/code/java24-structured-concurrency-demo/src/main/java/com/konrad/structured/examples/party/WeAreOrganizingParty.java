package com.konrad.structured.examples.party;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;

import static com.konrad.structured.examples.party.DemoThreadFactory.VT_THREAD_FACTORY;

public class WeAreOrganizingParty {

    private static final Logger log = LogManager.getLogger(WeAreOrganizingParty.class);

    public static void main(String[] ignoredArgs) throws InterruptedException {
        while (true) {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure("PartyType", VT_THREAD_FACTORY)) {
                scope.fork(() -> buyChips("Maciej"));
                scope.fork(() -> buyDrinks("Wojtek"));
                scope.fork(() -> buySausages("Max"));
                scope.fork(() -> setupPlaystation("Michał"));
                scope.fork(() -> lightTheBBQ("Konrad"));

                scope.join();
                scope.throwIfFailed();
            } catch (ExecutionException e) {
                log.error("Party has failed.... Because " + e.getMessage(), e);
                break;
            }
            log.info("Lets get party started!!!");
            log.info("--------------");
        }
    }

    private static boolean buyChips(String name) {
        return doSomething(name, "buying chips");
    }

    private static boolean buyDrinks(String name) {
        return doSomething(name, "buying drinks");
    }

    private static boolean buySausages(String name) {
        return doSomething(name, "buying sausages");
    }

    private static boolean setupPlaystation(String name) {
        return doSomething(name, "setting up PlayStation");
    }

    private static boolean lightTheBBQ(String name) {
        return doSomething(name, "lighting the barbecue");
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

    private static Boolean doSomething(String name, String toDo) {
        arrangeThings();
        if (haveFailed()) {
            throw new RuntimeException(name + " failed!! " + name + " is not " + toDo);
        }
        log.info(name + " is " + toDo);
        return true;
    }
}

record ThingToDo(String thing) {
}
