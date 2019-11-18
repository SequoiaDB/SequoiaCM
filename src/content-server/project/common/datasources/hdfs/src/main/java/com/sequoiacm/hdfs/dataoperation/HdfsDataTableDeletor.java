package com.sequoiacm.hdfs.dataoperation;

import java.util.List;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataTableDeletor;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.hdfs.dataservice.HdfsDataService;

public class HdfsDataTableDeletor implements ScmDataTableDeletor {
    private static final Logger logger = LoggerFactory.getLogger(HdfsDataTableDeletor.class);
    private HdfsDataService service;
    private List<String> tableNames;

    public HdfsDataTableDeletor(List<String> tableNames, ScmService service) {
        this.tableNames = tableNames;
        this.service = (HdfsDataService) service;
    }

    @Override
    public void delete() throws ScmDatasourceException {
        FileSystem fs = service.getFileSystem();
        for(String tableName:tableNames) {
            try {
                fs.delete(new Path(tableName), true);
            }
            catch (Exception e) {
                logger.warn("failed remove hdfs data path:path={}", tableName, e);
            }
        }
    }

}
