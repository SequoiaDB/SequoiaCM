package com.sequoiacm.cloud.authentication.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.sequoiacm.infrastructrue.security.core.ITransaction;
import com.sequoiacm.infrastructrue.security.core.ScmRole;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.core.ScmUserPasswordType;
import com.sequoiacm.infrastructrue.security.core.ScmUserRoleRepository;
import com.sequoiadb.base.SequoiadbDatasource;

public class SequoiadbScmUserRoleRepository implements ScmUserRoleRepository {
    private static final Logger logger = LoggerFactory
            .getLogger(SequoiadbScmUserRoleRepository.class);

    private static final String FIELD_USER_ID = "_id";
    private static final String FIELD_USER_NAME = "username";
    private static final String FIELD_USER_PASSWORD_TYPE = "passwordType";
    private static final String FIELD_USER_PASSWORD = "password";
    private static final String FIELD_USER_ENABLED = "enabled";
    private static final String FIELD_USER_ROLES = "roles";

    private static final String FIELD_ROLE_ID = "_id";
    private static final String FIELD_ROLE_NAME = "roleName";
    private static final String FIELD_ROLE_DESCRIPTION = "description";

    private static final String USER_NAME_INDEX = "username_index";
    private static final String ROLE_NAME_INDEX = "role_name_index";

    private final String collectionSpaceName;
    private final String userCollectionName;
    private final String roleCollectionName;
    private final SequoiadbDatasource datasource;
    private final SequoiadbTemplate template;
    private final PasswordEncoder passwordEncoder;

    public SequoiadbScmUserRoleRepository(SequoiadbDatasource datasource,
            String collectionSpaceName, String userCollectionName, String roleCollectionName,
            PasswordEncoder passwordEncoder) {
        Assert.notNull(datasource, "datasource cannot be null");
        Assert.hasText(collectionSpaceName, "collectionSpaceName cannot be null or empty");
        Assert.hasText(userCollectionName, "userCollectionName cannot be null or empty");
        Assert.hasText(roleCollectionName, "roleCollectionName cannot be null or empty");
        Assert.notNull(passwordEncoder, "passwordEncoder cannot be null");
        this.collectionSpaceName = collectionSpaceName;
        this.userCollectionName = userCollectionName;
        this.roleCollectionName = roleCollectionName;
        this.datasource = datasource;
        this.template = new SequoiadbTemplate(datasource);
        this.passwordEncoder = passwordEncoder;
        ensureIndexes();
    }



    private void ensureIndexes() {
        ensureUsernameIndex();
        ensureRoleNameIndex();
    }

    private void ensureUsernameIndex() {
        BSONObject def = new BasicBSONObject(FIELD_USER_NAME, 1);
        template.collection(collectionSpaceName, userCollectionName).ensureIndex(USER_NAME_INDEX,
                def, true);
    }

    private void ensureRoleNameIndex() {
        BSONObject def = new BasicBSONObject(FIELD_ROLE_NAME, 1);
        template.collection(collectionSpaceName, roleCollectionName).ensureIndex(ROLE_NAME_INDEX,
                def, true);
    }

    private ScmUser bsonToUser(BSONObject obj) {
        ScmUser.ScmUserBuilder builder = ScmUser.withUsername((String) obj.get(FIELD_USER_NAME));
        if (obj.containsField(FIELD_USER_ID)) {
            builder.userId(obj.get(FIELD_USER_ID).toString());
        }
        if (obj.containsField(FIELD_USER_PASSWORD_TYPE)) {
            ScmUserPasswordType userType = ScmUserPasswordType
                    .valueOf((String) obj.get(FIELD_USER_PASSWORD_TYPE));
            builder.passwordType(userType);
        }
        if (obj.containsField(FIELD_USER_PASSWORD)) {
            builder.password((String) obj.get(FIELD_USER_PASSWORD));
        }
        if (obj.containsField(FIELD_USER_ENABLED)) {
            builder.disabled(!(Boolean) obj.get(FIELD_USER_ENABLED));
        }
        if (obj.containsField(FIELD_USER_ROLES)) {
            BasicBSONList list = (BasicBSONList) obj.get(FIELD_USER_ROLES);
            List<ScmRole> roles = new ArrayList<>();
            for (Object o : list) {
                String roleId = (String) o;
                ScmRole role = findRoleById(roleId);
                if (role != null) {
                    roles.add(role);
                }
                else {
                    logger.warn("Cannot find role by id: {}", roleId);
                }
            }
            builder.roles(roles);
        }
        return builder.build();
    }

