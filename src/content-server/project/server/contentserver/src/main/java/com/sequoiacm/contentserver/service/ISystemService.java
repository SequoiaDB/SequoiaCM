package com.sequoiacm.contentserver.service;

import org.bson.BSONObject;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaCursor;

public interface ISystemService {
    BSONObject reloadSiteBizConf(int siteId, boolean isMetadataOnly) throws ScmServerException;

    BSONObject reloadNodeBizConf(int nodeId, boolean isMetadataOnly) throws ScmServerException;

    BSONObject reloadAllNodeBizConf(boolean isMetadataOnly) throws ScmServerException;

    BSONObject getConfs(String[] keys) throws ScmServerException;

    /*void getNodeList(PrintWriter writer, BSONObject condition) throws ScmServerException;*/
    MetaCursor getNodeList(BSONObject condition) throws ScmServerException;
    
}
