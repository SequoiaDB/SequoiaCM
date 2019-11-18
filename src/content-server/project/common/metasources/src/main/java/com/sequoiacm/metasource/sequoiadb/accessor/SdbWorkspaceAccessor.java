package com.sequoiacm.metasource.sequoiadb.accessor;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.metasource.MetaWorkspaceAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;
import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import com.sequoiadb.exception.SDBError;

public class SdbWorkspaceAccessor extends SdbMetaAccessor implements MetaWorkspaceAccessor {
    private static final Logger logger = LoggerFactory.getLogger(SdbWorkspaceAccessor.class);

    public SdbWorkspaceAccessor(SdbMetaSource metasource, String csName, String clName,
            TransactionContext transaction) {
        super(metasource, csName, clName, transaction);
    }

    @Override
    public void delete(String wsName) throws ScmMetasourceException {
        try {
            BSONObject deletor = new BasicBSONObject();
            deletor.put(FieldName.FIELD_CLWORKSPACE_NAME, wsName);
            delete(deletor);
        }
        catch (SdbMetasourceException e) {
            logger.error("delete workspace record failed:table={}.{},workspaceName={}", getCsName(),
                    getClName(), wsName);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "delete workspace record failed:table=" + getCsName() + "." + getClName()
                    + ",workspaceName=" + wsName,
                    e);
        }
    }
}