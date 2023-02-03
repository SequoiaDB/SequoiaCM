package com.sequoiacm.contentserver.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.sequoiacm.datasource.metadata.ScmLocation;
import org.bson.BSONObject;

import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.contentserver.dao.DatasourceReaderDao;
import com.sequoiacm.contentserver.model.ScmDataInfoDetail;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.common.ScmDataWriterContext;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataReader;
import com.sequoiacm.datasource.dataoperation.ScmSeekableDataWriter;
import com.sequoiacm.exception.ScmServerException;

public interface IDatasourceService {

    void deleteDataLocal(String wsName, ScmDataInfo dataInfo)
            throws ScmServerException;

    // 删除指定站点数据
    void deleteData(String wsName, ScmDataInfo dataInfo, int siteId)
            throws ScmServerException;

    DatasourceReaderDao readData(String workspaceName, String dataId, int dataType, long createTime,
            int readflag, OutputStream os, int wsVersion, String tableName)
            throws ScmServerException;

    BSONObject getDataInfo(String workspaceName, ScmDataInfo dataInfo)
            throws ScmServerException;

    void createDataInLocal(String wsName, ScmDataInfo dataInfo, ScmDataWriterContext context)
            throws ScmServerException;

    void createDataInLocal(String wsName, ScmDataInfo dataInfo, InputStream is,
            ScmDataWriterContext context)
            throws ScmServerException;

    // 若本地站点在指定工作区内，数据写入本地站点，否则写入主站点
    ScmDataInfoDetail createData(String ws, InputStream data, long createTime)
            throws ScmServerException;

    void deleteDataTables(List<String> tableNames, String wsName, ScmLocation location) throws ScmServerException;

    ScmDataReader getScmDataReader(String wsName, String dataId, int dataType, long createTime, int wsVersion, String tableName)
            throws ScmServerException, ScmDatasourceException;

    ScmSeekableDataWriter getScmSeekableDataWriter(String wsName, String dataId, int dataType,
            long createTime, int wsVersion, String tableName) throws ScmServerException;

    void deleteDataInSiteList(String wsName, String dataId, int type, long createTime,
            List<ScmFileLocation> siteList) throws ScmServerException;
}
