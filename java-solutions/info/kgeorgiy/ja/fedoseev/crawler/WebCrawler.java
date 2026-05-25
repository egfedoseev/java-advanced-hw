package info.kgeorgiy.ja.fedoseev.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

/**
 * Thread-safe web crawler implementation that recursively traverses web pages starting from a given URL.
 * It downloads pages and extracts links up to a specified maximum depth, filtering URLs by provided substrings.
 */
public class WebCrawler implements NewCrawler {

    private final Downloader downloader;
    private final int perHost;

    private final ExecutorService downloaderExecutor;
    private final ExecutorService extractorExecutor;

    private final ConcurrentHashMap<String, Semaphore> hostSemaphores = new ConcurrentHashMap<>();

    /**
     * Constructs a new {@code WebCrawler} instance with specified downloaders, extractors, and per-host limits.
     *
     * @param downloader  the {@link Downloader} used to download web pages and extract links.
     * @param downloaders the maximum number of simultaneously downloaded pages.
     * @param extractors  the maximum number of pages from which links are simultaneously extracted.
     * @param perHost     the maximum number of pages simultaneously downloaded from a single host.
     */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.perHost = perHost;

        downloaderExecutor = Executors.newFixedThreadPool(downloaders);
        extractorExecutor = Executors.newFixedThreadPool(extractors);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Result download(String url, int depth, final List<String> includes) {
        RecursiveDownloader recursiveDownloader = new RecursiveDownloader(downloader, perHost, downloaderExecutor,
                extractorExecutor, hostSemaphores, depth, includes);
        return recursiveDownloader.download(url);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        downloaderExecutor.close();
        extractorExecutor.close();
    }

    private static int parseArg(String[] args, int idx) {
        return args.length > idx ? Integer.parseInt(args[idx]) : 1;
    }

    /**
     * The main entry point for running the web crawler from the command line.
     * Usage: {@code WebCrawler url [depth [downloaders [extractors [perHost]]]]}
     *
     * @param args the command-line arguments specifying configuration and start URL.
     */
    public static void main(String[] args) {
        if (args.length < 1 || args.length > 5) {
            System.err.println("Usage: WebCrawler url [depth [downloaders [extractors [perHost]]]]");
            return;
        }

        final String url = args[0];

        int depth = parseArg(args, 1);
        int downloaders = parseArg(args, 2);
        int extractors = parseArg(args, 3);
        int perHost = parseArg(args, 4);

        try {
            Downloader downloader = new CachingDownloader(1.0);
            try (WebCrawler crawler = new WebCrawler(downloader, downloaders, extractors, perHost)) {
                Result result = crawler.download(url, depth);
                System.out.println("Downloaded: " + result.downloaded());
                System.out.println("With errors: " + result.errors().keySet());
            }
        } catch (IOException e) {
            System.err.println("Can't create temp directory: " + e.getMessage());
        }
    }
}
