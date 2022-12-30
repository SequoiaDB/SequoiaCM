package com.sequoiacm.contentserver.service;

import org.bson.BSONObject;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaCursor;

public interface ISiteService {
    BSONObject getSite(String  siteName) throws ScmServerException;


    MetaCursor getSiteList(BSONObject condition, long skip, long limit) throws ScmServerException;

    long countSite(BSONObject condition) throws ScmServerException;
}
