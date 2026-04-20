package info.kgeorgiy.ja.fedoseev.streams.spliterator;

import info.kgeorgiy.java.advanced.streams.Trees;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public abstract class AbstractTreeSpliterator<T, K> implements Spliterator<K> {
    public record IntPair<T>(int idx, T node) {
    }

    protected long estimated;
    protected final List<IntPair<T>> stack;
    private int mnLeft = Integer.MAX_VALUE;

    public AbstractTreeSpliterator(T node) {
        List<IntPair<T>> stack = new ArrayList<>();
        stack.addLast(new IntPair<>(0, node));
        this.stack = stack;
    }

    public AbstractTreeSpliterator(T node, long estimated) {
        this(node);
        this.estimated = estimated;
    }

    protected AbstractTreeSpliterator(List<IntPair<T>> stack, long estimated) {
        this.stack = stack;
        this.estimated = estimated;
    }

    protected abstract boolean isBranch(T node);

    protected abstract T getChild(T branch, int idx);

    protected abstract int getChildrenCount(T branch);

    private void addChildToStack(int idx) {
        T child = getChild(stack.getLast().node(), idx);
        stack.addLast(new IntPair<>(idx, child));
    }

    private boolean isSized() {
        return (characteristics() & SIZED) != 0;
    }

    protected void onElementConsumed() {
        if (isSized()) {
            --estimated;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean tryAdvance(Consumer<? super K> consumer) {
        if (stack.isEmpty()) {
            return false;
        }
        for (T last = stack.getLast().node; isBranch(last); last = stack.getLast().node) {
            mnLeft = Math.min(mnLeft, stack.size());
            addChildToStack(0);
        }
        consumer.accept(((Trees.Leaf<K>) stack.getLast().node).value());
        onElementConsumed();
        while (!stack.isEmpty()) {
            IntPair<T> last = stack.getLast();
            int nextIdx = last.idx + 1;
            stack.removeLast();
            if (stack.isEmpty()) {
                break;
            }
            if (stack.size() <= mnLeft) {
                mnLeft = Integer.MAX_VALUE;
            }
            T branch = stack.getLast().node;
            if (nextIdx < getChildrenCount(branch)) {
                if (nextIdx + 1 < getChildrenCount(branch)) {
                    mnLeft = Math.min(mnLeft, stack.size());
                }
                addChildToStack(nextIdx);
                break;
            }
        }
        return true;
    }

    protected abstract AbstractTreeSpliterator<T, K> createSpliterator(List<IntPair<T>> stack, long estimated);

    protected abstract long getEstimated(T node);

    private long getRetainedSize(int cutOff) {
        long sum = 0;
        for (int i = 0; i < cutOff; i++) {
            T node = stack.get(i).node();
            int currentIdx = stack.get(i).idx();
            for (int j = currentIdx + 1; j < getChildrenCount(node); j++) {
                sum += getEstimated(getChild(node, j));
            }
        }
        return sum;
    }

    @Override
    public AbstractTreeSpliterator<T, K> trySplit() {
        if (mnLeft >= stack.size()) {
            if (isBranch(stack.getLast().node()) && getChildrenCount(stack.getLast().node()) > 1) {
                mnLeft = stack.size();
                addChildToStack(0);
            } else {
                return null;
            }
        }
        List<IntPair<T>> split = new ArrayList<>(stack.subList(mnLeft, stack.size()));
        final int nextIdx = stack.get(mnLeft).idx + 1;

        long splitSize;
        if (estimated != Long.MAX_VALUE) {
            long retained = getRetainedSize(mnLeft);
            splitSize = this.estimated - retained;
            this.estimated -= splitSize;
        } else {
            T splitNode = split.getLast().node();
            splitSize = !isBranch(splitNode) ? 1 : Long.MAX_VALUE;
        }

        while (stack.size() > mnLeft) {
            stack.removeLast();
        }
        mnLeft = Integer.MAX_VALUE;
        addChildToStack(nextIdx);
        return createSpliterator(split, splitSize);
    }

    @Override
    public long estimateSize() {
        return estimated;
    }

    @Override
    public int characteristics() {
        if (estimated != Long.MAX_VALUE) {
            return IMMUTABLE | ORDERED | SIZED | SUBSIZED;
        }
        return IMMUTABLE | ORDERED;
    }
}
