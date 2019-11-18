package com.sequoiacm.cephs3.dataoperation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.cephs3.dataservice.CephS3DataService;
import com.sequoiacm.datasource.dataoperation.ScmDataDeletor;
import com.sequoiacm.datasource.dataservice.ScmService;

public class CephS3DataDeletorImpl implements ScmDataDeletor {

    private String key;
    private String bucketName;
    private CephS3DataService dataService;
    private static final Logger logger = LoggerFactory.getLogger(CephS3DataDeletorImpl.class);

    public CephS3DataDeletorImpl(String bucketName, String key, ScmService service)
            throws CephS3Exception {
        try {
            this.bucketName = bucketName;
            this.key = key;
            this.dataService = (CephS3DataService)service;
        }
        catch (Exception e) {
            logger.error("construct CephS3DataDeletorImpl failed:bucketName=" + bucketName
                    + ",key=" + key);
            throw new CephS3Exception(
                    "construct CephS3DataDeletorImpl failed:bucketName=" + bucketName + ",key="
                            + key, e);

        }
    }

    @Override
    public void delete() throws CephS3Exception {
        try {
            dataService.deleteObject(new DeleteObjectRequest(bucketName, key));
        }
        catch (CephS3Exception e) {
            logger.error("delete data failed:bucketName=" + bucketName + ",key=" + key);
            throw e;
        }
        catch (Exception e) {
            logger.error("delete data failed:bucketName=" + bucketName + ",key=" + key);
            throw new CephS3Exception(
                    "delete data failed:bucketName=" + bucketName + ",key=" + key, e);
        }
    }
}
