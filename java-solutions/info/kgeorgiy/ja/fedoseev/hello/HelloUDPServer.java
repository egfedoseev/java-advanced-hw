package info.kgeorgiy.ja.fedoseev.hello;

import info.kgeorgiy.java.advanced.hello.NewHelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class HelloUDPServer implements NewHelloServer {
    private static final int REQUEST_SIZE = 1024;
    private static final int QUEUE_CAPACITY = 1000;

    private ExecutorService workerExecutor;

    private final Thread.Builder.OfVirtual receiverBuilder = Thread.ofVirtual().name("receiver-", 0);
    private final List<Thread> receivers = new ArrayList<>();
    private final List<DatagramSocket> sockets = new ArrayList<>();

    private volatile boolean isRunning = false;
    private volatile boolean isClosed = false;

    private void runReceiver(DatagramSocket socket, String format) {
        while (!Thread.currentThread().isInterrupted()) {
            byte[] requestBuff = new byte[REQUEST_SIZE];
            DatagramPacket requestPacket = new DatagramPacket(requestBuff, requestBuff.length);
            try {
                socket.receive(requestPacket);
            } catch (IOException e) {
                if (socket.isClosed() || Thread.currentThread().isInterrupted()) {
                    break;
                }
                continue;
            }
            workerExecutor.execute(() -> processPacket(socket, format, requestPacket));
        }
    }

    private void processPacket(DatagramSocket socket, String format, DatagramPacket requestPacket) {
        String requestMessage = new String(
                requestPacket.getData(),
                requestPacket.getOffset(),
                requestPacket.getLength(),
                StandardCharsets.UTF_8);
        StringBuilder localizedRequestMessage = new StringBuilder();
        for (int i = 0; i < requestMessage.length(); ++i) {
            char c = requestMessage.charAt(i);
            if (Character.isDigit(c)) {
                localizedRequestMessage.append(Character.digit(c, 10));
            } else {
                localizedRequestMessage.append(c);
            }
        }
        requestMessage = localizedRequestMessage.toString();
        String responseMessage = format.replace("%%", requestMessage);


        byte[] responseBuff = responseMessage.getBytes(StandardCharsets.UTF_8);
        DatagramPacket responsePacket = new DatagramPacket(responseBuff, responseBuff.length, requestPacket.getSocketAddress());
        try {
            socket.send(responsePacket);
        } catch (IOException _) {
        }
    }

    @Override
    public void start(int threads, Map<Integer, String> ports) {
        if (isRunning) {
            throw new IllegalStateException("Server is already running");
        }
        if (isClosed) {
            throw new IllegalStateException("Server is closed");
        }
        isRunning = true;
        workerExecutor = new ThreadPoolExecutor(
                threads, threads,
                Long.MAX_VALUE, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadPoolExecutor.DiscardPolicy());
        for (Map.Entry<Integer, String> entry : ports.entrySet()) {
            try {
                DatagramSocket socket = new DatagramSocket(entry.getKey());
                sockets.add(socket);
                receivers.add(receiverBuilder.start(() -> runReceiver(socket, entry.getValue())));
            } catch (SocketException e) {
                System.err.println("Can't open socket: " + e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        isRunning = false;
        isClosed = true;
        receivers.forEach(Thread::interrupt);
        sockets.forEach(DatagramSocket::close);
        workerExecutor.close();
    }

    static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: port threads");
            return;
        }

        int port = Integer.parseInt(args[0]);
        int threads = Integer.parseInt(args[1]);

        HelloUDPServer server = new HelloUDPServer();
        server.start(port, threads);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("Signal received");
            server.close();
        }));

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.err.println("Main thread interrupted");
        }
    }
}
