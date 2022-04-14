package com.sequoiacm.contentserver.service;

import com.sequoiacm.contentserver.dao.DatasourceReaderDao;
import com.sequoiacm.contentserver.model.ScmDataInfoDetail;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmSeekableDataWriter;
import com.sequoiacm.datasource.dataoperation.ScmDataReader;
import com.sequoiacm.exception.ScmServerException;
import org.bson.BSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface IDatasourceService {

    void deleteDataLocal(String wsName, String dataId, int dataType, long createTime)
            throws ScmServerException;

    // 删除指定站点数据，目前只支持删除本地站点和主站点
    void deleteData(String wsName, String dataId, int dataType, long createTime, int siteId)
            throws ScmServerException;

    DatasourceReaderDao readData(String workspaceName, String dataId, int dataType, long createTime,
            int readflag, OutputStream os) throws ScmServerException;

    BSONObject getDataInfo(String workspaceName, String dataId, int dataType, long createTime)
            throws ScmServerException;

    void createDataInLocal(String wsName, String dataId, int dataType, long createTime)
            throws ScmServerException;

    void createDataInLocal(String wsName, String dataId, int dataType, long createTime, InputStream is)
            throws ScmServerException;

    // 若本地站点在指定工作区内，数据写入本地站点，否则写入主站点
    ScmDataInfoDetail createData(String ws, InputStream data, long createTime) throws ScmServerException;

    void deleteDataTables(List<String> tableNames) throws ScmServerException;

    ScmDataReader getScmDataReader(String wsName, String dataId, int dataType, long createTime)
            throws ScmServerException, ScmDatasourceException;

    ScmSeekableDataWriter getScmSeekableDataWriter(String wsName, String dataId, int dataType,
            long createTime) throws ScmServerException;
}
