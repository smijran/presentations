package com.konrad.structured.examples.party;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class ScopedValueHttpServer {

    private static final ScopedValue<String> CLIENT_IP = ScopedValue.newInstance();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/", new GetMethodHandler(new IpProviderHandler(new DoSomethingHandler())));
        server.start();

        System.out.println("Server started on port 8080");
    }


    static class DoSomethingHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {

            String clientIp = CLIENT_IP.get();

            String response = "Hello, your IP is: " + clientIp;

            // Send the response
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    static class IpProviderHandler implements HttpHandler {
        private final HttpHandler delegate;

        IpProviderHandler(HttpHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
            ScopedValue.runWhere(CLIENT_IP, clientIp,
                    () -> {
                        try {
                            delegate.handle(exchange);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    static class GetMethodHandler implements HttpHandler {
        private final HttpHandler delegate;

        GetMethodHandler(HttpHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Check if the request method is GET
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                delegate.handle(exchange);
            } else {
                // Send a 405 Method Not Allowed response for other methods
                exchange.sendResponseHeaders(405, -1);
            }

        }
    }
}
