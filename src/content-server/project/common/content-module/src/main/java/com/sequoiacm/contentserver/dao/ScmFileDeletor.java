package com.sequoiacm.contentserver.dao;

import com.sequoiacm.exception.ScmServerException;
import org.bson.BSONObject;

public interface ScmFileDeletor {
    public BSONObject delete() throws ScmServerException;
}
