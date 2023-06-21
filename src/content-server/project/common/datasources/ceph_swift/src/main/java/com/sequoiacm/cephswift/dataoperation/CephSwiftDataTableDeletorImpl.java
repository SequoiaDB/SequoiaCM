package com.sequoiacm.cephswift.dataoperation;

import com.sequoiacm.cephswift.CephSwiftException;
import com.sequoiacm.cephswift.dataservice.CephSwiftDataService;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataTableDeletor;
import com.sequoiacm.datasource.dataservice.ScmService;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CephSwiftDataTableDeletorImpl implements ScmDataTableDeletor {

    private static final Logger logger = LoggerFactory
            .getLogger(CephSwiftDataTableDeletorImpl.class);

    private List<String> tableNames;
    private CephSwiftDataService dataService;

    public CephSwiftDataTableDeletorImpl(List<String> tableNames, ScmService service) {
        this.tableNames = tableNames;
        this.dataService = (CephSwiftDataService) service;
    }

    @Override
    public void delete() throws ScmDatasourceException {
        Account account = dataService.createAccount();
        for (String tableName : tableNames) {
            try {
                Container container = dataService.getContainer(account, tableName);
                dataService.clearAndDeleteContainer(container);
                logger.info("clear and delete container success: containerName={}", tableName);
            }
            catch (CephSwiftException e) {
                logger.warn("clear and delete container failed:containerName={}", tableName, e);
            }
        }
    }
}