    private BSONObject userToBSONObj(ScmUser user) {
        BSONObject obj = new BasicBSONObject();
        obj.put(FIELD_USER_ID, new ObjectId(user.getUserId()));
        obj.put(FIELD_USER_NAME, user.getUsername());
        obj.put(FIELD_USER_PASSWORD_TYPE, user.getPasswordType().name());
        obj.put(FIELD_USER_PASSWORD, user.getPassword());
        obj.put(FIELD_USER_ENABLED, user.isEnabled());
        BasicBSONList roles = new BasicBSONList();
        for (ScmRole role : user.getAuthorities()) {
            roles.add(role.getRoleId());
        }
        obj.put(FIELD_USER_ROLES, roles);
        return obj;
    }

    private ScmRole bsonToRole(BSONObject obj) {
        return ScmRole.withRoleName((String) obj.get(FIELD_ROLE_NAME))
                .roleId(obj.get(FIELD_ROLE_ID).toString())
                .description((String) obj.get(FIELD_ROLE_DESCRIPTION)).build();
    }

    private BSONObject roleToBSONObj(ScmRole role) {
        BSONObject obj = new BasicBSONObject();
        obj.put(FIELD_ROLE_ID, new ObjectId(role.getRoleId()));
        obj.put(FIELD_ROLE_NAME, role.getRoleName());
        if (StringUtils.hasText(role.getDescription())) {
            obj.put(FIELD_ROLE_DESCRIPTION, role.getDescription());
        }
        return obj;
    }

    @Override
    public String generateUserId() {
        return ObjectId.get().toString();
    }

    @Override
    public void insertUser(ScmUser user) {
        BSONObject obj = userToBSONObj(user);
        template.collection(collectionSpaceName, userCollectionName).insert(obj);
    }

    @Override
    public void updateUser(ScmUser user, ITransaction t) {
        BSONObject obj = userToBSONObj(user);
        obj.removeField(FIELD_USER_ID);
        BSONObject matcher = new BasicBSONObject(FIELD_USER_ID, new ObjectId(user.getUserId()));
        BSONObject modifier = new BasicBSONObject("$set", obj);

        template.collection(collectionSpaceName, userCollectionName).update(matcher, modifier,
                (SequoiadbTransaction) t);
    }

    @Override
    public void deleteRoleFromUser(ScmUser user, String roleId, ITransaction t) {
        BSONObject matcher = new BasicBSONObject(FIELD_USER_ID, new ObjectId(user.getUserId()));

        BSONObject removedValue = new BasicBSONObject();
        removedValue.put(FIELD_USER_ROLES, roleId);
        BSONObject modifier = new BasicBSONObject("$pull", removedValue);

        template.collection(collectionSpaceName, userCollectionName).update(matcher, modifier,
                (SequoiadbTransaction) t);
    }

    @Override
    public void deleteUser(ScmUser user) {
        deleteUser(user, null);
    }

    @Override
    public void deleteUser(ScmUser user, ITransaction t) {
        BSONObject matcher = new BasicBSONObject(FIELD_USER_ID, new ObjectId(user.getUserId()));

        template.collection(collectionSpaceName, userCollectionName).delete(matcher,
                (SequoiadbTransaction) t);
    }

    @Override
    public ScmUser findUserByName(String userName) {
        BSONObject matcher = new BasicBSONObject(FIELD_USER_NAME, userName);
        BSONObject obj = template.collection(collectionSpaceName, userCollectionName)
                .findOne(matcher);
        if (obj == null) {
            return null;
        }
        return bsonToUser(obj);
    }

    @Override
    public ScmUser findUserById(String userId) {
        BSONObject matcher = new BasicBSONObject(FIELD_USER_ID, new ObjectId(userId));
        BSONObject obj = template.collection(collectionSpaceName, userCollectionName)
                .findOne(matcher);
        if (obj == null) {
            return null;
        }
        return bsonToUser(obj);
    }

