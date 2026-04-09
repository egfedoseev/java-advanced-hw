package info.kgeorgiy.ja.fedoseev.arrayset;

import info.kgeorgiy.ja.fedoseev.util.BinarySearch;

import java.util.*;

public abstract class ImmutableSet<E> implements NavigableSet<E> {
    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException("The pollFirst operation is unsupported, ArraySet is immutable");
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException("The pollLast operation is unsupported, ArraySet is immutable");
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException("The add operation is unsupported, ArraySet is immutable");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("The remove operation is unsupported, ArraySet is immutable");
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return collection.stream().allMatch(this::contains);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        throw new UnsupportedOperationException("The addAll operation is unsupported, ArraySet is immutable");
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        throw new UnsupportedOperationException("The retainAll operation is unsupported, ArraySet is immutable");
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        throw new UnsupportedOperationException("The removeAll operation is unsupported, ArraySet is immutable");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("The clear operation is unsupported, ArraySet is immutable");
    }
}
