package com.sequoiacm.metasource.sequoiadb.accessor;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.metasource.MetaRelAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;
import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiadb.exception.SDBError;

public class SdbRelAccessor extends SdbMetaAccessor implements MetaRelAccessor {
    private Logger logger = LoggerFactory.getLogger(SdbRelAccessor.class);

    // NOTE: this index name define in
    // com.sequoiacm.config.framework.workspace.metasource.WorkspaceMetaServiceSdbImpl
    private static final BSONObject QUERY_REL_HINT = (BSONObject) JSON
            .parse("{'1':'idx_name_pid'}");

    public SdbRelAccessor(SdbMetaSource metasource, String csName, String clName,
            TransactionContext context) {
        super(metasource, csName, clName, context);

    }

    @Override
    public void insert(BSONObject insertor) throws ScmMetasourceException {
        try {
            super.insert(insertor);
        }catch (SdbMetasourceException e) {
            if (e.getErrcode() == SDBError.SDB_IXM_DUP_KEY.getErrorCode()) {
                e.setScmError(ScmError.FILE_EXIST);
            }
            throw e;
        }
    }

    @Override
    public void updateRel(String fileId, String dirId, String fileName, BSONObject newInfo)
            throws SdbMetasourceException {
        try {
            BSONObject matcher = new BasicBSONObject();
            matcher.put(FieldName.FIELD_CLREL_FILEID, fileId);
            matcher.put(FieldName.FIELD_CLREL_FILENAME, fileName);
            matcher.put(FieldName.FIELD_CLREL_DIRECTORY_ID, dirId);

            //the dirId is shardingKey, can not update it directly
            if (newInfo.containsField(FieldName.FIELD_CLREL_DIRECTORY_ID)) {
                // update dirId, we need transaction
                if (!super.isInTransaction()) {
                    throw new ScmMetasourceException(
                            "not in transaction,update directory id failed:fileId="
                                    + fileId + ",updator=" + newInfo);
                }

                // lock the record by update it's fileId
                BSONObject fileIdBSON = new BasicBSONObject(FieldName.FIELD_CLREL_FILEID, fileId);
                BSONObject fileIdUpdator = new BasicBSONObject(
                        SequoiadbHelper.SEQUOIADB_MODIFIER_SET, fileIdBSON);
                BSONObject relRecord = super.queryAndUpdate(matcher, fileIdUpdator,
                        QUERY_REL_HINT);
                if(relRecord == null) {
                    // the file was deleted, just return.
                    logger.debug("update rel record failed,record not exists:fileId={}" , fileId);
                    return;
                }
                //this relRecord can not be modified until we commit the transaction.

                if(!relRecord.get(FieldName.FIELD_CLREL_DIRECTORY_ID).
                        equals(newInfo.get(FieldName.FIELD_CLREL_DIRECTORY_ID))) {
                    BSONObject newRelRecord = new BasicBSONObject();
                    newRelRecord.putAll(relRecord);
                    newRelRecord.putAll(newInfo);
                    newRelRecord.removeField("_id");
                    //insert new relRecord
                    super.insert(newRelRecord);
                    //delete old relRecord
                    super.delete(relRecord);
                    return;
                }

                //the dirId of relRecord is equals newInfo, just update it directly.
            }

            BSONObject updator = new BasicBSONObject();
            updator.put(SequoiadbHelper.SEQUOIADB_MODIFIER_SET, newInfo);
            super.update(matcher, updator, QUERY_REL_HINT);
        }
        catch (SdbMetasourceException e) {
            if (e.getErrcode() == SDBError.SDB_IXM_DUP_KEY.getErrorCode()) {
                e.setScmError(ScmError.FILE_EXIST);
            }
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "update file failed:table=" + getCsName() + "." + getClName() + ",fileId="
                            + fileId,
                            e);
        }
    }

    @Override
    public void deleteRel(String fileId, String dirId, String fileName)
            throws SdbMetasourceException {
        try {
            BSONObject deletor = new BasicBSONObject();
            deletor.put(FieldName.FIELD_CLREL_FILEID, fileId);
            deletor.put(FieldName.FIELD_CLREL_DIRECTORY_ID, dirId);
            deletor.put(FieldName.FIELD_CLREL_FILENAME, fileName);
            super.delete(deletor, QUERY_REL_HINT);
        }
        catch (SdbMetasourceException e) {
            logger.error("delete relation failed:table={}.{},fileId={}", getCsName(), getClName(),
                    fileId);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "delete relation failed:table=" + getCsName() + "." + getClName() + ",fileId="
                            + fileId,
                            e);
        }
    }

}
