package com.sequoiacm.contentserver.service;

import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import org.bson.BSONObject;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaCursor;

import javax.servlet.http.HttpServletResponse;

public interface ISiteService {
    BSONObject getSite(String  siteName) throws ScmServerException;


    MetaCursor getSiteList(BSONObject condition, long skip, long limit) throws ScmServerException;

    long countSite(BSONObject condition) throws ScmServerException;

    void getSecretFile(HttpServletResponse response, boolean isMeta) throws ScmServerException;
}
