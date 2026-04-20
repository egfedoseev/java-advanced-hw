package info.kgeorgiy.ja.fedoseev.streams;

import info.kgeorgiy.java.advanced.streams.HardStreams;
import info.kgeorgiy.java.advanced.streams.Trees;
import info.kgeorgiy.ja.fedoseev.streams.spliterator.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Gatherer;

public class Streams implements HardStreams {
    @Override
    public <T> Spliterator<T> nestedBinaryTreeSpliterator(Trees.Binary<List<T>> binary) {
        return new NestedTreeSpliterator<>(new BinaryTreeSpliterator<>(binary));
    }

    @Override
    public <T> Spliterator<T> nestedSizedBinaryTreeSpliterator(Trees.SizedBinary<List<T>> sizedBinary) {
        return new NestedTreeSpliterator<>(new SizedBinaryTreeSpliterator<>(sizedBinary));
    }

    @Override
    public <T> Spliterator<T> nestedNaryTreeSpliterator(Trees.Nary<List<T>> nary) {
        return new NestedTreeSpliterator<>(new NaryTreeSpliterator<>(nary));
    }

    @Override
    public <T> Collector<T, ?, List<T>> head(int k) {
        return Collector.of(ArrayList::new,
                (list, elem) -> {
                    if (list.size() < k) {
                        list.add(elem);
                    }
                },
                (left, right) -> {
                    if (left.size() < k && !right.isEmpty()) {
                        left.addAll(right.subList(0, Math.min(right.size(), k - left.size())));
                    }
                    return left;
                });
    }

