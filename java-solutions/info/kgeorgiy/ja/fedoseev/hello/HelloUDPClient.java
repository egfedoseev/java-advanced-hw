package info.kgeorgiy.ja.fedoseev.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

public class HelloUDPClient implements HelloClient {
    private static final int MAX_TRIES = Integer.MAX_VALUE;
    private static final int TIMEOUT = 1000;
    private static final int RESPONSE_SIZE = 1024;

    @Override
    public void run(String host, int port, String prefix, int requests, int threads) {
        SocketAddress socketAddress;
        try {
            socketAddress = new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (UnknownHostException e) {
            System.err.println("Unknown host " + host + ": " + e.getMessage());
            return;
        }


        try (ExecutorService sendExecutor = Executors.newFixedThreadPool(threads)) {
            List<Future<?>> futures = IntStream.iterate(1, idx -> idx + 1).limit(threads)
                    .<Future<?>>mapToObj(idx -> sendExecutor.submit(() -> {
                        sendRequests(prefix, requests, idx, socketAddress);
                    })).toList();
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (ExecutionException e) {
            System.err.println("Exception in worker thread: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void sendRequests(String prefix, int requests, int idx, SocketAddress socketAddress) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT);

            byte[] responseBuff = new byte[RESPONSE_SIZE];
            DatagramPacket responsePacket = new DatagramPacket(responseBuff, responseBuff.length);

            for (int i = 1; i <= requests; ++i) {
                String suffix = i + "_" + idx;
                String message = prefix + suffix;
                byte[] requestBuff = message.getBytes(StandardCharsets.UTF_8);
                DatagramPacket requestPacket = new DatagramPacket(requestBuff, requestBuff.length, socketAddress);

                for (int tries = 0; tries < MAX_TRIES; ++tries)
                    try {
                        socket.send(requestPacket);
                        socket.receive(responsePacket);
                        String responseMessage = new String(
                                responsePacket.getData(),
                                responsePacket.getOffset(),
                                responsePacket.getLength(),
                                StandardCharsets.UTF_8);
                        if (responseMessage.endsWith(suffix)) {
                            System.out.println(message + " " + responseMessage);
                            break;
                        }
                    } catch (IOException _) {
                    } finally {
                        responsePacket.setLength(responseBuff.length);
                    }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    static void main(String[] args) {
        if (args.length != 5) {
            System.err.println("Usage: host port prefix requests threads");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String prefix = args[2];
        int requests = Integer.parseInt(args[3]);
        int threads = Integer.parseInt(args[4]);

        HelloUDPClient client = new HelloUDPClient();
        client.run(host, port, prefix, requests, threads);
    }
}
