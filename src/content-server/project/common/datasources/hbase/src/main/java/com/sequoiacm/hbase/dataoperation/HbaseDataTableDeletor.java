package com.sequoiacm.hbase.dataoperation;

import java.util.List;

import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataTableDeletor;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.hbase.dataservice.HbaseDataService;

public class HbaseDataTableDeletor implements ScmDataTableDeletor {
    private HbaseDataService service;
    private List<String> tables;

    public HbaseDataTableDeletor(List<String> tables, ScmService service) {
        this.tables = tables;
        this.service = (HbaseDataService) service;
    }

    @Override
    public void delete() throws ScmDatasourceException {
        for (String table:tables) {
            service.deleteTable(table);
        }
    }

}
