package com.sequoiacm.contentserver.dao;

import org.bson.BSONObject;

import com.sequoiacm.exception.ScmServerException;

public interface IFileCreatorDao {
    BSONObject insert() throws ScmServerException;

    void processException();

    String getWorkspaceName();
}
