package com.sequoiacm.datasource.dataoperation;

import java.util.Date;
import java.util.List;

import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmLocation;

public interface ScmDataOpFactory {
    ScmDataWriter createWriter(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws ScmDatasourceException;

    ScmDataReader createReader(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws ScmDatasourceException;

    ScmDataDeletor createDeletor(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws ScmDatasourceException;

    ScmBreakpointDataWriter createBreakpointWriter(ScmLocation location, ScmService service,
            String wsName, String fileName, String dataId,
            Date createTime, boolean createData) throws ScmDatasourceException;

    ScmDataTableDeletor createDataTableDeletor(List<String> tableNames, ScmService service)
            throws ScmDatasourceException;
}
