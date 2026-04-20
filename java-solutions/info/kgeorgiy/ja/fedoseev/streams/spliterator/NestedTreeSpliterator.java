package info.kgeorgiy.ja.fedoseev.streams.spliterator;

import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public class NestedTreeSpliterator<T, K> implements Spliterator<K> {
    private final AbstractTreeSpliterator<T, List<K>> spliterator;
    private Spliterator<K> listSpliterator;

    public NestedTreeSpliterator(AbstractTreeSpliterator<T, List<K>> spliterator) {
        this.spliterator = spliterator;
    }

    @Override
    public boolean tryAdvance(Consumer<? super K> consumer) {
        while (listSpliterator == null || !listSpliterator.tryAdvance(consumer)) {
            if (!spliterator.tryAdvance(list -> listSpliterator = list.spliterator())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Spliterator<K> trySplit() {
        if (listSpliterator != null) {
            Spliterator<K> listSplit = listSpliterator.trySplit();
            if (listSplit != null) {
                return listSplit;
            }
        }

        AbstractTreeSpliterator<T, List<K>> split = spliterator.trySplit();
        if (split != null) {
            return new NestedTreeSpliterator<>(split);
        }

        if (listSpliterator == null) {
            if (spliterator.tryAdvance(list -> listSpliterator = list.spliterator())) {
                return listSpliterator.trySplit();
            }
        }
        return null;
    }

    @Override
    public long estimateSize() {
        if (spliterator.estimateSize() == 0 && listSpliterator != null) {
            return listSpliterator.estimateSize();
        }
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        if (spliterator.estimateSize() == 0 && listSpliterator != null) {
            return listSpliterator.characteristics();
        }
        return ORDERED;
    }
}
