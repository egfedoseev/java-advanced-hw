package info.kgeorgiy.ja.fedoseev.streams.spliterator;

import info.kgeorgiy.java.advanced.streams.Trees;

import java.util.List;

public class SizedBinaryTreeSpliterator<K> extends AbstractTreeSpliterator<Trees.SizedBinary<K>, K> {
    public SizedBinaryTreeSpliterator(Trees.SizedBinary<K> node) {
        super(node, node.size());
    }

    protected SizedBinaryTreeSpliterator(List<IntPair<Trees.SizedBinary<K>>> stack, long estimated) {
        super(stack, estimated);
    }

    @Override
    protected boolean isBranch(Trees.SizedBinary<K> node) {
        return node instanceof Trees.SizedBinary.Branch<K>;
    }

    @Override
    protected Trees.SizedBinary<K> getChild(Trees.SizedBinary<K> branch, int idx) {
        Trees.SizedBinary.Branch<K> binaryBranch = (Trees.SizedBinary.Branch<K>) branch;
        return idx == 0 ? binaryBranch.left() : binaryBranch.right();
    }

    @Override
    protected int getChildrenCount(Trees.SizedBinary<K> branch) {
        return 2;
    }

    @Override
    protected AbstractTreeSpliterator<Trees.SizedBinary<K>, K> createSpliterator(List<IntPair<Trees.SizedBinary<K>>> stack, long estimated) {
        return new SizedBinaryTreeSpliterator<>(stack, estimated);
    }

    @Override
    protected long getEstimated(Trees.SizedBinary<K> node) {
        return node.size();
    }
}
