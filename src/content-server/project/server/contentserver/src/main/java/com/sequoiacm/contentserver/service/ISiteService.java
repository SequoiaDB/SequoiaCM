package com.sequoiacm.contentserver.service;

import org.bson.BSONObject;

import com.sequoiacm.contentserver.exception.ScmServerException;
import com.sequoiacm.metasource.MetaCursor;

public interface ISiteService {
    BSONObject getSite(String  siteName) throws ScmServerException;

    /*void getSiteList(PrintWriter writer, BSONObject condition) throws ScmServerException;*/
    MetaCursor getSiteList(BSONObject condition) throws ScmServerException;
}
