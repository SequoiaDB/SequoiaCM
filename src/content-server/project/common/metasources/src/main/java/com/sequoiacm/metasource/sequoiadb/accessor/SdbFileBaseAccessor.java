package com.sequoiacm.metasource.sequoiadb.accessor;

import java.util.Date;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;
import com.sequoiacm.metasource.sequoiadb.FileCursorWithNullSiteFilter;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;
import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiacm.metasource.sequoiadb.config.SdbClFileInfo;
import com.sequoiacm.metasource.sequoiadb.config.SdbMetaSourceLocation;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.SDBError;

public class SdbFileBaseAccessor extends SdbMetaAccessor {
    private static final Logger logger = LoggerFactory.getLogger(SdbFileBaseAccessor.class);
    private SdbMetaSourceLocation location = null;

    public SdbFileBaseAccessor(SdbMetaSourceLocation location, SdbMetaSource metasource,
            String csName, String clName, TransactionContext context) {
        super(metasource, csName, clName, context);
        this.location = location;
    }

    @Override
    public MetaCursor query(BSONObject matcher, BSONObject selector, BSONObject orderBy)
            throws SdbMetasourceException {
        MetaCursor cursor = super.query(matcher, selector, orderBy);
        if(selector != null && !selector.containsField(FieldName.FIELD_CLFILE_FILE_SITE_LIST)) {
            return cursor;
        }

        try {
            return new FileCursorWithNullSiteFilter(cursor);
        }
        catch(Exception e) {
            cursor.close();
            throw e;
        }
    }