    @Override
    public <T> Collector<T, ?, List<T>> tail(int k) {
        return Collector.of(ArrayDeque<T>::new,
                (deque, elem) -> {
                    deque.add(elem);
                    if (deque.size() > k) {
                        deque.pollFirst();
                    }
                },
                (left, right) -> {
                    while (!left.isEmpty() && right.size() < k) {
                        right.addFirst(left.pollLast());
                    }
                    return right;
                }, deque -> k > 0 ? List.copyOf(deque) : List.of());
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> kth(int k) {
        class State {
            int count = 0;
            T value = null;
        }

        return Collector.of(State::new,
                (state, elem) -> {
                    if (state.count == k) {
                        state.value = elem;
                    }
                    state.count++;
                },
                (_, _) -> {
                    throw new UnsupportedOperationException("kth collector does not support parallel streams");
                },
                state -> Optional.ofNullable(state.value));
    }

    @Override
    public Gatherer<CharSequence, ?, CharSequence> stringSuffixes() {
        return Gatherer.of(Gatherer.Integrator.ofGreedy((_, seq, downstream) -> {
            for (int i = seq.length() - 1; i >= 0; --i) {
                downstream.push(seq.subSequence(i, seq.length()));
            }
            return true;
        }));
    }

    @Override
    public <T> Gatherer<T, ?, T> ithOfN(int i, int n) {
        class State {
            int cnt = 0;
        }
        return Gatherer.ofSequential(State::new,
                Gatherer.Integrator.ofGreedy((state, elem, downstream) -> {
                    if (state.cnt == i) {
                        downstream.push(elem);
                    }
                    if (++state.cnt == n) {
                        state.cnt = 0;
                    }
                    return true;
                }));
    }

    @Override
    public <T, K> Gatherer<T, ?, T> distinctPrefixBy(Function<? super T, K> function) {
        return Gatherer.ofSequential(HashSet<K>::new, Gatherer.Integrator.of((st, elem, downstream) -> {
            K applied = function.apply(elem);
            if (st.contains(applied)) {
                return false;
            }
            st.add(applied);
            return downstream.push(elem);
        }));
    }

    private static class Holder<T> {
        private T value;

        public Holder() {
        }

        public void set(T value) {
            this.value = value;
        }

        public T get() {
            return value;
        }

        public T or(T value) {
            return this.value == null ? value : this.value;
        }

        public boolean isPresent() {
            return value != null;
        }

        public void setIfAbsent(T value) {
            if (!isPresent()) {
                set(value);
            }
        }

        public Holder<T> combine(Holder<T> other, BiFunction<T, ? super T, T> combiner) {
            value = combiner.apply(value, other.get());
            return this;
        }
    }

    @Override
    public <T> Spliterator<T> binaryTreeSpliterator(Trees.Binary<T> tree) {
        return new BinaryTreeSpliterator<>(tree);
    }

    @Override
    public <T> Spliterator<T> sizedBinaryTreeSpliterator(Trees.SizedBinary<T> tree) {
        return new SizedBinaryTreeSpliterator<>(tree);
    }

    @Override
    public <T> Spliterator<T> naryTreeSpliterator(Trees.Nary<T> tree) {
        return new NaryTreeSpliterator<>(tree);
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> first() {
        return Collector.of(
                Holder::new,
                (Holder<T> acc, T val) -> acc.setIfAbsent(val),
                (left, right) -> left.isPresent() ? left : right,
                acc -> Optional.ofNullable(acc.get()));
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> last() {
        return Collector.of(
                Holder::new,
                (Holder<T> acc, T val) -> acc.set(val),
                (left, right) -> right.isPresent() ? right : left,
                acc -> Optional.ofNullable(acc.get()));
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> middle() {
        return Collector.of(ArrayList::new,
                (ArrayList<T> acc, T elem) -> acc.add(elem),
                (left, _) -> left,
                acc -> Optional.ofNullable(acc.isEmpty() ? null : acc.get(acc.size() / 2)));
    }

    private static int findCommon(CharSequence first, CharSequence second, int beginFirst, int beginSecond, int step) {
        int res = 0;
        for (int i = beginFirst, j = beginSecond;
             i >= 0 && j >= 0 && i < first.length() && j < second.length() && first.charAt(i) == second.charAt(j);
             i += step, j += step) {
            res++;
        }
        return res;
    }

    private static int findCommonPrefix(CharSequence first, CharSequence second) {
        return findCommon(first, second, 0, 0, 1);
    }

    private static int findCommonSuffix(CharSequence first, CharSequence second) {
        return findCommon(first, second, first.length() - 1, second.length() - 1, -1);
    }

    private static StringBuilder chopToCommon(StringBuilder sb, CharSequence seq, boolean isPrefix) {
        if (sb == null && seq == null) {
            return null;
        }
        if (sb == null) {
            return new StringBuilder(seq);
        }
        if (seq != null) {
            if (isPrefix) {
                sb.delete(findCommonPrefix(sb, seq), sb.length());
            } else {
                sb.delete(0, sb.length() - findCommonSuffix(sb, seq));
            }
        }
        return sb;
    }

    private static StringBuilder chopToCommonPrefix(StringBuilder sb, CharSequence seq) {
        return chopToCommon(sb, seq, true);
    }

    private static StringBuilder chopToCommonSuffix(StringBuilder sb, CharSequence seq) {
        return chopToCommon(sb, seq, false);
    }

    @Override
    public Collector<CharSequence, ?, String> commonPrefix() {
        return Collector.of(
                Holder::new,
                (Holder<StringBuilder> holder, CharSequence seq) -> holder.set(chopToCommonPrefix(holder.get(), seq)),
                (left, right) -> left.combine(right, Streams::chopToCommonPrefix),
                holder -> holder.or(new StringBuilder()).toString());

    }

    @Override
    public Collector<CharSequence, ?, String> commonSuffix() {
        return Collector.of(
                Holder::new,
                (Holder<StringBuilder> holder, CharSequence seq) -> holder.set(chopToCommonSuffix(holder.get(), seq)),
                (left, right) -> left.combine(right, Streams::chopToCommonSuffix),
                holder -> holder.or(new StringBuilder()).toString());
    }

    @Override
    public Gatherer<CharSequence, ?, CharSequence> stringPrefixes() {
        return Gatherer.of(Gatherer.Integrator.ofGreedy((_, seq, downstream) -> {
            for (int i = 1; i <= seq.length(); ++i) {
                downstream.push(seq.subSequence(0, i));
            }
            return true;
        }));
    }

    @Override
    public <T> Gatherer<T, ?, T> nth(int n) {
        class State {
            int cnt = 0;
        }
        return Gatherer.ofSequential(State::new, Gatherer.Integrator.ofGreedy((state, elem, downstream) -> {
            state.cnt++;
            if (state.cnt == n) {
                downstream.push(elem);
                state.cnt = 0;
            }
            return true;
        }));
    }

    @Override
    public <T> Gatherer<T, ?, T> distinctPrefix() {
        return distinctPrefixBy(Function.identity());
    }
}
