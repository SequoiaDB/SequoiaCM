package com.sequoiacm.metasource.sequoiadb.accessor;

import java.util.Date;

import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import com.sequoiadb.exception.SDBError;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.metasource.MetaBatchAccessor;
import com.sequoiacm.metasource.TransactionContext;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;

public class SdbBatchAccessor extends SdbMetaAccessor implements MetaBatchAccessor {
    private static final Logger logger = LoggerFactory.getLogger(SdbBatchAccessor.class);
    
    public SdbBatchAccessor(SdbMetaSource metasource, String csName, String clName, 
            TransactionContext context) {
        super(metasource, csName, clName, context);
    }
    
    @Override
    public void delete(String batchId) throws SdbMetasourceException {
        try {
            BSONObject deletor = new BasicBSONObject();
            addBatchIdAndCreateMonth(deletor, batchId);

            delete(deletor);
        }
        catch (SdbMetasourceException e) {
            logger.error("delete failed:table=" + getCsName() + "." + getClName()
                    + ",batchId=" + batchId);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "delete failed:table=" + getCsName() + "." + getClName()
                    + ",batchId=" + batchId, e);
        }
    }
    
    @Override
    public boolean update(String batchId, BSONObject newBatchInfo) throws SdbMetasourceException {
        try {
            // TODO 校验 batchInfo
            BSONObject updator = 
                    new BasicBSONObject(SequoiadbHelper.SEQUOIADB_MODIFIER_SET, newBatchInfo);
            BSONObject matcher = new BasicBSONObject();
            addBatchIdAndCreateMonth(matcher, batchId);

            return updateAndCheck(matcher, updator);
        }
        catch (SdbMetasourceException e) {
            logger.error("update failed:table=" + getCsName() + "." + getClName()
                    + ",batchId=" + batchId);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "update failed:table=" + getCsName() + "." + getClName()
                    + ",batchId=" + batchId, e);
        }
    }

    @Override
    public void attachFile(String batchId, String fileId, String updateUser) throws SdbMetasourceException {
        try {
            BSONObject push = new BasicBSONObject(FieldName.Batch.FIELD_FILES, 
                    new BasicBSONObject(FieldName.FIELD_CLFILE_ID, fileId));
            BSONObject updator = new BasicBSONObject(SequoiadbHelper.SEQUOIADB_MODIFIER_PUSH, push);
            putModifierInfo(updateUser, updator);
            
            BSONObject matcher = new BasicBSONObject();
            addBatchIdAndCreateMonth(matcher, batchId);
            update(matcher, updator);
        } 
        catch (SdbMetasourceException e) {
            logger.error("attachFile failed:table={}.{},batchId={},fileId={}", 
                    getCsName(), getClName(), batchId, fileId);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "attachFile failed:table=" + getCsName() + "." + getClName()
                    + ",batchId=" + batchId + ",fileId=" + fileId, e);
        }
    }

    @Override
    public void detachFile(String batchId, String fileId, String updateUser) throws SdbMetasourceException {
        try {
            BSONObject pull = new BasicBSONObject(FieldName.Batch.FIELD_FILES, 
                    new BasicBSONObject(FieldName.FIELD_CLFILE_ID, fileId));
            BSONObject updator = new BasicBSONObject(SequoiadbHelper.SEQUOIADB_MODIFIER_PULL, pull);
            putModifierInfo(updateUser, updator);
            BSONObject matcher = new BasicBSONObject();
            addBatchIdAndCreateMonth(matcher, batchId);
            
            update(matcher, updator);
        } 
        catch (SdbMetasourceException e) {
            logger.error("detachFile failed:table={}.{},batchId={},fileId={}", 
                    getCsName(), getClName(), batchId, fileId);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "detachFile failed:table=" + getCsName() + "." + getClName()
                    + ",batchId=" + batchId + ",fileId=" + fileId, e);
        }
        
    }
    
    private void addBatchIdAndCreateMonth(BSONObject matcher, String batchId)
            throws SdbMetasourceException {
        //IdParser idParser = new IdParser(batchId);
        matcher.put(FieldName.Batch.FIELD_ID, batchId);
        // TODO
        //matcher.put(FieldName.Batch.FIELD_INNER_CREATE_MONTH, idParser.getMonth());
    }

    private void putModifierInfo(String updateUser, BSONObject updator) {
        BasicBSONObject modifier = new BasicBSONObject();
        modifier.put(FieldName.Batch.FIELD_INNER_UPDATE_USER, updateUser);
        modifier.put(FieldName.Batch.FIELD_INNER_UPDATE_TIME, new Date().getTime());
        updator.put(SequoiadbHelper.SEQUOIADB_MODIFIER_SET, modifier);
    }
}
