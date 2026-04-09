package info.kgeorgiy.ja.fedoseev.arrayset;

import info.kgeorgiy.ja.fedoseev.util.BinarySearch;

import java.util.*;
import java.util.function.IntUnaryOperator;

public class ArraySet<E> extends ImmutableSet<E> {
    private class SetIterator implements Iterator<E> {
        private int nextIdx;
        private final IntUnaryOperator operator;

        private SetIterator(final int nextIdx, final IntUnaryOperator operator) {
            this.nextIdx = nextIdx;
            this.operator = operator;
        }

        @Override
        public boolean hasNext() {
            return nextIdx < elements.size() && nextIdx >= 0;
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            int oldIdx = nextIdx;
            nextIdx = operator.applyAsInt(nextIdx);
            return elements.get(oldIdx);
        }
    }

    private final Comparator<E> comparator;
    private List<E> elements;

    private void constructArray(Collection<? extends E> collection) {
        elements = new ArrayList<>(collection);
        elements.sort(comparator);
        int j = 0;
        for (int i = 0; i < elements.size(); i++) {
            if (i == 0 || compare(elements.get(i), elements.get(i - 1)) != 0) {
                elements.set(j++, elements.get(i));
            }
        }
        elements = new ArrayList<>(elements.subList(0, j));
    }

    public ArraySet() {
        this(Collections.emptySet());
    }

    public ArraySet(Collection<? extends E> collection) {
        comparator = null;
        constructArray(collection);
    }

    public ArraySet(Collection<? extends E> collection, Comparator<E> comparator) {
        this.comparator = comparator;
        constructArray(collection);
    }

    private ArraySet(List<E> list, Comparator<E> comparator) {
        this.comparator = comparator;
        elements = list;
    }

    @Override
    public NavigableSet<E> reversed() {
        return descendingSet();
    }

    private E get(final int idx) {
        if (idx < 0 || idx >= elements.size()) {
            return null;
        }
        return elements.get(idx);
    }

    @Override
    public E lower(final E e) {
        return get(BinarySearch.lowerBound(elements, e, comparator) - 1);
    }

    @Override
    public E floor(final E e) {
        return get(BinarySearch.upperBound(elements, e, comparator) - 1);
    }

    @Override
    public E ceiling(final E e) {
        return get(BinarySearch.lowerBound(elements, e, comparator));
    }

    @Override
    public E higher(final E e) {
        return get(BinarySearch.upperBound(elements, e, comparator));
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public boolean contains(final Object o) {
        try {
            @SuppressWarnings("unchecked") final E e = (E) o;
            return Collections.binarySearch(elements, e, comparator) >= 0;
        } catch (ClassCastException | NullPointerException e) {
            return false;
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new SetIterator(0, i -> i + 1);
    }

    @Override
    public Object[] toArray() {
        return elements.toArray();
    }

    @Override
    public <T> T[] toArray(T[] ts) {
        return elements.toArray(ts);
    }

    @SuppressWarnings("unchecked")
    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(elements.reversed(), comparator == null ? (Comparator<E>) Comparator.reverseOrder() : comparator.reversed());
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new SetIterator(elements.size() - 1, i -> i - 1);
    }

    private int calcLeftBorder(final E fromElem, boolean inclusive) {
        return inclusive ? BinarySearch.lowerBound(elements, fromElem, comparator) : BinarySearch.upperBound(elements, fromElem, comparator);
    }

    private int calcRightBorder(final E toElem, boolean inclusive) {
        return inclusive ? BinarySearch.upperBound(elements, toElem, comparator) : BinarySearch.lowerBound(elements, toElem, comparator);
    }

    @SuppressWarnings("unchecked")
    private int compare(final E e1, final E e2) {
        return comparator == null ? ((Comparator<E>) Comparator.naturalOrder()).compare(e1, e2) : comparator.compare(e1, e2);
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        if (compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException("fromElement is greater than toElement");
        }
        int left =  calcLeftBorder(fromElement, fromInclusive);
        int right = calcRightBorder(toElement, toInclusive);
        if (left > right) {
            right = left;
        }
        return new ArraySet<>(elements.subList(left, right), comparator);
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return new ArraySet<>(elements.subList(0, calcRightBorder(toElement, inclusive)), comparator);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return new ArraySet<>(elements.subList(calcLeftBorder(fromElement, inclusive), elements.size()), comparator);
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public E first() {
        if (isEmpty()) {
            throw new NoSuchElementException("Set is empty");
        }
        return elements.getFirst();
    }

    @Override
    public E last() {
        if (isEmpty()) {
            throw new NoSuchElementException("Set is empty");
        }
        return elements.getLast();
    }
}
