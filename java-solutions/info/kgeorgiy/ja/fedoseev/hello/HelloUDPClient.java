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
    private static final int TIMEOUT = 300;
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
                    .<Future<?>>mapToObj(idx -> sendExecutor.submit(() -> sendRequests(prefix, requests, idx, socketAddress)))
                    .toList();
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (ExecutionException e) {
            System.err.println("Exception in worker thread: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean endsWith(String s, int end, String suffix) {
        // 1. Проверяем, достаточно ли места в строке
        if (end < suffix.length()) {
            return false;
        }
        for (int i = 0; i < suffix.length(); ++i) {
            char charFromS = s.charAt(end - suffix.length() + i);
            char charFromSuffix = suffix.charAt(i);

            int digitS = Character.digit(charFromS, 10);
            int digitSuffix = Character.digit(charFromSuffix, 10);

            if (digitS == -1 || digitS != digitSuffix) {
                return false;
            }
        }

        int indexBeforeSuffix = end - suffix.length() - 1;
        return indexBeforeSuffix < 0 || !Character.isDigit(s.charAt(indexBeforeSuffix));
    }

    private static boolean startsWith(String s, int begin, String prefix) {
        boolean res = begin + prefix.length() <= s.length();
        for (int i = begin; i < begin + prefix.length() && res; ++i) {
            if (Character.digit(s.charAt(i), 10) != Character.digit(prefix.charAt(i - begin), 10)) {
                res = false;
            }
        }
        return res;
    }

    private static void sendRequests(String prefix, int requests, int thread, SocketAddress socketAddress) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT);

            byte[] responseBuff = new byte[RESPONSE_SIZE];
            DatagramPacket responsePacket = new DatagramPacket(responseBuff, responseBuff.length);

            for (int i = 1; i <= requests; ++i) {
                String suffix = i + "_" + thread;
                String message = prefix + suffix;
                byte[] requestBuff = message.getBytes(StandardCharsets.UTF_8);
                DatagramPacket requestPacket = new DatagramPacket(requestBuff, requestBuff.length, socketAddress);

                for (int tries = 0; tries < MAX_TRIES; ++tries)
                    try {
                        if (sendRequest(socket, requestPacket, responsePacket, message, i, thread)) {
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

    private static boolean sendRequest(DatagramSocket socket,
                                       DatagramPacket requestPacket,
                                       DatagramPacket responsePacket,
                                       String message,
                                       int i, int thread) throws IOException {
        socket.send(requestPacket);
        socket.receive(responsePacket);
        String responseMessage = new String(
                responsePacket.getData(),
                responsePacket.getOffset(),
                responsePacket.getLength(),
                StandardCharsets.UTF_8);

        boolean isMine = false;
        String first = Integer.toString(i);
        String second = Integer.toString(thread);
        for (int j = first.length(); j < responseMessage.length() - second.length(); ++j) {
            if (responseMessage.charAt(j) != '_') {
                continue;
            }
            if (endsWith(responseMessage, j, first) && startsWith(responseMessage, j + 1, second)) {
                isMine = true;
            }
        }

        if (isMine) {
            System.out.println(message + " " + responseMessage);
            return true;
        }
        return false;
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
