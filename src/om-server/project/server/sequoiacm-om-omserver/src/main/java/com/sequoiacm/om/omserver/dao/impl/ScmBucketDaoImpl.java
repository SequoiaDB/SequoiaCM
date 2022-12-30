package com.sequoiacm.om.omserver.dao.impl;

import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmPrivilege;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.privilege.ScmAllWorkspaceResource;
import com.sequoiacm.client.element.privilege.ScmBucketResource;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmWorkspaceResource;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.om.omserver.common.CommonUtil;
import com.sequoiacm.om.omserver.common.ScmFileUtil;
import com.sequoiacm.om.omserver.dao.ScmBucketDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.module.OmBucketDetail;
import com.sequoiacm.om.omserver.module.OmFileBasic;
import com.sequoiacm.om.omserver.module.OmFileInfo;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class ScmBucketDaoImpl implements ScmBucketDao {

    private ScmOmSession session;

    public ScmBucketDaoImpl(ScmOmSession session) {
        this.session = session;
    }

    @Override
    public long countFile(String bucketName, BSONObject condition) throws ScmInternalException {
        ScmSession con = session.getConnection();
        try {
            ScmBucket bucket = ScmFactory.Bucket.getBucket(con, bucketName);
            return bucket.countFile(condition);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), "failed to count bucket file: bucket="
                    + bucketName + ", detail=" + e.getMessage(), e);
        }
    }

    @Override
    public List<OmFileBasic> listFile(String bucketName, BSONObject filter, BSONObject orderBy,
            long skip, int limit) throws ScmInternalException {
        ScmSession con = session.getConnection();
        ScmCursor<ScmFileBasicInfo> cursor = null;
        try {
            ScmBucket bucket = ScmFactory.Bucket.getBucket(con, bucketName);
            cursor = bucket.listFile(filter, orderBy, skip, limit);
            List<OmFileBasic> res = new ArrayList<>();
            while (cursor.hasNext()) {
                ScmFileBasicInfo fileBasicInfo = cursor.getNext();
                if (fileBasicInfo.isDeleteMarker()) {
                    res.add(ScmFileUtil.transformToFileBasicInfo(fileBasicInfo));
                    continue;
                }
                ScmFile file = bucket.getFile(fileBasicInfo.getFileName(),
                        fileBasicInfo.getMajorVersion(), fileBasicInfo.getMinorVersion());
                res.add(ScmFileUtil.transformToFileBasicInfo(file));
            }
            return res;
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), "failed to list file in bucket: bucket="
                    + bucketName + ", detail=" + e.getMessage(), e);
        }
        finally {
            CommonUtil.closeResource(cursor);
        }
    }

    @Override
    public OmBucketDetail getBucketDetail(String bucketName) throws ScmInternalException {
        ScmSession con = session.getConnection();
        try {
            ScmBucket bucket = ScmFactory.Bucket.getBucket(con, bucketName);
            return transformToBucketDetail(bucket);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get bucket detail, " + e.getMessage(), e);
        }
    }

    @Override
    public void createFile(String bucketName, OmFileInfo fileInfo, BSONObject uploadConfig,
            InputStream is) throws ScmInternalException {
        ScmSession con = session.getConnection();
        try {
            ScmBucket bucket = ScmFactory.Bucket.getBucket(con, bucketName);
            ScmFile scmFile = bucket.createFile(fileInfo.getName());
            ScmFileUtil.createFile(scmFile, fileInfo, uploadConfig, is);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to create file in bucket, " + e.getMessage(), e);
        }
    }

    @Override
    public ScmBucket createBucket(ScmWorkspace workspace, String bucketName)
            throws ScmInternalException {
        try {
            return ScmFactory.Bucket.createBucket(workspace, bucketName);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), "failed to create bucket, bucketName="
                    + bucketName + ", detail=" + e.getMessage(), e);
        }
    }

    @Override
    public void deleteBucket(String bucketName) throws ScmInternalException {
        try {
            ScmFactory.Bucket.deleteBucket(session.getConnection(), bucketName);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), "failed to delete bucket, bucketName="
                    + bucketName + ", detail=" + e.getMessage(), e);
        }
    }

    @Override
    public void enableVersionControl(String bucketName) throws ScmInternalException {
        try {
            ScmBucket bucket = ScmFactory.Bucket.getBucket(session.getConnection(), bucketName);
            bucket.enableVersionControl();
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to enable bucket version control, bucketName=" + bucketName
                            + ", detail=" + e.getMessage(),
                    e);
        }
    }

    @Override
    public void suspendVersionControl(String bucketName) throws ScmInternalException {
        try {
            ScmBucket bucket = ScmFactory.Bucket.getBucket(session.getConnection(), bucketName);
            bucket.suspendVersionControl();
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to suspend bucket version control, bucketName=" + bucketName
                            + ", detail=" + e.getMessage(),
                    e);
        }
    }

    @Override
    public long countBucket(BSONObject filter) throws ScmInternalException {
        try {
            return ScmFactory.Bucket.countBucket(session.getConnection(), filter);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get bucket count, " + e.getMessage(), e);
        }
    }

    private OmBucketDetail transformToBucketDetail(ScmBucket bucket) {
        OmBucketDetail omBucketDetail = new OmBucketDetail();
        omBucketDetail.setId(bucket.getId());
        omBucketDetail.setName(bucket.getName());
        omBucketDetail.setWorkspace(bucket.getWorkspace());
        omBucketDetail.setVersionStatus(bucket.getVersionStatus().name());
        omBucketDetail.setCreateUser(bucket.getCreateUser());
        omBucketDetail.setCreateTime(bucket.getCreateTime());
        omBucketDetail.setUpdateUser(bucket.getUpdateUser());
        omBucketDetail.setUpdateTime(bucket.getUpdateTime());
        return omBucketDetail;
    }

    @Override
    public List<OmBucketDetail> listBucket(BSONObject condition, BSONObject orderBy, long skip,
            long limit) throws ScmInternalException {
        List<OmBucketDetail> bucketList = new ArrayList<>();
        ScmSession con = session.getConnection();
        ScmCursor<ScmBucket> cursor = null;
        try {
            cursor = ScmFactory.Bucket.listBucket(con, condition, orderBy, skip, limit);
            while (cursor.hasNext()) {
                ScmBucket bucket = cursor.getNext();
                bucketList.add(transformToBucketDetail(bucket));
            }
            return bucketList;
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), e.getMessage(), e);
        }
        finally {
            CommonUtil.closeResource(cursor);
        }
    }

    @Override
    public Set<String> getUserAccessibleBuckets(String username) throws ScmInternalException {
        Set<String> buckets = new TreeSet<>();
        ScmSession con = session.getConnection();
        try {
            ScmUser user = ScmFactory.User.getUser(con, username);
            Collection<ScmRole> roles = user.getRoles();
            for (ScmRole role : roles) {
                List<ScmPrivilege> roleRelatedPrivileges = getRoleRelatedPrivileges(role);
                for (ScmPrivilege privilege : roleRelatedPrivileges) {
                    buckets.addAll(getPrivilegeRelatedBuckets(privilege));
                    ScmResource resource = privilege.getResource();
                    if (resource instanceof ScmAllWorkspaceResource) {
                        return buckets;
                    }
                }
            }
            return buckets;
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), e.getMessage(), e);
        }
    }

    private List<ScmPrivilege> getRoleRelatedPrivileges(ScmRole role) throws ScmException {
        List<ScmPrivilege> roleRelatedPrivilege = new ArrayList<>();
        ScmSession con = session.getConnection();
        ScmCursor<ScmPrivilege> cursor = null;
        try {
            cursor = ScmFactory.Privilege.listPrivileges(con, role);
            while (cursor.hasNext()) {
                ScmPrivilege scmPrivilege = cursor.getNext();
                roleRelatedPrivilege.add(scmPrivilege);
            }
            return roleRelatedPrivilege;
        }
        finally {
            CommonUtil.closeResource(cursor);
        }
    }

    private Set<String> getPrivilegeRelatedBuckets(ScmPrivilege privilege) throws ScmException {
        Set<String> buckets = new HashSet<>();
        List<ScmPrivilegeType> privilegeTypes = privilege.getPrivilegeTypes();
        if (!privilegeTypes.contains(ScmPrivilegeType.READ)
                && !privilegeTypes.contains(ScmPrivilegeType.ALL)) {
            return buckets;
        }

        ScmResource resource = privilege.getResource();
        if (!(resource instanceof ScmAllWorkspaceResource
                || resource instanceof ScmWorkspaceResource
                || resource instanceof ScmBucketResource)) {
            return buckets;
        }

        BSONObject condition = new BasicBSONObject();
        if (resource instanceof ScmBucketResource) {
            String bucketResource = resource.toStringFormat();
            String[] array = bucketResource.split(":");
            if (array.length != 2) {
                throw new ScmInvalidArgumentException(
                        "bucket's resource is invalid:resource=" + resource);
            }
            condition.put(FieldName.Bucket.NAME, array[1]);
        }
        else if (resource instanceof ScmWorkspaceResource) {
            condition.put(FieldName.Bucket.WORKSPACE, resource.toStringFormat());
        }

        ScmSession con = session.getConnection();
        ScmCursor<ScmBucket> cursor = null;
        try {
            cursor = ScmFactory.Bucket.listBucket(con, condition, null, 0, -1);
            while (cursor.hasNext()) {
                buckets.add(cursor.getNext().getName());
            }
            return buckets;
        }
        finally {
            CommonUtil.closeResource(cursor);
        }
    }
}
