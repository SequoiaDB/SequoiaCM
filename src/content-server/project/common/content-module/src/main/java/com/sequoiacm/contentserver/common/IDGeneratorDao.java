package com.sequoiacm.contentserver.common;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.ScmMetasourceException;

import java.util.List;

public interface IDGeneratorDao {
    long getNewId(String type) throws ScmServerException;

    List<Long> getNewIds(String type, int count) throws ScmServerException;

    void ensureTable() throws ScmServerException, ScmMetasourceException;
}
