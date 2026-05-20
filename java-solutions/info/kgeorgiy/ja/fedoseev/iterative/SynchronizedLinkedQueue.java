package info.kgeorgiy.ja.fedoseev.iterative;

/**
 * A thread-safe queue based on a linked list.
 * <p>
 * This implementation uses internal method synchronization to ensure
 * safe addition and retrieval of elements across different threads.
 *
 * @param <T> the type of elements held in this queue
 */
public class SynchronizedLinkedQueue<T> {
    public SynchronizedLinkedQueue(int threadCount) {
        this.threadCount = threadCount;
    }

    private static class Node<T> {
        final T val;
        Node<T> next;

        Node() {
            val = null;
        }

        Node(T val) {
            this.val = val;
        }
    }

    private final int threadCount;
    private Node<T> head;
    private Node<T> tail = new Node<>();

    /**
     * Retrieves and removes the head of this queue, waiting if necessary
     * until an element becomes available.
     *
     * @return the retrieved element from the head of the queue
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    public synchronized T take() throws InterruptedException {
        while (head == null) {
            wait();
        }
        return poll();
    }

    /**
     * Retrieves and removes the head of this queue, or returns {@code null}
     * if this queue is empty.
     * Unlike the {@link #take()} method, this method does not block the calling thread.
     *
     * @return the retrieved element from the head of the queue, or {@code null} if the queue is empty
     */
    public synchronized T poll() {
        if (head == null) {
            return null;
        }
        T val = head.val;
        head = head.next;
        return val;
    }

    /**
     * Adds multiple elements to the end of the queue atomically.
     * All threads waiting for elements will be awakened.
     *
     * @param elements the collection of elements to add
     */
    public synchronized void addAll(Iterable<? extends T> elements) {
        int cnt = 0;
        for (T val : elements) {
            tail.next = new Node<>(val);
            tail = tail.next;
            if (head == null) {
                head = tail;
            }
            ++cnt;
        }

        if (cnt >= threadCount) {
            notifyAll();
        } else {
            for (int i = 0; i < cnt; ++i) {
                notify();
            }
        }
    }
}