    @Override
    public List<ScmUser> findUsersByRoleName(String roleName) {
        List<ScmUser> users = new ArrayList<>();

        ScmRole role = findRoleByName(roleName);
        if (role == null) {
            return users;
        }

        BSONObject matcher = new BasicBSONObject(FIELD_USER_ROLES, role.getRoleId());
        List<BSONObject> objs = template.collection(collectionSpaceName, userCollectionName)
                .find(matcher);
        for (BSONObject obj : objs) {
            ScmUser user = bsonToUser(obj);
            users.add(user);
        }
        return users;
    }

    @Override
    public String generateRoleId() {
        return ObjectId.get().toString();
    }

    @Override
    public void insertRole(ScmRole role) {
        BSONObject obj = roleToBSONObj(role);
        template.collection(collectionSpaceName, roleCollectionName).insert(obj);
    }

    @Override
    public void deleteRoleByName(String roleName) {
        BSONObject matcher = new BasicBSONObject(FIELD_ROLE_NAME, roleName);

        template.collection(collectionSpaceName, roleCollectionName).delete(matcher);
    }

    @Override
    public void deleteRoleById(String roleId) {
        deleteRoleById(roleId, null);
    }

    @Override
    public void deleteRoleById(String roleId, ITransaction t) {
        BSONObject matcher = new BasicBSONObject(FIELD_ROLE_ID, new ObjectId(roleId));
        template.collection(collectionSpaceName, roleCollectionName).delete(matcher,
                (SequoiadbTransaction) t);
    }

    @Override
    public void deleteRole(ScmRole role) {
        deleteRole(role, null);
    }

    @Override
    public void deleteRole(ScmRole role, ITransaction t) {
        deleteRoleById(role.getRoleId(), t);
    }

    @Override
    public ScmRole findRoleByName(String roleName) {
        BSONObject matcher = new BasicBSONObject(FIELD_ROLE_NAME, roleName);
        BSONObject obj = template.collection(collectionSpaceName, roleCollectionName)
                .findOne(matcher);
        if (obj == null) {
            return null;
        }
        return bsonToRole(obj);
    }

    @Override
    public ScmRole findRoleById(String roleId) {
        BSONObject matcher = new BasicBSONObject(FIELD_ROLE_ID, new ObjectId(roleId));
        BSONObject obj = template.collection(collectionSpaceName, roleCollectionName)
                .findOne(matcher);
        if (obj == null) {
            return null;
        }
        return bsonToRole(obj);
    }

    @Override
    public List<ScmRole> findAllRoles(BSONObject orderBy, long skip, long limit) {
        List<ScmRole> roles = new ArrayList<>();
        List<BSONObject> objs = template.collection(collectionSpaceName, roleCollectionName)
                .find(null, orderBy, skip, limit);
        for (BSONObject obj : objs) {
            ScmRole role = bsonToRole(obj);
            roles.add(role);
        }
        return roles;
    }

    @Override
    public List<ScmUser> findAllUsers(ScmUserPasswordType type, Boolean enabled, String roleName,
            BSONObject orderBy, long skip, long limit) {
        BSONObject matcher = new BasicBSONObject();
        if (type != null) {
            matcher.put(FIELD_USER_PASSWORD_TYPE, type.toString());
        }
        if (enabled != null) {
            matcher.put(FIELD_USER_ENABLED, enabled);
        }
        if (roleName != null) {
            ScmRole role = findRoleByName(roleName);
            if (role == null) {
                return Collections.emptyList();
            }
            matcher.put(FIELD_USER_ROLES, role.getRoleId());
        }
        return findAllUsers(matcher, orderBy, skip, limit);
    }

    private List<ScmUser> findAllUsers(BSONObject matcher, BSONObject orderBy, long skip,
            long limit) {
        List<ScmUser> users = new ArrayList<>();
        List<BSONObject> objs = template.collection(collectionSpaceName, userCollectionName)
                .find(matcher, orderBy, skip, limit);
        for (BSONObject obj : objs) {
            ScmUser user = bsonToUser(obj);
            users.add(user);
        }
        return users;
    }

}
