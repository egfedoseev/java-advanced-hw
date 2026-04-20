package info.kgeorgiy.ja.fedoseev.streams.spliterator;

import info.kgeorgiy.java.advanced.streams.Trees;

import java.util.List;

public class NaryTreeSpliterator<K> extends AbstractTreeSpliterator<Trees.Nary<K>, K> {
    public NaryTreeSpliterator(Trees.Nary<K> node) {
        super(node, node instanceof Trees.Nary.Node ? Long.MAX_VALUE : 1);
    }

    protected NaryTreeSpliterator(List<IntPair<Trees.Nary<K>>> stack, long estimated) {
        super(stack, estimated);
    }

    @Override
    protected boolean isBranch(Trees.Nary<K> node) {
        return node instanceof Trees.Nary.Node<K>;
    }

    @Override
    protected Trees.Nary<K> getChild(Trees.Nary<K> branch, int idx) {
        return ((Trees.Nary.Node<K>) branch).children().get(idx);
    }

    @Override
    protected int getChildrenCount(Trees.Nary<K> branch) {
        return ((Trees.Nary.Node<K>) branch).children().size();
    }

    @Override
    protected AbstractTreeSpliterator<Trees.Nary<K>, K> createSpliterator(List<IntPair<Trees.Nary<K>>> stack, long estimated) {
        return new NaryTreeSpliterator<>(stack, estimated);
    }

    @Override
    protected long getEstimated(Trees.Nary<K> node) {
        return node instanceof Trees.Nary.Node ? Long.MAX_VALUE : 1;
    }
}
