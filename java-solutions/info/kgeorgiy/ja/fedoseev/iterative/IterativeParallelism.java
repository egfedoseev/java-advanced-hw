package info.kgeorgiy.ja.fedoseev.iterative;

import info.kgeorgiy.java.advanced.iterative.NewListIP;

import java.util.*;
import java.util.concurrent.ThreadFactory;
import java.util.function.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Implementation of {@link NewListIP} that executes list operations concurrently.
 * <p>
 * This implementation uses Java 21 Virtual Threads to achieve lightweight and efficient parallelism.
 * The input list is partitioned into chunks evenly distributed across the specified number of threads.
 */
public class IterativeParallelism implements NewListIP {
    private final ThreadFactory threadFactory = Thread.ofVirtual().name("worker-", 0).factory();


    private <T, R> R runParallel(int n, int size, BiFunction<Integer, Integer, T> worker, Function<List<T>, R> reducer) throws InterruptedException {
        if (n < 1) {
            throw new IllegalArgumentException("Number of threads must be positive");
        }
        n = Math.max(1, Math.min(n, size));
        int blockSize = size / n;
        int rem = size % n;

        Thread[] threads = new Thread[n];
        List<T> results = new ArrayList<>(Collections.nCopies(n, null));
        RuntimeException[] errors = new RuntimeException[n];

        for (int i = 0; i < n; ++i) {
            final int idx = i;
            int begin = idx * blockSize + Math.min(idx, rem);
            int end = begin + blockSize + (idx < rem ? 1 : 0);

            threads[i] = threadFactory.newThread(() -> {
                try {
                    results.set(idx, worker.apply(begin, end));
                } catch (RuntimeException e) {
                    errors[idx] = e;
                }
            });
        }
        runThreads(threads);

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
        return reducer.apply(results);
    }

    private void runThreads(Thread[] threads) throws InterruptedException {
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int argMax(int threads, List<T> values, Comparator<? super T> comparator) throws InterruptedException {
        return argMax(threads, values, comparator, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int argMin(int threads, List<T> values, Comparator<? super T> comparator) throws InterruptedException {
        return argMin(threads, values, comparator, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int indexOf(int threads, List<T> values, Predicate<? super T> predicate) throws InterruptedException {
        return indexOf(threads, values, predicate, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int lastIndexOf(int threads, List<T> values, Predicate<? super T> predicate) throws InterruptedException {
        return lastIndexOf(threads, values, predicate, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> long sumIndices(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return sumIndices(threads, values, predicate, 1);
    }

    private IntStream getIndicesStream(int begin, int end, int step) {
        return IntStream.iterate(((begin + step - 1) / step) * step, i -> i < end, i -> i + step);
    }

    private <T> Stream<T> getValuesStream(int begin, int end, int step, List<T> values) {
        return getIndicesStream(begin, end, step).mapToObj(values::get);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int argMax(int threads, List<T> values, Comparator<? super T> comparator, int step) throws InterruptedException {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(comparator, "comparator");
        IntBinaryOperator maxIndex = (a, b) -> comparator.compare(values.get(a), values.get(b)) >= 0 ? a : b;
        return runParallel(threads, values.size(),
                (begin, end) -> getIndicesStream(begin, end, step).reduce(maxIndex).orElse(-1),
                results -> results.stream().mapToInt(Integer::intValue).filter(i -> i != -1).reduce(maxIndex).orElse(-1));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int argMin(int threads, List<T> values, Comparator<? super T> comparator, int step) throws InterruptedException {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(comparator, "comparator");
        IntBinaryOperator minIndex = (a, b) -> comparator.compare(values.get(a), values.get(b)) <= 0 ? a : b;
        return runParallel(threads, values.size(),
                (begin, end) -> getIndicesStream(begin, end, step).reduce(minIndex).orElse(-1),
                results -> results.stream().mapToInt(Integer::intValue).filter(i -> i != -1).reduce(minIndex).orElse(-1));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int indexOf(int threads, List<T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(predicate, "predicate");
        return runParallel(threads, values.size(),
                (begin, end) -> getIndicesStream(begin, end, step).filter(i -> predicate.test(values.get(i)))
                        .findFirst().orElse(-1),
                results -> results.stream().mapToInt(Integer::intValue).filter(i -> i != -1)
                        .findFirst().orElse(-1));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int lastIndexOf(int threads, List<T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(predicate, "predicate");
        return runParallel(threads, values.size(),
                (begin, end) -> getIndicesStream(begin, end, step).filter(i -> predicate.test(values.get(i)))
                        .reduce((_, b) -> b).orElse(-1),
                results -> results.stream().mapToInt(Integer::intValue).filter(i -> i != -1)
                        .reduce((_, b) -> b).orElse(-1));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> long sumIndices(int threads, List<? extends T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(predicate, "predicate");
        return runParallel(threads, values.size(),
                (begin, end) -> getIndicesStream(begin, end, step).filter(i -> predicate.test(values.get(i))).asLongStream().sum(),
                results -> results.stream().mapToLong(Long::longValue).sum());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int[] indices(int threads, List<? extends T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(predicate, "predicate");
        return runParallel(threads, values.size(),
                (begin, end) -> getIndicesStream(begin, end, step).filter(i -> predicate.test(values.get(i))).toArray(),
                results -> results.stream().flatMapToInt(Arrays::stream).toArray());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(predicate, "predicate");
        return runParallel(threads, values.size(),
                (begin, end) -> getValuesStream(begin, end, step, values).filter(predicate)
                        .toList(),
                results -> results.stream().<T>flatMap(List::stream).toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T, R> List<R> map(int threads, List<? extends T> values, Function<? super T, ? extends R> f, int step) throws InterruptedException {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(f, "f");
        return runParallel(threads, values.size(),
                (begin, end) -> getValuesStream(begin, end, step, values).map(f).toList(),
                results -> results.stream().<R>flatMap(List::stream).toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int[] indices(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return indices(threads, values, predicate, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return filter(threads, values, predicate, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return map(threads, values, f, 1);
    }
}
