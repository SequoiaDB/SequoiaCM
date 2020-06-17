package com.sequoiacm.mq.server.common;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public class CircularIterator<E> implements Iterator<E> {
    private List<E> list;
    private ListIterator<E> it;
    private int startIndex;
    private boolean flipped;

    public CircularIterator(List<E> list, int startIndex) {
        if (list.isEmpty()) {
            throw new IllegalArgumentException("list is empty");
        }
        this.list = list;
        this.startIndex = startIndex;
        this.it = list.listIterator(startIndex);
        this.flipped = false;
    }

    @Override
    public boolean hasNext() {
        if (!it.hasNext()) {
            it = list.listIterator();
            flipped = true;
        }

        if (flipped && it.nextIndex() == startIndex) {
            return false;
        }
        return true;
    }

    @Override
    public E next() {
        if (!hasNext()) {
            throw new NoSuchElementException("iteration has no more elements");
        }
        return it.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

    public static void main(String[] args) {
        List<Integer> l = Arrays.asList(1, 2, 3, 4, 5);
        CircularIterator<Integer> itt = new CircularIterator<>(l, 0);
        while (itt.hasNext()) {
            System.out.println(itt.next());
        }
    }
}
