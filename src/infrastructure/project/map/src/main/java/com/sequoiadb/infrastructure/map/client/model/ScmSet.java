package com.sequoiadb.infrastructure.map.client.model;

import java.util.Set;

public interface ScmSet<E> extends Set<E> {
    ScmIterator<E> iterator(int pageSize);
}
