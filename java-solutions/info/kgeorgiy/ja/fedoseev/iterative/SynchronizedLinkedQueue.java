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
    private static class Node<T> {
        private final T val;
        private Node<T> next;

        public Node() {
            val = null;
        }

        public Node(T val) {
            this.val = val;
        }
    }

    private Node<T> head;
    private Node<T> tail = new Node<>();

    /**
     * Adds the specified element to the end of the queue.
     * If there are threads waiting for elements (having called the {@link #take()} method),
     * one of them will be awakened.
     *
     * @param val the element to add to the queue
     */
    public synchronized void push(T val) {
        tail.next = new Node<>(val);
        tail = tail.next;
        if (head == null) {
            head = tail;
        }
        notify();
    }

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
}