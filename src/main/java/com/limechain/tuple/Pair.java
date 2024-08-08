package com.limechain.tuple;


import java.util.Collection;
import java.util.Iterator;

public final class Pair<A, B> {

    private static final long serialVersionUID = 2438099850625502138L;

    private static final int SIZE = 2;

    private final A val0;
    private final B val1;


    public static <A, B> Pair<A, B> with(final A value0, final B value1) {
        return new Pair<A, B>(value0, value1);
    }


    /**
     * <p>
     * Create tuple from array. Array has to have exactly two elements.
     * </p>
     *
     * @param <X>   the array component type
     * @param array the array to be converted to a tuple
     * @return the tuple
     */
    public static <X> Pair<X, X> fromArray(final X[] array) {
        if (array == null) {
            throw new IllegalArgumentException("Array cannot be null");
        }
        if (array.length != 2) {
            throw new IllegalArgumentException(
                    "Array must have exactly 2 elements in order to create a Pair. Size is " + array.length);
        }
        return new Pair<X, X>(array[0], array[1]);
    }


    /**
     * <p>
     * Create tuple from collection. Collection has to have exactly two elements.
     * </p>
     *
     * @param <X>        the collection component type
     * @param collection the collection to be converted to a tuple
     * @return the tuple
     */
    public static <X> Pair<X, X> fromCollection(final Collection<X> collection) {
        return fromIterable(collection);
    }


    /**
     * <p>
     * Create tuple from iterable. Iterable has to have exactly two elements.
     * </p>
     *
     * @param <X>      the iterable component type
     * @param iterable the iterable to be converted to a tuple
     * @return the tuple
     */
    public static <X> Pair<X, X> fromIterable(final Iterable<X> iterable) {
        return fromIterable(iterable, 0, true);
    }


    /**
     * <p>
     * Create tuple from iterable, starting from the specified index. Iterable
     * can have more (or less) elements than the tuple to be created.
     * </p>
     *
     * @param <X>      the iterable component type
     * @param iterable the iterable to be converted to a tuple
     * @return the tuple
     */
    public static <X> Pair<X, X> fromIterable(final Iterable<X> iterable, int index) {
        return fromIterable(iterable, index, false);
    }


    private static <X> Pair<X, X> fromIterable(final Iterable<X> iterable, int index, final boolean exactSize) {

        if (iterable == null) {
            throw new IllegalArgumentException("Iterable cannot be null");
        }

        boolean tooFewElements = false;

        X element0 = null;
        X element1 = null;

        final Iterator<X> iter = iterable.iterator();

        int i = 0;
        while (i < index) {
            if (iter.hasNext()) {
                iter.next();
            } else {
                tooFewElements = true;
            }
            i++;
        }

        if (iter.hasNext()) {
            element0 = iter.next();
        } else {
            tooFewElements = true;
        }

        if (iter.hasNext()) {
            element1 = iter.next();
        } else {
            tooFewElements = true;
        }

        if (tooFewElements && exactSize) {
            throw new IllegalArgumentException("Not enough elements for creating a Pair (2 needed)");
        }

        if (iter.hasNext() && exactSize) {
            throw new IllegalArgumentException(
                    "Iterable must have exactly 2 available elements in order to create a Pair.");
        }

        return new Pair<X, X>(element0, element1);

    }

    public Pair(final A value0,
                final B value1) {
        this.val0 = value0;
        this.val1 = value1;
    }


    public A getValue0() {
        return this.val0;
    }


    public B getValue1() {
        return this.val1;
    }

    public int getSize() {
        return SIZE;
    }


    public <X> Pair<X, B> setAt0(final X value) {
        return new Pair<X, B>(
                value, this.val1);
    }

    public <X> Pair<A, X> setAt1(final X value) {
        return new Pair<A, X>(
                this.val0, value);
    }

}