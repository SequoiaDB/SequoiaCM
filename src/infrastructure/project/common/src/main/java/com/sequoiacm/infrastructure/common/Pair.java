package com.sequoiacm.infrastructure.common;

public class Pair<V> {
    private V oldValue;
    private V overflowValue;

    public Pair(V oldValue, V overflowValue) {
        this.oldValue = oldValue;
        this.overflowValue = overflowValue;
    }

    public V getOldValue() {
        return oldValue;
    }

    public V getOverflowValue() {
        return overflowValue;
    }

    @Override
    public String toString() {
        return "Pair{" +
                "oldValue=" + oldValue +
                ", overflowValue=" + overflowValue +
                '}';
    }
}
