package com.sequoiacm.cephswift.dataoperation;

import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cephswift.CephSwiftException;
import com.sequoiacm.cephswift.dataservice.CephSwiftDataService;
import com.sequoiacm.datasource.dataoperation.ScmDataDeletor;
import com.sequoiacm.datasource.dataservice.ScmService;

public class CephSwiftDataDeletorImpl implements ScmDataDeletor {
    private static final Logger logger = LoggerFactory.getLogger(CephSwiftDataDeletorImpl.class);
    private String containerName;
    private String objectName;
    private CephSwiftDataService dataService;

    public CephSwiftDataDeletorImpl(String containerName, String objectName, ScmService service)
            throws CephSwiftException {
        try {
            this.containerName = containerName;
            this.objectName = objectName;
            this.dataService = (CephSwiftDataService)service;
        }
        catch (Exception e) {
            logger.error("construct CephSwiftDataDeletorImpl failed:container=" + containerName
                    + ",object=" + objectName);
            throw new CephSwiftException(
                    "construct CephSwiftDataDeletorImpl failed:container=" + containerName
                    + ",object=" + objectName, e);
        }
    }

    @Override
    public void delete() throws CephSwiftException {
        try {
            Account account = dataService.createAccount();
            Container container = dataService.getContainer(account, containerName);
            StoredObject object = dataService.getObject(container, objectName);
            String countStr;

            countStr = (String) dataService.getMeta(object, CephSwiftCommonDefine.SEGMENT_COUNT);

            dataService.deleteObject(object);
            if (countStr != null) {
                int count = Integer.valueOf(countStr);
                for (int i = 1; i <= count; i++) {
                    StoredObject segObject = dataService.getObjectSegment(container, objectName, i);
                    dataService.deleteObject(segObject);
                }
            }
        }
        catch (CephSwiftException e) {
            logger.error("delete data failed:container=" + containerName + ",object=" + objectName);
            throw e;
        }
        catch (Exception e) {
            logger.error("delete data failed:container=" + containerName + ",object=" + objectName);
            throw new CephSwiftException(
                    "delete data failed:container=" + containerName + ",object=" + objectName, e);
        }
    }

}
