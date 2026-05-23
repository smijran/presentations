package jugtoberfest;

import java.util.concurrent.ThreadFactory;

import static java.lang.System.*;

public class ShowVThreadApi {
    public static void main(String[] args) {
        Thread thread = Thread.startVirtualThread(
                () -> out.println("Run")
        );

        Thread secondThread = Thread.ofVirtual()
                .name("VirtualThread")
                .start(() -> out.println("Run another."));

        ThreadFactory threadFactory =
                Thread.ofVirtual()
                        .uncaughtExceptionHandler((
                                th, ex) -> err.println("Exception " + ex.getMessage()))
                        .factory();
    }
}
