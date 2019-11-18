package org.springframework.session.data.sequoiadb;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBQuery;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.session.FindByIndexNameSessionRepository;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class JacksonSequoiadbSessionConverter extends AbstractSequoiadbSessionConverter {

    private static final Logger logger = LoggerFactory.getLogger(JacksonSequoiadbSessionConverter.class);

    private static final String ATTRS_FIELD_NAME = ATTRIBUTES + ".";

    private final ObjectMapper objectMapper;

    public JacksonSequoiadbSessionConverter(Iterable<Module> modules) {
        this.objectMapper = buildObjectMapper();
        this.objectMapper.registerModules(modules);
    }

    public JacksonSequoiadbSessionConverter() {
        this(Collections.<Module>emptyList());
    }

    private ObjectMapper buildObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // serialize fields instead of properties
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        // ignore unresolved fields (mostly 'principal')
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        objectMapper.setPropertyNamingStrategy(new SequoiadbIdNamingStrategy());

        objectMapper.addMixIn(SequoiadbSession.class, SequoiadbSessionMixin.class);
        objectMapper.addMixIn(HashMap.class, HashMapMixin.class);
        objectMapper.addMixIn(HashSet.class, HashSetMixin.class);
        return objectMapper;
    }

    /**
     * Used to whitelist {@link SequoiadbSession} for {@link SecurityJackson2Modules}
     */
    private static class SequoiadbSessionMixin {
        // nothing
    }

    /**
     * Used to whitelist {@link HashMap} for {@link SecurityJackson2Modules}
     */
    private static class HashMapMixin {
        // nothing
    }

    /**
     * Used to whitelist {@link HashSet} for {@link SecurityJackson2Modules}
     */
    private static class HashSetMixin {
        // nothing
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    protected DBQuery getQueryForIndex(String indexName, Object indexValue) {
        if (FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME
                .equals(indexName)) {
            DBQuery query = new DBQuery();
            query.setMatcher(new BasicBSONObject(PRINCIPAL_FIELD_NAME, indexValue));
            return query;
        } else {
            DBQuery query = new DBQuery();
            query.setMatcher(new BasicBSONObject(
                    ATTRS_FIELD_NAME + SequoiadbSession.coverDot(indexName), indexValue));
            return query;
        }
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
    protected BSONObject convert(SequoiadbSession source) {
        try {
            BSONObject dbSession = (BSONObject) JSON.parse(this.objectMapper.writeValueAsString(source));
            dbSession.put(PRINCIPAL_FIELD_NAME, extractPrincipal(source));
            return dbSession;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot convert SequoiadbSession to BSONObject", e);
        }
    }

    @Override
    protected SequoiadbSession convert(BSONObject source) {
        String json = JSON.serialize(source);
        try {
            return this.objectMapper.readValue(json, SequoiadbSession.class);
        } catch (IOException e) {
            logger.error("Cannot convert BSONObject[{}] to SequoiadbSession",
                    JSON.serialize(source), e);
            return null;
        }
    }

    private static class SequoiadbIdNamingStrategy extends PropertyNamingStrategy.PropertyNamingStrategyBase {

        @Override
        public String translate(String propertyName) {
            if (propertyName.equals("id")) {
                return "_id";
            } else if (propertyName.equals("_id")) {
                return "id";
            }
            return propertyName;
        }
    }
}
