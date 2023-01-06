package com.sequoiacm.metasource.sequoiadb;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.common.ScmIdParser;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;

public class SequoiadbHelper {
    private static final Logger logger = LoggerFactory.getLogger(SequoiadbHelper.class);

    public static final String SEQUOIADB_MATCHER_DOLLAR0 = "$0";
    public static final String SEQUOIADB_MATCHER_AND = "$and";
    public static final String SEQUOIADB_MATCHER_OR = "$or";
    public static final String SEQUOIADB_MATCHER_IN = "$in";
    public static final String SEQUOIADB_MATCHER_ISNULL = "$isnull";
    public static final String SEQUOIADB_MATCHER_ET = "$et";
    public static final String SEQUOIADB_MATCHER_NE = "$ne";
    public static final String SEQUOIADB_MATCHER_NOT = "$not";
    public static final String SEQUOIADB_MATCHER_GTE  = "$gte";   //NOT SMALL
    public static final String SEQUOIADB_MATCHER_GT = "$gt";    //GREATER
    public static final String SEQUOIADB_MATCHER_LT = "$lt";    //LESS_THAN

    public static final String SEQUOIADB_MODIFIER_PUSH = "$push";
    public static final String SEQUOIADB_MODIFIER_PULL = "$pull";
    public static final String SEQUOIADB_MODIFIER_SET = "$set";
    public static final String SEQUOIADB_MODIFIER_REPLACE = "$replace";
    public static final String SEQUOIADB_MODIFIER_UNSET = "$unset";
    public static final String SEQUOIADB_MODIFIER_INC = "$inc";

    public static final String SEQUOIADB_CATALOG_NAME_FIELD = "Name";
    public static final String SEQUOIADB_CATALOG_MAINCL_FIELD = "MainCLName";

    private static final int SDB_IXM_CREATING = -387;         //DB error code
    private static final int SDB_IXM_COVER_CREATING = -389;   //DB error code

    public static void closeCursor(DBCursor cursor) {
        try {
            if (null != cursor) {
                cursor.close();
            }
        }
        catch (Exception e) {
            logger.warn("close cursor failed", e);
        }
    }

