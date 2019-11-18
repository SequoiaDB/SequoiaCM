package org.springframework.session.data.sequoiadb;

import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBQuery;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

public class JdkSequoiadbSessionConverter extends AbstractSequoiadbSessionConverter {

    static final String ID = "_id";
    private static final String CREATION_TIME = "creationTime";
    private static final String LAST_ACCESSED_TIME = "lastAccessedTime";
    private static final String MAX_INTERVAL = "maxInterval";

    private final Converter<Object, byte[]> serializer;
    private final Converter<byte[], Object> deserializer;

    public JdkSequoiadbSessionConverter(Converter<Object, byte[]> serializer,
                                        Converter<byte[], Object> deserializer) {
        Assert.notNull(serializer, "serializer cannot be null");
        Assert.notNull(deserializer, "deserializer cannot be null");
        this.serializer = serializer;
        this.deserializer = deserializer;
    }

    public JdkSequoiadbSessionConverter() {
        this(new SerializingConverter(), new DeserializingConverter());
    }

    @Override
    protected DBQuery getQueryForIndex(String indexName, Object indexValue) {
        if (FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME
                .equals(indexName)) {
            DBQuery query = new DBQuery();
            query.setMatcher(new BasicBSONObject(PRINCIPAL_FIELD_NAME, indexValue));
            return query;
        }
        return null;
    }

    @Override
    protected void ensureIndexes(DBCollection sessionCollection) {
        if (hasIndex(sessionCollection, PRINCIPAL_FIELD_NAME)) {
            return;
        }

        try {
            sessionCollection.createIndex(PRINCIPAL_FIELD_NAME, new BasicBSONObject(PRINCIPAL_FIELD_NAME, 1), false, false);
        } catch (BaseException e) {
            if (e.getErrorCode() != SDBError.SDB_IXM_EXIST.getErrorCode() &&
                    e.getErrorCode() != SDBError.SDB_IXM_REDEF.getErrorCode() &&
                    e.getErrorCode() != SDBError.SDB_IXM_EXIST_COVERD_ONE.getErrorCode()) {
                throw e;
            }
        }
    }

    @Override
    protected BSONObject convert(SequoiadbSession session) {
        BasicBSONObject basicDBObject = new BasicBSONObject();
        basicDBObject.put(ID, session.getId());
        basicDBObject.put(CREATION_TIME, session.getCreationTime());
        basicDBObject.put(LAST_ACCESSED_TIME, session.getLastAccessedTime());
        basicDBObject.put(MAX_INTERVAL, session.getMaxInactiveIntervalInSeconds());
        basicDBObject.put(PRINCIPAL_FIELD_NAME, extractPrincipal(session));
        basicDBObject.put(ATTRIBUTES, serializeAttributes(session));
        return basicDBObject;
    }

    @Override
    protected SequoiadbSession convert(BSONObject sessionWrapper) {
        SequoiadbSession session = new SequoiadbSession(
                (String) sessionWrapper.get(ID),
                (Integer) sessionWrapper.get(MAX_INTERVAL));
        session.setCreationTime((Long) sessionWrapper.get(CREATION_TIME));
        session.setLastAccessedTime((Long) sessionWrapper.get(LAST_ACCESSED_TIME));
        deserializeAttributes(sessionWrapper, session);
        return session;
    }

    private byte[] serializeAttributes(Session session) {
        Map<String, Object> attributes = new HashMap<String, Object>();
        for (String attrName : session.getAttributeNames()) {
            attributes.put(attrName, session.getAttribute(attrName));
        }
        return this.serializer.convert(attributes);
    }

    @SuppressWarnings("unchecked")
    private void deserializeAttributes(BSONObject sessionWrapper, Session session) {
        byte[] attributesBytes = (byte[]) sessionWrapper.get(ATTRIBUTES);
        Map<String, Object> attributes = (Map<String, Object>) this.deserializer
                .convert(attributesBytes);
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            session.setAttribute(entry.getKey(), entry.getValue());
        }
    }
}
