package info.kgeorgiy.ja.fedoseev.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

public class WebCrawler implements NewCrawler {

    private final Downloader downloader;
    private final int perHost;

    private final ExecutorService downloaderExecutor;
    private final ExecutorService extractorExecutor;

    private final ConcurrentHashMap<String, Semaphore> hostSemaphores = new ConcurrentHashMap<>();

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.perHost = perHost;

        downloaderExecutor = Executors.newFixedThreadPool(downloaders);
        extractorExecutor = Executors.newFixedThreadPool(extractors);
    }

    @Override
    public Result download(String url, int depth, final List<String> includes) {
        RecursiveDownloader recursiveDownloader = new RecursiveDownloader(downloader, perHost, downloaderExecutor,
                extractorExecutor, hostSemaphores, depth, includes);
        return recursiveDownloader.downloadRecursively(url);
    }

    @Override
    public void close() {
        downloaderExecutor.close();
        extractorExecutor.close();
    }

    private static int parseArg(String[] args, int idx) {
        return args.length > idx ? Integer.parseInt(args[idx]) : 1;
    }

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
