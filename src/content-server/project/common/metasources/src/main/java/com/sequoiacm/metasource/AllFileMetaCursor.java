package com.sequoiacm.metasource;

import org.bson.BSONObject;

import java.util.Objects;

public class AllFileMetaCursor implements MetaCursor {

    private CachePreviousCursor currentFileCursor;
    private CachePreviousCursor historyFileCursor;

    private BSONObject orderBy;

    public AllFileMetaCursor(MetaCursor currentFileCursor, MetaCursor historyFileCursor,
            BSONObject oderBy)
            throws ScmMetasourceException {
        this.currentFileCursor = new CachePreviousCursor(currentFileCursor);
        this.historyFileCursor = new CachePreviousCursor(historyFileCursor);
        this.orderBy = oderBy;
    }

    @Override
    public boolean hasNext() throws ScmMetasourceException {
        return historyFileCursor.hasNext() || currentFileCursor.hasNext();
    }

    @Override
    public BSONObject getNext() throws ScmMetasourceException {
        if (orderBy == null || orderBy.isEmpty()) {
            BSONObject c = currentFileCursor.getNextAndChose();
            if (c != null) {
                return c;
            }
            return historyFileCursor.getNextAndChose();
        }
        else {
            BSONObject currentFileCursorNext = currentFileCursor.getNext();
            BSONObject historyFileCursorNext = historyFileCursor.getNext();
            if (currentFileCursorNext == null) {
                historyFileCursor.chose();
                return historyFileCursorNext;
            }
            if (historyFileCursorNext == null) {
                currentFileCursor.chose();
                return currentFileCursorNext;
            }
            BSONObject chosenObj = compareAndChose(currentFileCursorNext, historyFileCursorNext);
            if (chosenObj == historyFileCursorNext) {
                historyFileCursor.chose();
            }
            else {
                currentFileCursor.chose();
            }
            return chosenObj;
        }
    }

    private BSONObject compareAndChose(BSONObject o1, BSONObject o2) {
        for (String sortKey : orderBy.keySet()) {
            boolean ascending = Objects.equals(1, orderBy.get(sortKey));
            Object v1 = o1.get(sortKey);
            Object v2 = o2.get(sortKey);
            if (v1 == null && v2 != null) {
                return ascending ? o1 : o2;
            }
            else if (v1 != null && v2 == null) {
                return ascending ? o2 : o1;
            }
            else if (v1 == null && v2 == null) {
                continue;
            }

            if (v1 instanceof Comparable && v2 instanceof Comparable) {
                int result = ((Comparable) v1).compareTo(v2);
                if (result < 0) {
                    // v1 < v2
                    return ascending ? o1 : o2;
                }
                else if (result > 0) {
                    // v1 > v2
                    return ascending ? o2 : o1;
                }

                // v1 == v2 忽略，比较下一个排序字段
            }
            else {
                throw new IllegalArgumentException("unable to compare filed values " + v1 + " and "
                        + v2 + ", filed=" + sortKey);
            }
        }
        // o1与o2相等或没有可排序的字段，随便返回一个
        return o1;
    }

    @Override
    public void close() {
        if (historyFileCursor != null) {
            historyFileCursor.close();
        }

        if (currentFileCursor != null) {
            currentFileCursor.close();
        }

    }

    private static class CachePreviousCursor {
        private final MetaCursor metaCursor;

        private BSONObject cache;

        public CachePreviousCursor(MetaCursor metaCursor) {
            this.metaCursor = metaCursor;
        }

        public boolean hasNext() throws ScmMetasourceException {
            return cache != null || metaCursor.hasNext();
        }

        public BSONObject getNext() throws ScmMetasourceException {
            if (cache != null) {
                return cache;
            }
            cache = metaCursor.getNext();
            return cache;
        }

        public void chose() {
            this.cache = null;
        }

        public BSONObject getNextAndChose() throws ScmMetasourceException {
            BSONObject next = getNext();
            chose();
            return next;
        }

        public void close() {
            if (metaCursor != null) {
                metaCursor.close();
            }
        }
    }
}
