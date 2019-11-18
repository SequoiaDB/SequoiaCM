package com.sequoiacm.cephswift.dataoperation;

import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataTableDeletor;

public class CephSwiftDataTableDeletorImpl implements ScmDataTableDeletor {

    public CephSwiftDataTableDeletorImpl() {
    }

    @Override
    public void delete() throws ScmDatasourceException {
        throw new ScmDatasourceException("cepth swift delete data table is not implemted");
    }

}
