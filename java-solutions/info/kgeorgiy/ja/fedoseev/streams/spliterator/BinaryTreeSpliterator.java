package info.kgeorgiy.ja.fedoseev.streams.spliterator;

import info.kgeorgiy.java.advanced.streams.Trees;

import java.util.List;


public class BinaryTreeSpliterator<K> extends AbstractTreeSpliterator<Trees.Binary<K>, K> {
    public BinaryTreeSpliterator(Trees.Binary<K> node) {
        super(node, node instanceof Trees.Binary.Branch ? Long.MAX_VALUE : 1);
    }

    protected BinaryTreeSpliterator(List<IntPair<Trees.Binary<K>>> stack, long estimated) {
        super(stack, estimated);
    }

    @Override
    protected boolean isBranch(Trees.Binary<K> node) {
        return node instanceof Trees.Binary.Branch<?>;
    }

    @Override
    protected Trees.Binary<K> getChild(Trees.Binary<K> branch, int idx) {
        Trees.Binary.Branch<K> binaryBranch = (Trees.Binary.Branch<K>) branch;
        return idx == 0 ? binaryBranch.left() : binaryBranch.right();
    }

    @Override
    protected int getChildrenCount(Trees.Binary<K> branch) {
        return 2;
    }

    @Override
    protected AbstractTreeSpliterator<Trees.Binary<K>, K> createSpliterator(List<IntPair<Trees.Binary<K>>> stack, long estimated) {
        return new BinaryTreeSpliterator<>(stack, estimated);
    }

    @Override
    protected long getEstimated(Trees.Binary<K> node) {
        return node instanceof Trees.Binary.Branch ? Long.MAX_VALUE : 1;
    }
}
