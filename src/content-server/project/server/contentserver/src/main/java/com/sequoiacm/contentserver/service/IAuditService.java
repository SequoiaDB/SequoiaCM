package com.sequoiacm.contentserver.service;

import org.bson.BSONObject;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaCursor;

public interface IAuditService {

    MetaCursor getList(BSONObject matcher) throws ScmServerException;
}
