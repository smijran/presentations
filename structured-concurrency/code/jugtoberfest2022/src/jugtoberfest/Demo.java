package jugtoberfest;

import java.util.BitSet;
import java.util.DoubleSummaryStatistics;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class Demo {

    static class Client {
        private final Server server;
        private final int index;

        Client(Server server, int index) {
            this.server = server;
            this.index = index;
        }

        void sendMessage() {
            server.sendMessage(index);
        }
    }

    static class Server implements Runnable {
        private final BitSet clientsCheckout;

        public Server(int noOfClients) {
            clientsCheckout = new BitSet(noOfClients);
            clientsCheckout.set(0, noOfClients);
        }

        public void sendMessage(int index) {
            synchronized (clientsCheckout) {
                clientsCheckout.clear(index);
                clientsCheckout.notifyAll();
            }
        }

        public void run() {
            while (true) {
                synchronized (clientsCheckout) {
                    try {
                        clientsCheckout.wait();
                        if (clientsCheckout.isEmpty()) {
                            return;
                        }
                    } catch (InterruptedException exception) {
                        exception.printStackTrace();
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        DoubleSummaryStatistics statistics =
                LongStream.generate(Demo::experiment)
                        .limit(3)
                        .peek(time -> System.out.printf("Run %d ms\n", time))
                        .asDoubleStream()
                        .summaryStatistics();
        System.out.println(statistics);
    }

    private static long experiment() {
        final int NO_OF_CLIENTS = 7_500;
        Server server = new Server(NO_OF_CLIENTS);
        Set<Client> clients = IntStream.range(0, NO_OF_CLIENTS).mapToObj(i -> new Client(server, i)).collect(Collectors.toSet());
//        ThreadFactory factory = Thread.ofPlatform().name("Platform").factory();
        ThreadFactory factory = Thread.ofVirtual().name("Virtual").factory();

        long start = System.currentTimeMillis();
        try {
            Thread serverThread = factory.newThread(server);
            serverThread.start();
            CyclicBarrier cyclicBarrier = new CyclicBarrier(NO_OF_CLIENTS);
            clients.stream()
                    .map(client -> (Runnable) () -> {
                        try {
                            cyclicBarrier.await();
                            client.sendMessage();
                        } catch (InterruptedException | BrokenBarrierException exception) {
                            exception.printStackTrace();
                        }
                    })
                    .map(factory::newThread)
                    .forEach(Thread::start);
            serverThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long stop = System.currentTimeMillis();

        return stop - start;
    }
}
