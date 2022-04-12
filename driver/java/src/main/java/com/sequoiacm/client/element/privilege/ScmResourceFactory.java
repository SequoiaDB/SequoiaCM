package com.sequoiacm.client.element.privilege;

import org.bson.BSONObject;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.FieldName;

/**
 * Resource factory.
 */
public class ScmResourceFactory {

    /**
     * Create a workspace resource with specified workspace name.
     *
     * @param workspaceName
     *            workspace name.
     * @return workspace resource.
     */
    public static ScmResource createWorkspaceResource(String workspaceName) {
        return new ScmWorkspaceResource(workspaceName);
    }

    /**
     * Create a directory resource with specified workspace name and path.
     *
     * @param workspaceName
     *            workspace name.
     * @param directory
     *            path.
     * @return directory resource.
     */
    public static ScmResource createDirectoryResource(String workspaceName, String directory) {
        return new ScmDirectoryResource(workspaceName, directory);
    }

    /**
     * Create a bucket resource with specified workspace name and path.
     *
     * @param workspaceName
     *            workspace name.
     * @param bucketName
     *            bucket name. path.
     * @return bucket resource.
     */
    public static ScmResource createBucketResource(String workspaceName, String bucketName) {
        return new ScmBucketResource(bucketName, workspaceName);
    }

    /**
     * Create a all workspace resource.
     *
     * @return all workspace resource.
     */
    public static ScmResource createAllWsResource() {
        return new ScmAllWorkspaceResource();
    }

    /**
     * Create a resource with specified type and string format resource.
     *
     * @param type
     *            resource type.
     * @param resource
     *            string format resource.
     * @return resource.
     * @throws ScmException
     *             if error happens.
     */
    public static ScmResource createResource(String type, String resource) throws ScmException {
        if (type.equals(ScmWorkspaceResource.RESOURCE_TYPE)) {
            return createWorkspaceResource(resource);
        }

        if (type.equals(ScmDirectoryResource.RESOURCE_TYPE)) {
            String[] array = resource.split(":");
            if (array.length != 2) {
                throw new ScmInvalidArgumentException(
                        "directory's resource is invalid:resource=" + resource);
            }

            return createDirectoryResource(array[0], array[1]);
        }

        if (type.equals(ScmAllWorkspaceResource.RESOURCE_TYPE)) {
            return new ScmAllWorkspaceResource();
        }

        throw new ScmInvalidArgumentException("unreconigzed type:type=" + type);
    }

    /**
     * Create a resource with specified bsonObject.
     *
     * @param obj
     *            a bson containing information about resource.
     * @return resource.
     * @throws ScmException
     *             if error happens.
     */
    public static ScmResource createResource(BSONObject obj) throws ScmException {
        if (obj == null) {
            throw new ScmInvalidArgumentException("obj is null");
        }

        try {
            String type = BsonUtils.getStringChecked(obj, FieldName.Resource.FIELD_RESOURCE_TYPE);
            String resource = BsonUtils.getStringChecked(obj, FieldName.Resource.FIELD_RESOURCE);

            return createResource(type, resource);
        }
        catch (Exception e) {
            throw new ScmInvalidArgumentException("obj is invalid:obj=" + obj, e);
        }
    }
}
