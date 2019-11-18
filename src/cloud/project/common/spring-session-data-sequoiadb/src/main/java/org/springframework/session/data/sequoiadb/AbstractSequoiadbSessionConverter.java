package org.springframework.session.data.sequoiadb;

import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.DBQuery;
import org.bson.BSONObject;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

import java.util.Collections;
import java.util.Set;

public abstract class AbstractSequoiadbSessionConverter implements GenericConverter {

    static final String ATTRIBUTES = "attributes";
    static final String PRINCIPAL_FIELD_NAME = "principal";
    private static final String SPRING_SECURITY_CONTEXT = "SPRING_SECURITY_CONTEXT";

    /**
     * Returns query to be executed to return sessions based on a particular index.
     *
     * @param indexName  name of the index
     * @param indexValue value to query against
     * @return built query or null if indexName is not supported
     */
    protected abstract DBQuery getQueryForIndex(String indexName, Object indexValue);

    protected abstract void ensureIndexes(DBCollection sessionCollection);

    protected boolean hasIndex(DBCollection sessionCollection, String indexName) {
        DBCursor cursor = sessionCollection.getIndex(indexName);
        if (cursor != null) {
            if (cursor.hasNext()) {
                cursor.close();
                return true;
            } else {
                cursor.close();
                return false;
            }
        } else {
            return false;
        }
    }

    static String extractPrincipal(Session expiringSession) {
        String resolvedPrincipal = AuthenticationParser
                .extractName(expiringSession.getAttribute(SPRING_SECURITY_CONTEXT));
        if (resolvedPrincipal != null) {
            return resolvedPrincipal;
        } else {
            return expiringSession.getAttribute(
                    FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME);
        }
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(
                new ConvertiblePair(BSONObject.class, SequoiadbSession.class));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) {
            return null;
        }

        if (BSONObject.class.isAssignableFrom(sourceType.getType())) {
            return convert((BSONObject) source);
        } else {
            return convert((SequoiadbSession) source);
        }
    }

    protected abstract BSONObject convert(SequoiadbSession session);

    protected abstract SequoiadbSession convert(BSONObject sessionWrapper);
}
