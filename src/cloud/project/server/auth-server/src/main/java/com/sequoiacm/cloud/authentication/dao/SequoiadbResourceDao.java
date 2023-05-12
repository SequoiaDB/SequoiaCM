package com.sequoiacm.cloud.authentication.dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.infrastructrue.security.core.ITransaction;
import com.sequoiacm.infrastructrue.security.core.ScmResource;
import com.sequoiadb.datasource.SequoiadbDatasource;

@Repository("IResourceDao")
public class SequoiadbResourceDao implements IResourceDao {
    private static final String CS_SCMSYSTEM = "SCMSYSTEM";
    private static final String CL_PRIV_RESOURCE = "PRIV_RESOURCE";

    private static final String FIELD_ID = "id";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_RESOURCE = "resource";

    private static final String RESOURCE_ID_INDEX = "id_index";

    private final SequoiadbDatasource datasource;
    private final SequoiadbTemplate template;

    @Autowired
    public SequoiadbResourceDao(SequoiadbDatasource datasource) {
        this.datasource = datasource;
        this.template = new SequoiadbTemplate(datasource);
        ensureIndexes();
    }

    private void ensureIndexes() {
        ensureIdIndex();
    }

    private void ensureIdIndex() {
        BSONObject def = new BasicBSONObject(FIELD_ID, 1);
        template.collection(CS_SCMSYSTEM, CL_PRIV_RESOURCE).ensureIndex(RESOURCE_ID_INDEX, def,
                true);
    }

    @Override
    public void insertResource(ScmResource resource) {
        insertResource(resource, null);
    }

    @Override
    public void insertResource(ScmResource r, ITransaction t) {
        BSONObject obj = resourceToBSONObj(r);
        template.collection(CS_SCMSYSTEM, CL_PRIV_RESOURCE).insert(obj, (SequoiadbTransaction) t);
    }

    private BSONObject resourceToBSONObj(ScmResource resource) {
        BSONObject obj = new BasicBSONObject();
        obj.put(FIELD_ID, resource.getId());
        obj.put(FIELD_TYPE, resource.getType());
        obj.put(FIELD_RESOURCE, resource.getResource());
        return obj;
    }

    @Override
    public void deleteResource(ScmResource resource) {
        deleteResource(resource);
    }

    @Override
    public void deleteResource(ScmResource resource, ITransaction t) {
        BSONObject matcher = new BasicBSONObject(FIELD_ID, resource.getId());
        template.collection(CS_SCMSYSTEM, CL_PRIV_RESOURCE).delete(matcher,
                (SequoiadbTransaction) t);
    }

    @Override
    public List<ScmResource> listResources() {
        List<ScmResource> resources = new ArrayList<>();

        BSONObject matcher = new BasicBSONObject();
        List<BSONObject> objs = template.collection(CS_SCMSYSTEM, CL_PRIV_RESOURCE).find(matcher);
        for (BSONObject obj : objs) {
            ScmResource resource = bsonToResource(obj);
            resources.add(resource);
        }

        return resources;
    }

    @Override
    public List<ScmResource> listResourcesByWorkspace(String workspaceName) {
        List<ScmResource> resources = new ArrayList<>();

        BSONObject matcher = new BasicBSONObject();
        BSONObject regex = new BasicBSONObject();
        regex.put("$regex", "^" + workspaceName + "($|:.*)");
        matcher.put(ScmResource.JSON_FIELD_RESOURCE, regex);
        List<BSONObject> objs = template.collection(CS_SCMSYSTEM, CL_PRIV_RESOURCE).find(matcher);
        for (BSONObject obj : objs) {
            ScmResource resource = bsonToResource(obj);
            resources.add(resource);
        }

        return resources;
    }

    @Override
    public String generateResourceId() {
        return ObjectId.get().toString();
    }

    private ScmResource bsonToResource(BSONObject obj) {
        String id = "";
        Object tmp = obj.get(FIELD_ID);
        if (null != tmp) {
            id = (String) tmp;
        }

        String type = "";
        tmp = obj.get(FIELD_TYPE);
        if (null != tmp) {
            type = (String) tmp;
        }

        String resource = "";
        tmp = obj.get(FIELD_RESOURCE);
        if (null != tmp) {
            resource = (String) tmp;
        }

        return new ScmResource(id, type, resource);
    }

    @Override
    public ScmResource getResource(String resourceType, String resource) {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(ScmResource.JSON_FIELD_TYPE, resourceType);
        matcher.put(ScmResource.JSON_FIELD_RESOURCE, resource);

        BSONObject result = template.collection(CS_SCMSYSTEM, CL_PRIV_RESOURCE).findOne(matcher);
        if (null != result) {
            return bsonToResource(result);
        }

        return null;
    }

    @Override
    public ScmResource getResourceById(String resourceId) {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(ScmResource.JSON_FIELD_ID, resourceId);

        BSONObject result = template.collection(CS_SCMSYSTEM, CL_PRIV_RESOURCE).findOne(matcher);
        if (null != result) {
            return bsonToResource(result);
        }

        return null;
    }
}
