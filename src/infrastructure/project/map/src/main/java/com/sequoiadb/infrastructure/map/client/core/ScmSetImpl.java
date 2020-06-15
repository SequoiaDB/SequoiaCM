package com.sequoiadb.infrastructure.map.client.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import com.sequoiadb.infrastructure.map.CommonHelper;
import com.sequoiadb.infrastructure.map.ScmMapRuntimeException;
import com.sequoiadb.infrastructure.map.ScmMapServerException;
import com.sequoiadb.infrastructure.map.client.ScmCursor;
import com.sequoiadb.infrastructure.map.client.model.ScmIterator;
import com.sequoiadb.infrastructure.map.client.model.ScmMap;
import com.sequoiadb.infrastructure.map.client.model.ScmSet;

class ScmSetImpl<E> implements ScmSet<E> {
    private ScmMap<?, ?> map;
    private boolean isKeyType;

    public ScmSetImpl(ScmMap<?, ?> map, boolean isKeyType) {
        this.map = map;
        this.isKeyType = isKeyType;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public Object[] toArray() {
        return set().toArray();

    }

    private Set<E> set() {
        ScmCursor<?> cursor = null;
        try {
            Set<E> set = new HashSet<>();
            if (isKeyType) {
                cursor = map.listKey(null, null, 0, -1);
                while (cursor.hasNext()) {
                    set.add((E) cursor.getNext());
                }
            }
            else {
                cursor = map.listEntry(null, null, 0, -1);
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

    @Override
    public <T> T[] toArray(T[] a) {
        return set().toArray(a);
    }

    @Override
    public ScmIterator<E> iterator() {
        return new ScmIteratorImpl<E>(map, isKeyType);
    }

    @Override
    public ScmIterator<E> iterator(int pageSize) {
        return new ScmIteratorImpl<E>(map, pageSize, isKeyType);
    }

    @Override
    public boolean remove(Object o) {
        if (isKeyType) {
            return map.remove(o) != null;
        }
        if (o != null && Entry.class.isAssignableFrom(o.getClass())) {
            Entry<?, ?> entry = (Entry<?, ?>) o;
            return map.remove(entry.getKey()) != null;
        }
        return false;

    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public String toString() {
        return set().toString();
    }

    @Override
    public boolean contains(Object o) {
        if (isKeyType) {
            return map.containsKey(o);
        }
        throw new UnsupportedOperationException("entry set unsupport contains");
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if (isKeyType) {
            return map.containKeySet(c);
        }
        throw new UnsupportedOperationException("entry set unsupport containsAll");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if (isKeyType) {
            return map.retainKeySet(c);
        }
        throw new UnsupportedOperationException("entry set unsupport retainAll");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (isKeyType) {
            return map.removeKeySet(c);
        }
        throw new UnsupportedOperationException("entry set unsupport removeAll");
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }
}