    @Override
    public BSONObject queryOne(BSONObject matcher, BSONObject selector, BSONObject orderBy)
            throws ScmMetasourceException {
        BSONObject record = super.queryOne(matcher, selector, orderBy);
        SequoiadbHelper.removeNullElementFromList(record, FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        return record;
    }

    private void createSubHistoryFile(Sequoiadb sdb, Date createDate) throws SdbMetasourceException {
        SdbClFileInfo metaCLInfo = location.getClFileInfo(getClName(), createDate);
        String subClName = metaCLInfo.getClHistoryName();
        String clFullName = getCsName() + "." + subClName;
        try {
            BSONObject options = generatorClFileOptions();
            options.putAll(metaCLInfo.getClOptions());
            logger.info(
                    "creating cl:cl=" + getCsName() + "." + subClName + ",options=" + options.toString());
            SequoiadbHelper.createCL(sdb, getCsName(), subClName, options);

            BSONObject indexDef = new BasicBSONObject();
            indexDef.put(FieldName.FIELD_CLFILE_ID, 1);
            indexDef.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, 1);
            indexDef.put(FieldName.FIELD_CLFILE_MINOR_VERSION, 1);
            SequoiadbHelper.createIndex(sdb, getCsName(), subClName,
                    "idx_" + subClName + "_" + FieldName.FIELD_CLFILE_ID + "_version",
                    indexDef, true, false);

            indexDef = new BasicBSONObject();
            indexDef.put(FieldName.FIELD_CLFILE_FILE_DATA_ID, 1);
            SequoiadbHelper.createIndex(sdb, getCsName(), subClName,
                    "idx_" + subClName + "_" + FieldName.FIELD_CLFILE_FILE_DATA_ID, indexDef,
                    false, false);

            BSONObject lowb = new BasicBSONObject();
            lowb.put(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH, metaCLInfo.getLowMonth());
            BSONObject upperb = new BasicBSONObject();
            upperb.put(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH, metaCLInfo.getUpperMonth());

            options = new BasicBSONObject();
            options.put("LowBound", lowb);
            options.put("UpBound", upperb);
            logger.info("attaching cl:cl=" + clFullName + ",options=" + options.toString());
            SequoiadbHelper.attachCL(sdb, getCsName(), getClName() + "_HISTORY",
                    clFullName, options);
        }
        catch (SdbMetasourceException e) {
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "create file table failed:cl=" + clFullName, e);
        }
    }

    private BSONObject generatorClFileOptions() {
        BSONObject key = new BasicBSONObject(FieldName.FIELD_CLFILE_ID, 1);
        BSONObject options = new BasicBSONObject();
        options.put("ShardingType", "hash");
        options.put("ShardingKey", key);
        options.put("Compressed", true);
        options.put("CompressionType", "lzw");
        options.put("ReplSize", -1);
        options.put("AutoSplit", true);
        options.put("EnsureShardingIndex", false);

        return options;
    }

    private void createSubCl(Sequoiadb sdb,Date createDate) throws SdbMetasourceException {
        SdbClFileInfo metaCLInfo = location.getClFileInfo(getClName(), createDate);
        String subClName = metaCLInfo.getClName();
        String clFullName = getCsName() + "." + subClName;
        try {
            BSONObject options = generatorClFileOptions();
            options.putAll(metaCLInfo.getClOptions());
            logger.info("creating cl:cl={}.{},options={}", getCsName(), subClName,
                    options.toString());
            SequoiadbHelper.createCL(sdb, getCsName(), subClName, options);

            BSONObject indexDef = new BasicBSONObject();
            indexDef.put(FieldName.FIELD_CLFILE_ID, 1);
            SequoiadbHelper.createIndex(sdb, getCsName(), subClName,
                    "idx_" + subClName + "_" + FieldName.FIELD_CLFILE_ID, indexDef, true, false);

            indexDef = new BasicBSONObject();
            indexDef.put(FieldName.FIELD_CLFILE_FILE_DATA_ID, 1);
            SequoiadbHelper.createIndex(sdb, getCsName(), subClName,
                    "idx_" + subClName + "_" + FieldName.FIELD_CLFILE_FILE_DATA_ID, indexDef, false,
                    false);

            indexDef = new BasicBSONObject();
            indexDef.put(FieldName.FIELD_CLFILE_NAME, 1);
            SequoiadbHelper.createIndex(sdb, getCsName(), subClName,
                    "idx_" + subClName + "_" + FieldName.FIELD_CLREL_FILENAME, indexDef, false,
                    false);

            indexDef = new BasicBSONObject();
            indexDef.put(FieldName.FIELD_CLFILE_INNER_CREATE_TIME, 1);
            SequoiadbHelper.createIndex(sdb, getCsName(), subClName, "idx_" + subClName + "_"
                    + FieldName.FIELD_CLFILE_INNER_CREATE_TIME, indexDef, false, false);

            BSONObject lowb = new BasicBSONObject();
            lowb.put(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH, metaCLInfo.getLowMonth());
            BSONObject upperb = new BasicBSONObject();
            upperb.put(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH, metaCLInfo.getUpperMonth());

            options = new BasicBSONObject();
            options.put("LowBound", lowb);
            options.put("UpBound", upperb);
            logger.info("attaching cl:cl=" + clFullName + ",options=" + options.toString());
            SequoiadbHelper.attachCL(sdb, getCsName(), getClName(), clFullName,
                    options);
        }
        catch (SdbMetasourceException e) {
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "create file table failed:cl=" + clFullName, e);
        }
    }

    public BSONObject delete(String fileId, int majorVersion, int minorVersion)
            throws SdbMetasourceException {
        try {
            BSONObject deletor = new BasicBSONObject();
            SequoiadbHelper.addFileIdAndCreateMonth(deletor, fileId);
            if (majorVersion != -1 && minorVersion != -1) {
                deletor.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, majorVersion);
                deletor.put(FieldName.FIELD_CLFILE_MINOR_VERSION, minorVersion);
            }

            return queryAndDelete(deletor);
        }
        catch (SdbMetasourceException e) {
            logger.error("delete failed:table=" + getCsName() + "." + getClName() + ",fileId="
                    + fileId + ",majorVersion=" + majorVersion + ",minorVersion=" + minorVersion);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "delete failed:table=" + getCsName() + "." + getClName() + ",fileId=" + fileId
                    + ",majorVersion=" + majorVersion + ",minorVersion=" + minorVersion,
                    e);
        }
    }

    public boolean addToSiteList(String fileId, int majorVersion, int minorVersion, int siteId,
            Date date) throws SdbMetasourceException {
        try {
            BSONObject matcher = SequoiadbHelper.dollarSiteNotInList(siteId);
            // matcher.put(FieldName.FIELD_CLFILE_ID, fileId);
            SequoiadbHelper.addFileIdAndCreateMonth(matcher, fileId);
            matcher.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, majorVersion);
            matcher.put(FieldName.FIELD_CLFILE_MINOR_VERSION, minorVersion);

            BSONObject updator = SequoiadbHelper.pushOneSiteToList(siteId,date.getTime());
            return updateAndCheck(matcher, updator);
        }
        catch (SdbMetasourceException e) {
            logger.error("addToSiteList failed:table=" + getCsName() + "." + getClName()
            + ",fileId=" + fileId + ",majorVersion=" + majorVersion
            + ",minorVersion=" + minorVersion + ",siteId=" + siteId);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "addToSiteList failed:table=" + getCsName() + "." + getClName()
                    + ",fileId=" + fileId + ",majorVersion=" + majorVersion
                    + ",minorVersion=" + minorVersion + ",siteId=" + siteId, e);
        }
    }

    public boolean deleteNullFromSiteList(String fileId, int majorVersion, int minorVersion)
            throws SdbMetasourceException {
        try {
            BSONObject matcher = new BasicBSONObject();
            SequoiadbHelper.addFileIdAndCreateMonth(matcher, fileId);
            BSONObject updator = SequoiadbHelper.pullNullFromList();
            return updateAndCheck(matcher, updator);
        }
        catch (SdbMetasourceException e) {
            logger.error("deleteNullFromSiteList failed:table=" + getCsName() + "." + getClName()
            + ",fileId=" + fileId + ",majorVersion=" + majorVersion
            + ",minorVersion=" + minorVersion);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "deleteNullFromSiteList failed:table=" + getCsName() + "." + getClName()
                    + ",fileId=" + fileId + ",majorVersion=" + majorVersion
                    + ",minorVersion=" + minorVersion, e);
        }
    }

    public boolean updateAccessTime(String fileId, int majorVersion, int minorVersion, int siteId,
            Date date) throws SdbMetasourceException {
        try {
            BSONObject condition1 = new BasicBSONObject();
            condition1.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST + "."
                    + SequoiadbHelper.SEQUOIADB_MATCHER_DOLLAR0 + "."
                    + FieldName.FIELD_CLFILE_FILE_SITE_LIST_ID, siteId);

            // {id:xx, majorVersion:1, minorVersion:0}
            BSONObject condition2 = new BasicBSONObject();
            // condition2.put(FieldName.FIELD_CLFILE_ID, fileId);
            SequoiadbHelper.addFileIdAndCreateMonth(condition2, fileId);
            condition2.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, majorVersion);
            condition2.put(FieldName.FIELD_CLFILE_MINOR_VERSION, minorVersion);

            // [condition1, condition2]
            BasicBSONList bsonList = new BasicBSONList();
            bsonList.add(condition1);
            bsonList.add(condition2);

            // {$and:[condition1, condition2]}
            BSONObject matcher = new BasicBSONObject();
            matcher.put(SequoiadbHelper.SEQUOIADB_MATCHER_AND, bsonList);

            BSONObject lasstAccessTime = new BasicBSONObject();
            lasstAccessTime.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST + "."
                    + SequoiadbHelper.SEQUOIADB_MATCHER_DOLLAR0 + "."
                    + FieldName.FIELD_CLFILE_FILE_SITE_LIST_TIME, date.getTime());
            BSONObject updator = new BasicBSONObject(SequoiadbHelper.SEQUOIADB_MODIFIER_SET,
                    lasstAccessTime);

            return updateAndCheck(matcher, updator);
        }
        catch (SdbMetasourceException e) {
            logger.error("updateAccessTime failed:table=" + getCsName() + "." + getClName()
            + ",fileId=" + fileId + ",majorVersion=" + majorVersion
            + ",minorVersion=" + minorVersion);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "updateAccessTime failed:table=" + getCsName() + "." + getClName()
                    + ",fileId=" + fileId + ",majorVersion=" + majorVersion
                    + ",minorVersion=" + minorVersion, e);
        }
    }

    public boolean deleteFromSiteList(String fileId, int majorVersion, int minorVersion, int siteId)
            throws SdbMetasourceException {
        try {
            BSONObject matcher = SequoiadbHelper.dollarSiteInList(siteId);
            // matcher.put(FieldName.FIELD_CLFILE_ID, fileId);
            SequoiadbHelper.addFileIdAndCreateMonth(matcher, fileId);
            matcher.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, majorVersion);
            matcher.put(FieldName.FIELD_CLFILE_MINOR_VERSION, minorVersion);

            BSONObject updator = SequoiadbHelper.unsetDollar0FromList();
            if(updateAndCheck(matcher, updator)) {
                return deleteNullFromSiteList(fileId, majorVersion, minorVersion);
            }
            return false;
        }
        catch (SdbMetasourceException e) {
            logger.error("deleteFromSiteList failed:table=" + getCsName() + "." + getClName()
            + ",fileId=" + fileId + ",majorVersion=" + majorVersion
            + ",minorVersion=" + minorVersion);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "deleteFromSiteList failed:table=" + getCsName() + "." + getClName()
                    + ",fileId=" + fileId + ",majorVersion=" + majorVersion
                    + ",minorVersion=" + minorVersion, e);
        }
    }

    public void createFileTable(BSONObject file) throws SdbMetasourceException{
        long createTime = (long) file.get(FieldName.FIELD_CLFILE_INNER_CREATE_TIME);
        Date createDate = new Date(createTime);

        Sequoiadb sdb = null;
        try {
            sdb = getConnection();
            createSubCl(sdb, createDate);
            createSubHistoryFile(sdb, createDate);
        }
        finally {
            releaseConnection(sdb);
        }
    }
}
