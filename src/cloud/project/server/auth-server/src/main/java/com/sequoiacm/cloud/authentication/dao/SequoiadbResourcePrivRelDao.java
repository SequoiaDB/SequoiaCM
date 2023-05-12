package com.sequoiacm.cloud.authentication.dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.infrastructrue.security.core.ITransaction;
import com.sequoiacm.infrastructrue.security.core.ScmPrivilege;
import com.sequoiadb.datasource.SequoiadbDatasource;

@Repository("IResourcePrivRelDao")
public class SequoiadbResourcePrivRelDao implements IResourcePrivRelDao {

    private static final String CS_SCMSYSTEM = "SCMSYSTEM";
    private static final String CL_PRIV_ROLE_RESOURCE_REL = "PRIV_ROLE_RESOURCE_REL";

    private static final String FIELD_ID = "id";
    private static final String FIELD_ROLE_TYPE = "role_type";
    private static final String FIELD_ROLE_ID = "role_id";
    private static final String FIELD_RESOURCE_ID = "resource_id";
    private static final String FIELD_PRIVILEGE = "privilege";

    private static final String PRIV_RESOURCE_ID_INDEX = "id_index";

    private final SequoiadbDatasource datasource;
    private final SequoiadbTemplate template;

    @Autowired
    public SequoiadbResourcePrivRelDao(SequoiadbDatasource datasource) {
        this.datasource = datasource;
        this.template = new SequoiadbTemplate(datasource);
        ensureIndexes();
    }

    private void ensureIndexes() {
        ensureIdIndex();
    }

    private void ensureIdIndex() {
        BSONObject def = new BasicBSONObject(FIELD_ID, 1);
        template.collection(CS_SCMSYSTEM, CL_PRIV_ROLE_RESOURCE_REL)
                .ensureIndex(PRIV_RESOURCE_ID_INDEX, def, true);
    }

    @Override
    public void insertPrivilege(ScmPrivilege privilege) {
        insertPrivilege(privilege, null);
    }

    @Override
    public void insertPrivilege(ScmPrivilege privilege, ITransaction t) {
        BSONObject obj = privilgeToBSONObj(privilege);
        template.collection(CS_SCMSYSTEM, CL_PRIV_ROLE_RESOURCE_REL).insert(obj,
                (SequoiadbTransaction) t);
    }

    private BSONObject privilgeToBSONObj(ScmPrivilege privilege) {
        BSONObject obj = new BasicBSONObject();
        obj.put(FIELD_ID, privilege.getId());
        obj.put(FIELD_ROLE_TYPE, privilege.getRoleType());
        obj.put(FIELD_ROLE_ID, privilege.getRoleId());
        obj.put(FIELD_RESOURCE_ID, privilege.getResourceId());
        obj.put(FIELD_PRIVILEGE, privilege.getPrivilege());
        return obj;
    }

    @Override
    public void deletePrivilege(ScmPrivilege privilege) {
        deletePrivilege(privilege, null);
    }

    @Override
    public void deletePrivilege(ScmPrivilege privilge, ITransaction t) {
        BSONObject matcher = new BasicBSONObject(FIELD_ID, privilge.getId());
        template.collection(CS_SCMSYSTEM, CL_PRIV_ROLE_RESOURCE_REL).delete(matcher,
                (SequoiadbTransaction) t);
    }

    @Override
    public String generatePrivilegeId() {
        return ObjectId.get().toString();
    }

    @Override
    public List<ScmPrivilege> listPrivilegesByRoleId(String roleId) {
        List<ScmPrivilege> privileges = new ArrayList<>();
        BSONObject matcher = new BasicBSONObject();
        matcher.put(ScmPrivilege.JSON_FIELD_ROLE_ID, roleId);

        List<BSONObject> objs = template.collection(CS_SCMSYSTEM, CL_PRIV_ROLE_RESOURCE_REL)
                .find(matcher);
        for (BSONObject obj : objs) {
            ScmPrivilege privilege = bsonToResource(obj);
            privileges.add(privilege);
        }

        return privileges;
    }

