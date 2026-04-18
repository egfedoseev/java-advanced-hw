package info.kgeorgiy.ja.fedoseev.streams;

import info.kgeorgiy.java.advanced.streams.Trees;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class AbstractTreeSpliterator<T> implements Spliterator<T> {
    private List<T> stack;
    private int mnLeft = Integer.MAX_VALUE;

    protected AbstractTreeSpliterator(List<T> stack) {
        this.stack = stack;
    }

    protected abstract boolean isBranch(T node);
    protected abstract boolean isLeaf(T node);
    protected abstract <E extends T> E getChild(T branch, int idx);
    protected abstract int getChildrenCount(T branch);

    private int getNextChild(T branch, T child) {
        for (int i = 0; i < getChildrenCount(branch) - 1; ++i) {
            if (getChild(branch, i) == child) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> consumer) {
        if (stack.isEmpty()) {
            return false;
        }
        for (T last = stack.getLast(); isBranch(last = stack.getLast()); last = stack.getLast()) {
            mnLeft = Math.min(mnLeft, stack.size());
            stack.addLast(getChild(last, 0));
        }
        T leaf = stack.getLast();
        consumer.accept(stack.getLast());
        boolean found = false;
        while (stack.size() > 1) {
            var prev = stack.getLast();
            stack.removeLast();
            if (stack.size() <= mnLeft) {
                mnLeft = Integer.MAX_VALUE;
            }
            T branch = stack.getLast();
            int idx = getChildIndex(branch, prev);
            if (idx < ) {

            }
            for (int i = 0; i < getChildrenCount(branch) - 1; ++i) {
                if (getChild(branch, i) == prev) {
                    stack.addLast(getChild(branch, i + 1));
                    found = true;
                    break;
                }
            }
        }
        return true;
    }

    @Override
    public Spliterator<T> trySplit() {
        if (mnLeft >= stack.size()) {
            return null;
        }
        List<T> split = new ArrayList<>(stack.subList(mnLeft, stack.size()));
        stack = stack.stream().limit(mnLeft).collect(Collectors.toCollection(ArrayList::new));
        stack.addLast((stack.getLast()).right());
        mnLeft = Integer.MAX_VALUE;
        return new Streams.BinarySpliterator<T>(split);
    }
}