    public static void createCL(Sequoiadb sdb, String csName, String clName, BSONObject options)
            throws SdbMetasourceException {
        try {
            logger.info("creating cl:clName=" + csName + "." + clName + ", options=" + options);
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            cs.createCollection(clName, options);
        }
        catch (BaseException e) {
            if (e.getErrorCode() != SDBError.SDB_DMS_EXIST.getErrorCode()) {
                throw new SdbMetasourceException(e.getErrorCode(),
                        "csName=" + csName + ",clName=" + clName + ",options=" + options,
                        e);
            }
            else {
                // SDB_DMS_EXIST, success do nothing here.
            }
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(), "createcl failed:cs="
                    + csName + ",cl=" + clName + ",options=" + options, e);
        }
    }

    public static void attachCL(Sequoiadb sdb, String csName, String mainCL, String subClFullName,
            BSONObject options) throws SdbMetasourceException {
        try {
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(mainCL);
            if (null == cl) {
                throw new SdbMetasourceException(SDBError.SDB_DMS_NOTEXIST.getErrorCode(),
                        "getCollection failed:cl=" + csName + "." + mainCL);
            }

            cl.attachCollection(subClFullName, options);
        }
        catch (BaseException e) {
            if (e.getErrorCode() != SDBError.SDB_RELINK_SUB_CL.getErrorCode()) {
                throw new SdbMetasourceException(e.getErrorCode(), "attach cl failed:mainCL="
                        + csName + "." + mainCL + ",subCL=" + subClFullName, e);
            }
            else {
                // SDB_ERRORCODE_SDB_RELINK_SUB_CL, success do nothing here.
            }
        }
    }

    public static void createIndex(Sequoiadb sdb, String csName, String clName, String indexName,
            BSONObject indexDef, boolean isUnique, boolean enforced) throws SdbMetasourceException {
        try {
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            if (null == cl) {
                throw new SdbMetasourceException(SDBError.SDB_DMS_NOTEXIST.getErrorCode(),
                        "getCollection failed:cl=" + csName + "." + clName);
            }

            logger.info("creating index:table={}.{},indexName={},indexDef={}", csName, clName,
                    indexName, indexDef);
            cl.createIndex(indexName, indexDef, isUnique, enforced);
        }
        catch (BaseException e) {
            if (e.getErrorCode() != SDBError.SDB_IXM_REDEF.getErrorCode()
                    && e.getErrorCode() != SDBError.SDB_IXM_EXIST_COVERD_ONE.getErrorCode()
                    && e.getErrorCode() != SDB_IXM_CREATING
                    && e.getErrorCode() != SDB_IXM_COVER_CREATING) {
                throw new SdbMetasourceException(e.getErrorCode(),
                        "create index failed:table=" + csName + "." + clName + ",indexName="
                                + indexName + ",indexDef=" + indexDef,
                        e);
            }
            else {
                // SDB_ERRORCODE_SDB_IXM_REDEF, success do nothing here.
                logger.warn("create index failed:table={}.{},indexName={},indexDef={}", csName,
                        clName, indexName, indexDef, e);
            }
        }
    }

    private static boolean checkIndex(Sequoiadb sequoiadb, String csClName, String fieldName) {
        if (csClName == null || csClName.isEmpty()) {
            return false;
        }
        String[] csCl = csClName.split("\\.");
        if (csCl.length != 2) {
            return false;
        }
        DBCursor indexes = null;
        try {
            indexes = sequoiadb.getCollectionSpace(csCl[0]).getCollection(csCl[1]).getIndexes();
            while (indexes.hasNext()) {
                BSONObject index = indexes.getNext();
                BSONObject indexDef = (BSONObject) index.get("IndexDef");
                BSONObject keyBson = (BSONObject) indexDef.get("key");
                Object[] keyArr = keyBson.keySet().toArray();
                if (keyArr.length > 0 && keyArr[0].equals(fieldName)) {
                    return true;
                }
            }
            return false;
        }
        finally {
            if (indexes != null) {
                indexes.close();
            }
        }
    }

    public static boolean isIndexFieldExist(Sequoiadb sdb, String fieldName, String csName,
            String clName) throws SdbMetasourceException {
        DBCursor snapCursor = null;
        try {
            snapCursor = sdb.getSnapshot(8, new BasicBSONObject("Name", csName + "." + clName),
                    null, null);
            if (snapCursor.hasNext()) {
                BSONObject snapInfo = snapCursor.getNext();
                boolean isMainCl = false;
                if (snapInfo.containsField("IsMainCL")) {
                    isMainCl = (boolean) snapInfo.get("IsMainCL");
                }
                if (!isMainCl) {
                    // 不存在在主子表，检查当前表是否存在索引
                    return checkIndex(sdb, csName + "." + clName, fieldName);
                }
                BasicBSONList subClList = (BasicBSONList) snapInfo.get("CataInfo");
                if (subClList == null || subClList.size() <= 0) {
                    return checkIndex(sdb, csName + "." + clName, fieldName);
                }
                if (subClList.size() == 1) {
                    String subClFullName = (String) ((BSONObject) subClList.get(0))
                            .get("SubCLName");
                    return checkIndex(sdb, subClFullName, fieldName);
                }
                else {
                    // 存在主子表，检查第一个子表和最后一个子表是否存在索引
                    String firstSubClFullName = (String) ((BSONObject) subClList.get(0))
                            .get("SubCLName");
                    String lastSubClFullName = (String) ((BSONObject) subClList
                            .get(subClList.size() - 1)).get("SubCLName");
                    return checkIndex(sdb, firstSubClFullName, fieldName)
                            && checkIndex(sdb, lastSubClFullName, fieldName);
                }
            }
            else {
                return false;
            }
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "failed to check index field, table=" + csName + "." + clName
                            + ", indexFieldName=" + fieldName,
                    e);
        }
        finally {
            if (snapCursor != null) {
                snapCursor.close();
            }
        }

    }

    public static BSONObject addFileIdAndCreateMonth(BSONObject matcher, String fileId)
            throws SdbMetasourceException {
        ScmIdParser idParser = null;
        try {
            idParser = new ScmIdParser(fileId);
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_INVALIDARG.getErrorCode(),
                    "Failed to create IdParser", e);
        }
        matcher.put(FieldName.FIELD_CLFILE_ID, fileId);
        matcher.put(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH, idParser.getMonth());
        return matcher;
    }

    private static BSONObject generateOneSiteInList(int siteId, long time, int wsVersion, String tableName) {
        BSONObject oneSite = new BasicBSONObject();
        oneSite.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST_ID, siteId);
        oneSite.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST_TIME, time);
        oneSite.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST_CREATE_TIME, time);
        oneSite.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST_WS_VERSION, wsVersion);
        if( tableName != null && !tableName.isEmpty()){
            oneSite.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST_TABLE_NAME, tableName);
        }
        return new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_SITE_LIST, oneSite);
    }

    public static BSONObject pushOneSiteToList(int siteId, long time, int wsVersion, String tableName) {
        BSONObject siteInfo = generateOneSiteInList(siteId, time, wsVersion, tableName);
        return new BasicBSONObject(SEQUOIADB_MODIFIER_PUSH, siteInfo);
    }

    public static BSONObject dollarSiteNotInList(int siteId) {
        BSONObject listSiteId = new BasicBSONObject();
        String listSiteKey = FieldName.FIELD_CLFILE_FILE_SITE_LIST + "." + SEQUOIADB_MATCHER_DOLLAR0
                + "." + FieldName.FIELD_CLFILE_FILE_SITE_LIST_ID;

        // {"site_list.$0.site_id" : siteId}
        listSiteId.put(listSiteKey, siteId);

        BSONObject array = new BasicBSONList();
        array.put("0", listSiteId);

        // {"$not":[{"site_list.$0.site_id" : siteId}]}
        BSONObject result = new BasicBSONObject(SEQUOIADB_MATCHER_NOT, array);

        return result;
    }

    public static BSONObject pullNullFromList() {
        BSONObject nullInList = new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_SITE_LIST, null);
        BSONObject pull = new BasicBSONObject(SEQUOIADB_MODIFIER_PULL, nullInList);
        return pull;
    }

    public static BSONObject dollarSiteInList(int siteId) {
        BSONObject dollarSite = new BasicBSONObject();
        String dollarSiteKey = FieldName.FIELD_CLFILE_FILE_SITE_LIST + "."
                + SEQUOIADB_MATCHER_DOLLAR0 + "." + FieldName.FIELD_CLFILE_FILE_SITE_LIST_ID;
        dollarSite.put(dollarSiteKey, siteId);

        return dollarSite;
    }

    public static BSONObject unsetDollar0FromList() {
        BSONObject dollarInList = new BasicBSONObject();
        dollarInList.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST + "." + SEQUOIADB_MATCHER_DOLLAR0,
                "");

        return new BasicBSONObject(SEQUOIADB_MODIFIER_UNSET, dollarInList);
    }

    public static void removeNullElementFromList(BSONObject obj, String arrayKey) {
        if (obj == null) {
            return;
        }
        BasicBSONList list = (BasicBSONList) obj.get(arrayKey);
        if (list == null) {
            return;
        }
        while (list.contains(null)) {
            list.remove(null);
        }
    }
}
