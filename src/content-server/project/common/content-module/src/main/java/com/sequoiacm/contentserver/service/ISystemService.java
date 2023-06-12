package com.sequoiacm.contentserver.service;

import org.bson.BSONObject;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaCursor;

public interface ISystemService {

    BSONObject getConfs(String[] keys) throws ScmServerException;

    /*void getNodeList(PrintWriter writer, BSONObject condition) throws ScmServerException;*/
    MetaCursor getNodeList(BSONObject condition) throws ScmServerException;
    
}
