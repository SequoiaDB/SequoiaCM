package com.sequoiacm.cephswift.dataservice;

import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.client.factory.AccountFactory;
import org.javaswift.joss.client.factory.AuthenticationMethod;
import org.javaswift.joss.exception.CommandException;
import org.javaswift.joss.instructions.UploadInstructions;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cephswift.CephSwiftException;
import com.sequoiacm.cephswift.dataoperation.CephSwiftCommonDefine;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.infrastructure.crypto.AuthInfo;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;

public class CephSwiftDataService extends ScmService {

    private AccountConfig config;
    private static final Logger logger = LoggerFactory.getLogger(CephSwiftDataService.class);

    public CephSwiftDataService(int siteId, ScmSiteUrl siteUrl) throws CephSwiftException {
        super(siteId, siteUrl);
        try {
            config = new AccountConfig();
            config.setUsername(siteUrl.getUser());
            AuthInfo auth = ScmFilePasswordParser.parserFile(siteUrl.getPassword());
            config.setPassword(auth.getPassword());
            config.setAuthUrl(siteUrl.getUrls().get(0));
            config.setAuthenticationMethod(AuthenticationMethod.BASIC);

            // try connect to swift.
            createAccount();
        }
        catch (CommandException e) {
            throw new CephSwiftException(e.getHttpStatusCode(), e.getError().toString(),
                    "create CephSwiftDataService failed:siteId=" + siteId + ",siteUrl=" + siteUrl,
                    e);
        }
        catch (Exception e) {
            throw new CephSwiftException(
                    "create CephSwiftDataService failed:siteId=" + siteId + ",siteUrl=" + siteUrl,
                    e);
        }
    }

    public void uploadObject(StoredObject obj, UploadInstructions uploadInstructions)
            throws CephSwiftException {
        try {
            obj.uploadObject(uploadInstructions);
        }
        catch (CommandException e) {
            throw new CephSwiftException(e.getHttpStatusCode(), e.getError().toString(),
                    "upload object failed:siteId=" + siteId + ",object=" + obj.getName(), e);
        }
        catch (Exception e) {
            throw new CephSwiftException(
                    "upload object failed:siteId=" + siteId + ",object=" + obj.getName(), e);
        }
    }

    public StoredObject getObject(Container container, String objectName)
            throws CephSwiftException {
        try {
            return container.getObject(objectName);
        }
        catch (CommandException e) {
            throw new CephSwiftException(e.getHttpStatusCode(), e.getError().toString(),
                    "get object failed:siteId=" + siteId + ",container=" + container.getName()
                    + ",object=" + objectName,
                    e);
        }
        catch (Exception e) {
            throw new CephSwiftException("get object failed:siteId=" + siteId + ",container="
                    + container.getName() + ",object=" + objectName, e);
        }
    }

    public StoredObject getObjectSegment(Container container, String objectName, int part)
            throws CephSwiftException {
        try {
            return container.getObjectSegment(objectName, part);
        }
        catch (CommandException e) {
            throw new CephSwiftException(e.getHttpStatusCode(), e.getError().toString(),
                    "get object Segment failed:siteId=" + siteId + ",container="
                            + container.getName() + ",object=" + objectName + ",part=" + part,
                            e);
        }
        catch (Exception e) {
            throw new CephSwiftException(
                    "get object Segment failed:siteId=" + siteId + ",container="
                            + container.getName() + ",object=" + objectName + ",part=" + part,
                            e);
        }
    }

    public Account createAccount() throws CephSwiftException {
        try {
            return new AccountFactory(config).createAccount();
        }
        catch (CommandException e) {
            throw new CephSwiftException(e.getHttpStatusCode(), String.valueOf(e.getError()),
                    "get account failed:siteId=" + siteId, e);
        }
        catch (Exception e) {
            throw new CephSwiftException("get account failed:siteId=" + siteId, e);
        }
    }

    public Container getContainer(Account account, String name) throws CephSwiftException {
        try {
            return account.getContainer(name);
        }
        catch (CommandException e) {
            throw new CephSwiftException(e.getHttpStatusCode(), e.getError().toString(),
                    "get container failed:siteId=" + siteId + ",container=" + name, e);
        }
        catch (Exception e) {
            throw new CephSwiftException(
                    "get container failed:siteId=" + siteId + ",container=" + name, e);
        }
    }

