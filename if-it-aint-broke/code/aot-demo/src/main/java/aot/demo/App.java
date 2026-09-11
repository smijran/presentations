package aot.demo;

import com.sun.net.httpserver.HttpServer;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

/**
 * A minimal "web app" (JDK-builtin HttpServer, zero external dependencies)
 * that starts, serves exactly one request against itself, and reports
 * "time to first request" measured from the OS-level process start time
 * (not from main() -- that would exclude JVM bootstrap, which is exactly
 * what AOT class loading/linking (JEP 483) and training (JEP 514/515)
 * target) to the moment the first HTTP response is received.
 *
 * Run cold, then after an AOT cache training run + reuse
 * (-XX:AOTCacheOutput / -XX:AOTCache, JEP 514's one-step ergonomics) to
 * compare cold-start vs AOT-assisted time-to-first-request.
 */
public class App {

    public static void main(String[] args) throws Exception {
        Instant jvmStart = ProcessHandle.current().info().startInstant().orElse(Instant.now());

        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = "ok".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        int port = server.getAddress().getPort();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/")).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Instant firstResponseAt = Instant.now();

        long timeToFirstRequestMs = Duration.between(jvmStart, firstResponseAt).toMillis();

        System.out.printf("First response body=%s status=%d%n", response.body(), response.statusCode());
        System.out.println("TIME TO FIRST REQUEST: " + timeToFirstRequestMs + " ms");

        server.stop(0);
    }
}
