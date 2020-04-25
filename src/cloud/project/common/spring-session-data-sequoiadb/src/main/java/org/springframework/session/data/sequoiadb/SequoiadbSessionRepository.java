package org.springframework.session.data.sequoiadb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.util.StringUtils;

import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.DBQuery;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.base.SequoiadbDatasource;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;

public class SequoiadbSessionRepository
        implements FindByIndexNameSessionRepository<SequoiadbSession> {

    public static final String DEFAULT_COLLECTION_SPACE_NAME = "spring";
    public static final String DEFAULT_COLLECTION_NAME = "sessions";

    private final SequoiadbDatasource sequoiadbDatasource;

    private AbstractSequoiadbSessionConverter sequoiadbSessionConverter = SessionConverterProvider
            .getDefaultSequoiadbConverter();

    private Integer maxInactiveInterval = SequoiadbSession.DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS;
    private String collectionSpaceName = DEFAULT_COLLECTION_SPACE_NAME;
    private String collectionName = DEFAULT_COLLECTION_NAME;

    public SequoiadbSessionRepository(SequoiadbDatasource sequoiadbDatasource) {
        this.sequoiadbDatasource = sequoiadbDatasource;
    }

    @Override
    public Map<String, SequoiadbSession> findByIndexNameAndIndexValue(String indexName,
            String indexValue) {
        DBQuery query = this.sequoiadbSessionConverter.getQueryForIndex(indexName, indexValue);
        if (query == null) {
            return Collections.emptyMap();
        }

        HashMap<String, SequoiadbSession> result = new HashMap<String, SequoiadbSession>();

        Sequoiadb sdb;
        try {
            sdb = sequoiadbDatasource.getConnection();
        }
        catch (InterruptedException e) {
            throw new BaseException(SDBError.SDB_INTERRUPT, e);
        }
        try {
            DBCursor cursor = sdb.getCollectionSpace(collectionSpaceName)
                    .getCollection(collectionName).query(query);
            if (cursor != null) {
                while (cursor.hasNext()) {
                    BSONObject obj = cursor.getNext();
                    SequoiadbSession session = convert(obj);
                    if (session != null) {
                        result.put(session.getId(), session);
                    }
                }
                cursor.close();
            }
        }
        finally {
            sequoiadbDatasource.releaseConnection(sdb);
        }
        return result;
    }

    @Override
    public SequoiadbSession createSession() {
        SequoiadbSession session = new SequoiadbSession();
        if (this.maxInactiveInterval != null) {
            session.setMaxInactiveIntervalInSeconds(this.maxInactiveInterval);
        }
        session.setNew(true);
        return session;
    }

    @Override
    public void save(SequoiadbSession session) {
        BSONObject sessionObj = convert(session);
        BSONObject matcher = new BasicBSONObject(JdkSequoiadbSessionConverter.ID, session.getId());
        sessionObj.removeField(JdkSequoiadbSessionConverter.ID);
        BSONObject modifier = new BasicBSONObject("$set", sessionObj);

        Sequoiadb sdb;
        try {
            sdb = sequoiadbDatasource.getConnection();
        }
        catch (InterruptedException e) {
            throw new BaseException(SDBError.SDB_INTERRUPT, e);
        }
        try {
            if (session.isNew()) {
                sdb.getCollectionSpace(collectionSpaceName).getCollection(collectionName)
                        .upsert(matcher, modifier, null);
                session.setNew(false);
            }
            else {
                sdb.getCollectionSpace(collectionSpaceName).getCollection(collectionName)
                        .update(matcher, modifier, null);
            }
        }
        finally {
            sequoiadbDatasource.releaseConnection(sdb);
        }
    }

    @Override
    public SequoiadbSession getSession(String sessionId) {
        return getSession(sessionId, false);
    }

    public SequoiadbSession getSession(String sessionId, boolean attributesIgnorable) {
        BSONObject sessionWrapper = findSession(sessionId);
        if (sessionWrapper == null) {
            return null;
        }
        if (attributesIgnorable) {
            sessionWrapper.removeField(AbstractSequoiadbSessionConverter.ATTRIBUTES);
        }
        SequoiadbSession session = convert(sessionWrapper);
        if (session == null) {
            return null;
        }
        if (session.isExpired()) {
            delete(sessionId);
            return null;
        }
        session.setNew(false);
        return session;
    }

    @Override
    public void delete(String sessionId) {
        BSONObject matcher = new BasicBSONObject(JdkSequoiadbSessionConverter.ID, sessionId);
        Sequoiadb sdb;
        try {
            sdb = sequoiadbDatasource.getConnection();
        }
        catch (InterruptedException e) {
            throw new BaseException(SDBError.SDB_INTERRUPT, e);
        }
        try {
            sdb.getCollectionSpace(collectionSpaceName).getCollection(collectionName)
                    .delete(matcher);
        }
        finally {
            sequoiadbDatasource.releaseConnection(sdb);
        }
    }

    @PostConstruct
    public void ensureIndexesAreCreated() {
        Sequoiadb sdb;
        try {
            sdb = sequoiadbDatasource.getConnection();
        }
        catch (InterruptedException e) {
            throw new BaseException(SDBError.SDB_INTERRUPT, e);
        }
        try {
            DBCollection collection = sdb.getCollectionSpace(collectionSpaceName)
                    .getCollection(collectionName);
            sequoiadbSessionConverter.ensureIndexes(collection);
        }
        finally {
            sequoiadbDatasource.releaseConnection(sdb);
        }
    }

    public List<SequoiadbSession> getAllSessions(boolean attributesIgnorable) {
        return getAllSessions(null, attributesIgnorable);
    }

    public List<SequoiadbSession> getAllSessions(String principal, boolean attributesIgnorable) {
        List<SequoiadbSession> sessions = new ArrayList<>();

        List<BSONObject> objects = findSessions(principal);
        if (objects != null && objects.size() > 0) {
            for (BSONObject obj : objects) {
                if (attributesIgnorable) {
                    obj.removeField(AbstractSequoiadbSessionConverter.ATTRIBUTES);
                }
                SequoiadbSession session = convert(obj);
                if (session != null) {
                    if (session.isExpired()) {
                        delete(session.getId());
                    }
                    else {
                        sessions.add(session);
                    }
                }
            }
        }

        return sessions;
    }

    private BSONObject findSession(String id) {
        BSONObject matcher = new BasicBSONObject(JdkSequoiadbSessionConverter.ID, id);
        Sequoiadb sdb;
        try {
            sdb = sequoiadbDatasource.getConnection();
        }
        catch (InterruptedException e) {
            throw new BaseException(SDBError.SDB_INTERRUPT, e);
        }
        try {
            return sdb.getCollectionSpace(collectionSpaceName).getCollection(collectionName)
                    .queryOne(matcher, null, null, null, 0);
        }
        finally {
            sequoiadbDatasource.releaseConnection(sdb);
        }
    }

    private List<BSONObject> findSessions(String principal) {
        List<BSONObject> objects = new ArrayList<>();
        BSONObject matcher = new BasicBSONObject();
        if (StringUtils.hasText(principal)) {
            matcher.put(AbstractSequoiadbSessionConverter.PRINCIPAL_FIELD_NAME, principal);
        }

        Sequoiadb sdb;
        try {
            sdb = sequoiadbDatasource.getConnection();
        }
        catch (InterruptedException e) {
            throw new BaseException(SDBError.SDB_INTERRUPT, e);
        }
        try {
            DBCursor cursor = sdb.getCollectionSpace(collectionSpaceName)
                    .getCollection(collectionName).query(matcher, null, null, null, 0);
            if (cursor != null) {
                while (cursor.hasNext()) {
                    objects.add(cursor.getNext());
                }
                cursor.close();
            }
        }
        finally {
            sequoiadbDatasource.releaseConnection(sdb);
        }

        return objects;
    }

    public void deleteSessions(String principal) {
        if (!StringUtils.hasText(principal)) {
            return;
        }

        BSONObject matcher = new BasicBSONObject(
                AbstractSequoiadbSessionConverter.PRINCIPAL_FIELD_NAME, principal);

        Sequoiadb sdb;
        try {
            sdb = sequoiadbDatasource.getConnection();
        }
        catch (InterruptedException e) {
            throw new BaseException(SDBError.SDB_INTERRUPT, e);
        }
        try {
            sdb.getCollectionSpace(collectionSpaceName).getCollection(collectionName)
                    .delete(matcher);
        }
        finally {
            sequoiadbDatasource.releaseConnection(sdb);
        }
    }

    /**
     * Clean specified amount of expired sessions.
     * @param num Max sessions to delete.
     * @return actually deleted expired session num
     */
    public int cleanExpiredSessions(int num) {
        if (num <= 0) {
            return 0;
        }

        int count = 0;

        Sequoiadb sdb;
        try {
            sdb = sequoiadbDatasource.getConnection();
        }
        catch (InterruptedException e) {
            throw new BaseException(SDBError.SDB_INTERRUPT, e);
        }
        try {
            DBCursor cursor = sdb.getCollectionSpace(collectionSpaceName)
                    .getCollection(collectionName).query();
            if (cursor != null) {
                while (cursor.hasNext()) {
                    BSONObject obj = cursor.getNext();
                    SequoiadbSession session = convert(obj);
                    if (session != null && session.isExpired()) {
                        delete(session.getId());
                        count++;
                        if (count > num) {
                            break;
                        }
                    }
                }
                cursor.close();
            }
        }
        finally {
            sequoiadbDatasource.releaseConnection(sdb);
        }

        return count;
    }

    public long countSessions() {
        Sequoiadb sdb;
        long count = 0;
        try {
            sdb = sequoiadbDatasource.getConnection();
        }
        catch (InterruptedException e) {
            throw new BaseException(SDBError.SDB_INTERRUPT, e);
        }
        try {
            count = sdb.getCollectionSpace(collectionSpaceName).getCollection(collectionName)
                    .getCount();
        }
        finally {
            sequoiadbDatasource.releaseConnection(sdb);
        }
        return count;
    }

    public SequoiadbSession convert(BSONObject session) {
        return (SequoiadbSession) this.sequoiadbSessionConverter.convert(session,
                TypeDescriptor.valueOf(BSONObject.class),
                TypeDescriptor.valueOf(SequoiadbSession.class));
    }

    public BSONObject convert(SequoiadbSession session) {
        return (BSONObject) this.sequoiadbSessionConverter.convert(session,
                TypeDescriptor.valueOf(SequoiadbSession.class),
                TypeDescriptor.valueOf(BSONObject.class));
    }

    public void setSequoiadbSessionConverter(
            AbstractSequoiadbSessionConverter mongoSessionConverter) {
        this.sequoiadbSessionConverter = mongoSessionConverter;
    }

    public void setMaxInactiveIntervalInSeconds(Integer maxInactiveIntervalInSeconds) {
        this.maxInactiveInterval = maxInactiveIntervalInSeconds;
    }

    public void setCollectionSpaceName(String collectionSpaceName) {
        this.collectionSpaceName = collectionSpaceName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }
}
