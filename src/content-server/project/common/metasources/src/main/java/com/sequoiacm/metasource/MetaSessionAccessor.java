package com.sequoiacm.metasource;

import java.util.Date;

public interface MetaSessionAccessor extends MetaAccessor {
    public void delete(String sessionId) throws ScmMetasourceException;
    public boolean updateDate(String sessionId, Date date) throws ScmMetasourceException;
}