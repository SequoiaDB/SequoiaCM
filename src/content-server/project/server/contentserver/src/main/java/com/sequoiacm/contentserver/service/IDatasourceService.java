package com.sequoiacm.contentserver.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.bson.BSONObject;

import com.sequoiacm.contentserver.dao.DatasourceReaderDao;
import com.sequoiacm.exception.ScmServerException;

public interface IDatasourceService {

    void deleteData(String wsName, String dataId, int dataType, long createTime)
            throws ScmServerException;

    DatasourceReaderDao readData(String workspaceName, String dataId, int dataType, long createTime,
            int readflag, OutputStream os) throws ScmServerException;

    BSONObject getDataInfo(String workspaceName, String dataId, int dataType, long createTime)
            throws ScmServerException;

    void createData(String wsName, String dataId, int dataType, long createTime, InputStream is)
            throws ScmServerException;

    void deleteDataTables(List<String> tableNames) throws ScmServerException;
}
