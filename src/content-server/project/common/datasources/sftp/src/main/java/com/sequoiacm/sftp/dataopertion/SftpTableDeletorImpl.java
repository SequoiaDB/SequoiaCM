package com.sequoiacm.sftp.dataopertion;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataTableDeletor;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.sftp.dataservice.SftpDataService;

public class SftpTableDeletorImpl implements ScmDataTableDeletor {
    private static final Logger logger = LoggerFactory.getLogger(ScmDataTableDeletor.class);

    private List<String> tableNames;
    private SftpDataService service;

    public SftpTableDeletorImpl(List<String> tableNames, ScmService service) {
        this.tableNames = tableNames;
        this.service = (SftpDataService) service;
    }

    @Override
    public void delete() throws ScmDatasourceException {
        throw new ScmDatasourceException("sftp delete data table is not implemented");
    }
}
