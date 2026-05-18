package info.kgeorgiy.ja.fedoseev.iterative;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * An implementation of {@link ParallelMapper} that evaluates functions concurrently
 * using a fixed pool of worker platform threads.
 * <p>
 * This class uses a custom synchronized queue to distribute tasks among the threads.
 * If the mapper is closed via the {@link #close()} method, all worker threads are interrupted,
 * and any pending tasks will immediately throw an {@link IllegalStateException} to prevent deadlocks.
 */
public class ParallelMapperImpl implements ParallelMapper {

    private final SynchronizedLinkedQueue<Runnable> queue = new SynchronizedLinkedQueue<>();
    private final Thread[] threads;

    private volatile boolean closed = false;

    /**
     * Constructs a new {@code ParallelMapperImpl} with the specified number of worker threads.
     * The threads are created and started immediately upon instantiation.
     *
     * @param threads the number of concurrent worker threads to be used by this mapper
     */
    public ParallelMapperImpl(int threads) {
        final Thread.Builder.OfVirtual threadBuilder = Thread.ofVirtual().name("worker-", 0);
        this.threads = new Thread[threads];
        for (int i = 0; i < threads; ++i) {
            this.threads[i] = threadBuilder.start(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        queue.take().run();
                    }
                } catch (InterruptedException _) {
                } finally {
                    Runnable task = queue.poll();
                    while (task != null) {
                        task.run();
                        task = queue.poll();
                    }
                    Thread.currentThread().interrupt();
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if this mapper has been closed before or during task execution
     */
    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> f, final List<? extends T> items) throws InterruptedException {
        if (closed) {
            throw new IllegalStateException("Mapper is closed");
        }

        final Object lock = new Object();
        final int[] tasksRemaining = {items.size()};

        final List<R> results = new ArrayList<>(items.size());
        results.addAll(Collections.nCopies(items.size(), null));
        final RuntimeException[] errors = new RuntimeException[items.size()];

        for (int i = 0; i < items.size(); ++i) {
            final int idx = i;
            Runnable wrappedTask = () -> {
                try {
                    if (closed) {
                        throw new IllegalStateException("Mapper is closed");
                    }
                    results.set(idx, f.apply(items.get(idx)));
                } catch (RuntimeException e) {
                    errors[idx] = e;
                } finally {
                    synchronized (lock) {
                        tasksRemaining[0]--;
                        if (tasksRemaining[0] == 0) {
                            lock.notifyAll();
                        }
                    }
                }
            };
            queue.push(wrappedTask);
        }

        synchronized (lock) {
            while (tasksRemaining[0] > 0) {
                lock.wait();
            }
        }

        RuntimeException mainException = null;
        for (RuntimeException error : errors) {
            if (error != null) {
                if (mainException == null) {
                    mainException = error;
                } else {
                    mainException.addSuppressed(error);
                }
            }
        }

        if (mainException != null) {
            throw mainException;
        }
        return results;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        closed = true;
        for (Thread thread : threads) {
            thread.interrupt();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}