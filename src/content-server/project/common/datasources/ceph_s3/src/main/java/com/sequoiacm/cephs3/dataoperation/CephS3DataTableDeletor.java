package com.sequoiacm.cephs3.dataoperation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataTableDeletor;

public class CephS3DataTableDeletor implements ScmDataTableDeletor {
    private static final Logger logger = LoggerFactory.getLogger(CephS3DataTableDeletor.class);

    @Override
    public void delete() throws ScmDatasourceException {
        throw new ScmDatasourceException("cepth s3 delete data table is not implemted");
    }

}
