package info.kgeorgiy.ja.fedoseev.util;

import java.util.Comparator;
import java.util.List;
import java.util.function.IntPredicate;

public final class BinarySearch {
    private BinarySearch() {
    }

    public static int binarySearch(final int sz, final IntPredicate checker) {
        int l = -1, r = sz;
        while (r - l > 1) {
            final int md = (r + l) / 2;
            if (checker.test(md)) {
                r = md;
            } else {
                l = md;
            }
        }
        return r;
    }

    @SuppressWarnings("unchecked")
    private static <E> Comparator<? super E> checkNaturalOrder(Comparator<? super E> comparator) {
        return comparator != null ? comparator : (Comparator<? super E>) Comparator.naturalOrder();
    }

    private static <E> int bound(final List<? extends E> elements, final E x, final Comparator<? super E> comparator, final boolean isLower) {
        final Comparator<? super E> realComparator = checkNaturalOrder(comparator);
        return binarySearch(elements.size(), idx -> {
            int cmp = realComparator.compare(x, elements.get(idx));
            return isLower ? cmp <= 0 : cmp < 0;
        });
    }

    public static <E> int lowerBound(final List<? extends E> elements, final E x, final Comparator<? super E> comparator) {
        return bound(elements, x, comparator, true);
    }

    public static <E> int upperBound(final List<? extends E> elements, final E x, final Comparator<? super E> comparator) {
        return bound(elements, x, comparator, false);
    }

    public static <E extends Comparable<? super E>> int lowerBound(final List<? extends E> elements, final E x) {
        return binarySearch(elements.size(), idx -> x.compareTo(elements.get(idx)) <= 0);
    }

    public static <E extends Comparable<? super E>> int upperBound(final List<? extends E> elements, final E x) {
        return binarySearch(elements.size(), idx -> x.compareTo(elements.get(idx)) < 0);
    }
}
