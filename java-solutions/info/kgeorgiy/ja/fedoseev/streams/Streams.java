package info.kgeorgiy.ja.fedoseev.streams;

import info.kgeorgiy.java.advanced.streams.EasyStreams;
import info.kgeorgiy.java.advanced.streams.Trees;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Gatherer;

public class Streams implements EasyStreams {
    private static class BinarySpliterator<T> implements Spliterator<Trees.Binary<T>> {
        private int mnLeft = Integer.MAX_VALUE;

        List<Trees.Binary<T>> stack = new ArrayList<>();

        BinarySpliterator(Trees.Binary<T> tree) {
            stack.add(tree);
        }

        private BinarySpliterator(List<Trees.Binary<T>> stack) {
            this.stack = stack;
        }

        @Override
        public boolean tryAdvance(Consumer<? super Trees.Binary<T>> consumer) {
            if (stack.isEmpty()) {
                return false;
            }
            while (stack.getLast() instanceof Trees.Binary.Branch<T> branch) {
                mnLeft = Math.min(mnLeft, stack.size());
                stack.addLast(branch.left());
            }
            Trees.Binary<?> leaf = stack.getLast();
            consumer.accept(stack.getLast());
            while (stack.size() > 1) {
                var prev = stack.getLast();
                stack.removeLast();
                if (stack.size() <= mnLeft) {
                    mnLeft = Integer.MAX_VALUE;
                }
                Trees.Binary.Branch<T> branch = (Trees.Binary.Branch<T>) stack.getLast();
                if (branch.left() == prev) {
                    stack.addLast(branch.right());
                    break;
                }
            }
            return true;
        }

        @Override
        public Spliterator<Trees.Binary<T>> trySplit() {
            if (mnLeft >=  stack.size()) {
                return null;
            }
            List<Trees.Binary<T>> split = new ArrayList<>(stack.subList(mnLeft, stack.size()));
            stack = stack.stream().limit(mnLeft).collect(Collectors.toCollection(ArrayList::new));
            stack.addLast(((Trees.Binary.Branch<T>) stack.getLast()).right());
            mnLeft = Integer.MAX_VALUE;
            return new BinarySpliterator<T>(split);
        }

        @Override
        public long estimateSize() {
            return 0;
        }

        @Override
        public int characteristics() {
            return IMMUTABLE;
        }
    }

    @Override
    public <T> Spliterator<T> binaryTreeSpliterator(Trees.Binary<T> tree) {
        return null;
    }

    @Override
    public <T> Spliterator<T> sizedBinaryTreeSpliterator(Trees.SizedBinary<T> tree) {
        return null;
    }

    @Override
    public <T> Spliterator<T> naryTreeSpliterator(Trees.Nary<T> tree) {
        return null;
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> first() {
        return null;
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> last() {
        return null;
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> middle() {
        return null;
    }

    @Override
    public Collector<CharSequence, ?, String> commonPrefix() {
        return null;
    }

    @Override
    public Collector<CharSequence, ?, String> commonSuffix() {
        return null;
    }

    @Override
    public Gatherer<CharSequence, ?, CharSequence> stringPrefixes() {
        return null;
    }

    @Override
    public <T> Gatherer<T, ?, T> nth(int n) {
        return null;
    }

    @Override
    public <T> Gatherer<T, ?, T> distinctPrefix() {
        return null;
    }
}
