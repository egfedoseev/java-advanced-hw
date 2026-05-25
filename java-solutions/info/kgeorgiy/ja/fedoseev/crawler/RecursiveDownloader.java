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

/**
 * Performs layered, asynchronous web crawling using {@link CompletableFuture}.
 * Encapsulates the state of a single recursive crawling execution session, managing tracking of visited pages,
 * error handling, and coordination across download and extraction thread pools.
 *
 */
public class RecursiveDownloader {

    private final ConcurrentHashMap<String, IOException> errors = new ConcurrentHashMap<>();

    private final Downloader downloader;
    private final int perHost;
    private final ExecutorService downloaderExecutor;
    private final ExecutorService extractorExecutor;
    private final ConcurrentMap<String, Semaphore> hostSemaphores;
    private final int maxDepth;
    private final List<String> includes;

    /**
     * Constructs a {@code RecursiveDownloader} execution context for a single crawl operation.
     *
     * @param downloader          the {@link Downloader} used to retrieve documents.
     * @param perHost             the maximum number of concurrent downloads allowed for a single host.
     * @param downloaderExecutor  the {@link ExecutorService} responsible for managing page download threads.
     * @param extractorExecutor   the {@link ExecutorService} responsible for managing link extraction threads.
     * @param hostSemaphores      a shared map of host names to {@link Semaphore}s for throttling requests per host.
     * @param maxDepth            the maximum depth to traverse.
     * @param includes            the list of valid substrings used to filter allowed URLs.
     */
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

    /**
     * Executes the layered asynchronous crawl operation starting from the given URL.
     * Traverses links layer by layer up to the configured maximum depth using non-blocking pipelines.
     *
     * @param url                 the starting URL for the crawling process.
     * @return a {@link Result} instance summarizing successful downloads and collected failures.
     */
    public Result download(String url) {
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
