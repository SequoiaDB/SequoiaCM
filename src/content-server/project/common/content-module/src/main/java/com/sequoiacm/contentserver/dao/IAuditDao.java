package com.sequoiacm.contentserver.dao;

import org.bson.BSONObject;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaCursor;

public interface IAuditDao {

    MetaCursor query(BSONObject matcher) throws ScmServerException;

}
