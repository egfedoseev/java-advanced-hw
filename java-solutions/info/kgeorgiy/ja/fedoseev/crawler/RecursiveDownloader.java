package info.kgeorgiy.ja.fedoseev.crawler;

import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.Result;
import info.kgeorgiy.java.advanced.crawler.URLUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class RecursiveDownloader {

    private final ConcurrentHashMap<String, IOException> errors = new ConcurrentHashMap<>();

    private final Downloader downloader;
    private final int perHost;
    private final ExecutorService downloaderExecutor;
    private final ExecutorService extractorExecutor;
    private final ConcurrentMap<String, Semaphore> hostSemaphores;
    private final int maxDepth;
    private final List<String> includes;

    public RecursiveDownloader(Downloader downloader, int perHost, ExecutorService downloaderExecutor, ExecutorService extractorExecutor,
                               ConcurrentMap<String, Semaphore> hostSemaphores, int maxDepth, List<String> includes) {
        this.downloader = downloader;
        this.perHost = perHost;
        this.downloaderExecutor = downloaderExecutor;
        this.extractorExecutor = extractorExecutor;
        this.hostSemaphores = hostSemaphores;
        this.maxDepth = maxDepth;
        this.includes = List.copyOf(includes);
    }

    private boolean isValidUrl(String url) {
        if (includes == null) {
            return false;
        }
        return includes.stream().anyMatch(url::contains);
    }

    private Document downloadDocument(String url) {
        try {
            Semaphore hostSemaphore = hostSemaphores.computeIfAbsent(URLUtils.getHost(url), _ -> new Semaphore(perHost));
            hostSemaphore.acquire();
            try {
                return downloader.download(url);
            } finally {
                hostSemaphore.release();
            }
        } catch (IOException e) {
            errors.put(url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    private List<String> extractLinks(String url, Document document) {
        List<String> res = new ArrayList<>();
        try {
            res = document.extractLinks().stream().filter(this::isValidUrl).toList();
        } catch (IOException e) {
            errors.put(url, e);
        }
        return res;
    }

    public Result downloadRecursively(String url) {
        if (!isValidUrl(url)) {
            return new Result(List.of(), errors);
        }
        Set<String> visited = new HashSet<>();
        List<String> links = List.of(url);
        for (int i = 0; i < maxDepth; ++i) {
            final int idx = i;
            links = links.stream().filter(visited::add).toList();
            List<CompletableFuture<List<String>>> linksFuture = links.stream().map(link -> CompletableFuture
                    .supplyAsync(() -> downloadDocument(link), downloaderExecutor)
                    .thenApplyAsync(document -> {
                        if (document == null || idx == maxDepth - 1) {
                            return List.<String>of();
                        }
                        return extractLinks(link, document);
                    }, extractorExecutor)).toList();

            try {
                links = new ArrayList<>();
                for (CompletableFuture<List<String>> futureList : linksFuture) {
                    links.addAll(futureList.get());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ExecutionException e) {
                System.err.println("Exception in worker thread: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }

        visited.removeAll(errors.keySet());
        return new Result(visited.stream().toList(), errors);
    }
}
