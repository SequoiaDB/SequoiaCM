package com.sequoiadb.infrastructure.map.client.core;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiadb.infrastructure.map.CommonDefine;
import com.sequoiadb.infrastructure.map.CommonHelper;
import com.sequoiadb.infrastructure.map.ScmMapRuntimeException;
import com.sequoiadb.infrastructure.map.ScmMapServerException;
import com.sequoiadb.infrastructure.map.client.ScmCursor;
import com.sequoiadb.infrastructure.map.client.model.ScmEntry;
import com.sequoiadb.infrastructure.map.client.model.ScmIterator;
import com.sequoiadb.infrastructure.map.client.model.ScmMap;

class ScmIteratorImpl<E> implements ScmIterator<E> {
    private ScmMap<?, ?> map;
    private Iterator<E> iterator;
    private long pageSize = 1000;
    private boolean isKeyType;
    private long nexeIndex = 0;
    private E nextValue;
    private boolean nextValueState = false;;

    public ScmIteratorImpl(ScmMap<?, ?> map, int pageSize, boolean isKeyType) {
        this.map = map;
        this.pageSize = pageSize;
        this.isKeyType = isKeyType;
        iterator = set().iterator();
    }

    public ScmIteratorImpl(ScmMap<?, ?> map, boolean isKeyType) {
        this(map, 1000, isKeyType);
    }

    @Override
    public boolean hasNext() {
        if (!iterator.hasNext()) {
            iterator = set().iterator();
            return iterator.hasNext();
        }
        return true;
    }

    @Override
    public E next() {
        try {
            nextValue = iterator.next();
        }
        catch (NoSuchElementException e) {
            if (hasNext()) {
                return next();
            }
            throw e;
        }
        nextValueState = true;
        nexeIndex++;
        return nextValue;
    }

    @Override
    public void remove() {
        if (nextValueState) {
            if (isKeyType) {
                map.remove(nextValue);
            }
            else {
                ScmEntry<?, ?> entry = (ScmEntry<?, ?>) nextValue;
                map.remove(entry.getKey());
            }
            nextValueState = false;
            return;
        }
        // not next() || repeat remove()
        throw new IllegalStateException();
    }

    private Set<E> set() {
        ScmCursor<?> cursor = null;
        try {
            Set<E> set = new HashSet<>();
            BSONObject orderby = new BasicBSONObject(CommonDefine.FieldName.KEY, 1);
            if (isKeyType) {
                cursor = map.listKey(null, orderby, nexeIndex, pageSize);
                while (cursor.hasNext()) {
                    set.add((E) cursor.getNext());
                }
            }
            else {
                cursor = map.listEntry(null, orderby, nexeIndex, pageSize);
                while (cursor.hasNext()) {
                    set.add((E) cursor.getNext());
                }
            }
            return set;
        }
        catch (ScmMapServerException e) {
            throw new ScmMapRuntimeException(e);
        }
        finally {
            CommonHelper.close(cursor);
        }
    }

}