    public boolean createContainer(Container c) throws CephSwiftException {
        try {
            logger.info("creating container:containerName=" + c.getName());
            c.create();
            return true;
        }
        catch (CommandException e) {
            if (!e.getError().toString().equals(CephSwiftException.ERR_ENTITY_ALREADY_EXISTS)) {
                throw new CephSwiftException(e.getHttpStatusCode(), e.getError().toString(),
                        "create container failed:siteId=" + siteId + ",container=" + c.getName(),
                        e);
            }
            return false;
        }
        catch (Exception e) {
            throw new CephSwiftException(
                    "create container failed:siteId=" + siteId + ",container=" + c.getName(), e);
        }
    }

    public long getObjectSize(Container container, String objName) throws CephSwiftException {
        try {
            StoredObject obj = container.getObject(objName);
            String fileSize = (String) obj.getMetadata(CephSwiftCommonDefine.FILE_SIZE);
            if (fileSize == null) {
                // normal object,return objectLength
                return obj.getContentLength();
            }
            // return manifest object sizeMeta
            return Integer.valueOf(fileSize);
        }
        catch (CommandException e) {
            throw new CephSwiftException(
                    e.getHttpStatusCode(), e.getError().toString(), "get object size failed:siteId="
                            + siteId + ",container=" + container.getName() + "object=" + objName,
                            e);
        }
        catch (Exception e) {
            throw new CephSwiftException("get object size failed:siteId=" + siteId + ",container="
                    + container.getName() + "object=" + objName, e);
        }
    }

    public void deleteObject(StoredObject object) throws CephSwiftException {
        try {
            object.delete();
        }
        catch (CommandException e) {
            if (!e.getError().toString().equals(CephSwiftException.ERR_ENTITY_DOES_NOT_EXIST)) {
                throw new CephSwiftException(e.getHttpStatusCode(), e.getError().toString(),
                        "delete object failed:siteId=" + siteId + ",object=" + object.getName(), e);
            }
        }
        catch (Exception e) {
            throw new CephSwiftException(
                    "delete object failed:siteId=" + siteId + ",object=" + object.getName(), e);
        }
    }

    public Object getMeta(StoredObject obj, String key) throws CephSwiftException {
        try {
            return obj.getMetadata(key);
        }
        catch (CommandException e) {
            throw new CephSwiftException(e.getHttpStatusCode(), e.getError().toString(),
                    "get metadata failed:siteId=" + siteId + ",object=" + obj.getName() + ",key="
                            + key,
                            e);
        }
        catch (Exception e) {
            throw new CephSwiftException("get metadata failed:siteId=" + siteId + ",object="
                    + obj.getName() + ",key=" + key, e);
        }
    }

    public boolean isObjectExist(StoredObject obj) throws CephSwiftException {
        try {
            return obj.exists();
        }
        catch (CommandException e) {
            throw new CephSwiftException(e.getHttpStatusCode(), e.getError().toString(),
                    "check objet status failed:siteId=" + siteId + ",object=" + obj.getName(), e);
        }
        catch (Exception e) {
            throw new CephSwiftException(
                    "check objet status failed:siteId=" + siteId + ",object=" + obj.getName(), e);
        }
    }

    public void setAndSaveMetadata(StoredObject obj, String key, Object value)
            throws CephSwiftException {
        try {
            obj.setAndSaveMetadata(key, value);
        }
        catch (CommandException e) {
            throw new CephSwiftException(e.getHttpStatusCode(), e.getError().toString(),
                    "set and save metadata failed:siteId=" + siteId + ",object=" + obj.getName(),
                    e);
        }
        catch (Exception e) {
            throw new CephSwiftException(
                    "set and save metadata failed:siteId=" + siteId + ",object=" + obj.getName(),
                    e);
        }
    }

    @Override
    public String getType() {
        return "ceph_swift";
    }

    @Override
    public void clear() {
        config = null;
    }

}
