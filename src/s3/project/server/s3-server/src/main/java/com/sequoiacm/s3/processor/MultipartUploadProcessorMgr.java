package com.sequoiacm.s3.processor;

import com.sequoiacm.contentserver.datasourcemgr.ScmDataSourceType;
import com.sequoiacm.s3.processor.impl.MultipartUploadProcessorSeekable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class MultipartUploadProcessorMgr {

    @Autowired
    MultipartUploadProcessorSeekable processorSeekable;

    public MultipartUploadProcessor getProcessor(String datasourceType) {
        if (datasourceType.equals(ScmDataSourceType.SEQUOIADB.getName())) {
            return processorSeekable;
        }
        else {
            return null;
        }
    }
}