    @Override
    public List<ScmPrivilege> listPrivilegesByResourceId(String resourceId) {
        List<ScmPrivilege> privileges = new ArrayList<>();
        BSONObject matcher = new BasicBSONObject();
        matcher.put(ScmPrivilege.JSON_FIELD_RESOURCE_ID, resourceId);

        List<BSONObject> objs = template.collection(CS_SCMSYSTEM, CL_PRIV_ROLE_RESOURCE_REL)
                .find(matcher);
        for (BSONObject obj : objs) {
            ScmPrivilege privilege = bsonToResource(obj);
            privileges.add(privilege);
        }

        return privileges;
    }

    @Override
    public List<ScmPrivilege> listPrivileges() {
        List<ScmPrivilege> privileges = new ArrayList<>();
        BSONObject matcher = new BasicBSONObject();

        List<BSONObject> objs = template.collection(CS_SCMSYSTEM, CL_PRIV_ROLE_RESOURCE_REL)
                .find(matcher);
        for (BSONObject obj : objs) {
            ScmPrivilege privilege = bsonToResource(obj);
            privileges.add(privilege);
        }

        return privileges;
    }

    private ScmPrivilege bsonToResource(BSONObject obj) {
        String id = "";
        Object tmp = obj.get(FIELD_ID);
        if (null != tmp) {
            id = (String) tmp;
        }

        String roleType = "";
        tmp = obj.get(FIELD_ROLE_TYPE);
        if (null != tmp) {
            roleType = (String) tmp;
        }

        String roleId = "";
        tmp = obj.get(FIELD_ROLE_ID);
        if (null != tmp) {
            roleId = (String) tmp;
        }

        String resourceId = "";
        tmp = obj.get(FIELD_RESOURCE_ID);
        if (null != tmp) {
            resourceId = (String) tmp;
        }

        String privilege = "";
        tmp = obj.get(FIELD_PRIVILEGE);
        if (null != tmp) {
            privilege = (String) tmp;
        }

        return new ScmPrivilege(id, roleType, roleId, resourceId, privilege);
    }

    @Override
    public ScmPrivilege getPrivilege(String roleType, String roleId, String resourceId) {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(ScmPrivilege.JSON_FIELD_ROLE_TYPE, roleType);
        matcher.put(ScmPrivilege.JSON_FIELD_ROLE_ID, roleId);
        matcher.put(ScmPrivilege.JSON_FIELD_RESOURCE_ID, resourceId);

        BSONObject obj = template.collection(CS_SCMSYSTEM, CL_PRIV_ROLE_RESOURCE_REL)
                .findOne(matcher);
        if (null != obj) {
            return bsonToResource(obj);
        }

        return null;
    }

    @Override
    public ScmPrivilege getPrivilegeById(String privilegeId) {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(ScmPrivilege.JSON_FIELD_ID, privilegeId);
        BSONObject obj = template.collection(CS_SCMSYSTEM, CL_PRIV_ROLE_RESOURCE_REL)
                .findOne(matcher);
        if (null != obj) {
            return bsonToResource(obj);
        }

        return null;
    }

    @Override
    public void updatePrivilegeValue(String id, String privilege) {
        updatePrivilegeValue(id, privilege, null);
    }

    @Override
    public void updatePrivilegeValue(String id, String privilege, ITransaction t) {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(ScmPrivilege.JSON_FIELD_ID, id);

        BSONObject newValue = new BasicBSONObject(ScmPrivilege.JSON_FIELD_PRIVILEGE, privilege);
        BSONObject modifier = new BasicBSONObject("$set", newValue);

        template.collection(CS_SCMSYSTEM, CL_PRIV_ROLE_RESOURCE_REL).update(matcher, modifier,
                (SequoiadbTransaction) t);
    }
}
