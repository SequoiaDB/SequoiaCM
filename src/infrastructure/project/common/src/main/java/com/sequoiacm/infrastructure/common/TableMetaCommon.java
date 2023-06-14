package com.sequoiacm.infrastructure.common;

import com.sequoiacm.common.FieldName;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Domain;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TableMetaCommon {

    private static final Logger logger = LoggerFactory.getLogger(TableMetaCommon.class);
    private static final String CS_METADATA_EXTRA = "_META";

    private static List<String> getConformCsList(Set<String> csInDomain,
            List<String> extraCsList, String wsMetaCsName) {
        List<String> conformCsList = new ArrayList<String>();
        if (csInDomain.contains(wsMetaCsName)) {
            conformCsList.add(wsMetaCsName);
        }
        for (String extraCs : extraCsList) {
            if (csInDomain.contains(extraCs)) {
                conformCsList.add(extraCs);
            }
        }
        return conformCsList;
    }

    // 返回 false 表示 cs 存在
    private static boolean createExtraMetaCs(Sequoiadb db, BSONObject ws, String csName) {
        logger.info("creating workspace extra meta cs:cs={}", csName);
        BSONObject metaLocation = BsonUtils.getBSONChecked(ws,
                FieldName.FIELD_CLWORKSPACE_META_LOCATION);
        String domain = BsonUtils.getStringChecked(metaLocation,
                FieldName.FIELD_CLWORKSPACE_LOCATION_DOMAIN);
        BSONObject csOption = new BasicBSONObject("Domain", domain);
        BSONObject metaOptions = BsonUtils.getBSON(metaLocation,
                FieldName.FIELD_CLWORKSPACE_META_OPTIONS);
        if (metaOptions != null) {
            BSONObject customCsOptions = BsonUtils.getBSON(metaOptions,
                    FieldName.FIELD_CLWORKSPACE_META_CS);
            if (customCsOptions != null) {
                csOption.putAll(customCsOptions);
            }
        }
        try {
            db.createCollectionSpace(csName, csOption);
            return true;
        }
        catch (BaseException e) {
            if (e.getErrorCode() != SDBError.SDB_DMS_CS_EXIST.getErrorCode()) {
                throw e;
            }
            return false;
        }
    }

    private static String getNextExtraCsName(List<String> extraCsList, String wsMetaCsName) {
        int newExtraCsNum = 1;
        for (int i = extraCsList.size() - 1; i >= 0; i--) {
            String cs = extraCsList.get(i);
            try {
                String numStr = cs.substring(cs.lastIndexOf("_") + 1);
                newExtraCsNum = Integer.parseInt(numStr) + 1;
                break;
            }
            catch (Exception e) {
                // 忽略不规则集合名导致的异常
            }
        }
        return wsMetaCsName + "_" + newExtraCsNum;
    }

    private static Set<String> listCsInDomain(Sequoiadb sdb, String domainName) {
        DBCursor dbCursor = null;
        try {
            Domain domain = sdb.getDomain(domainName);
            Set<String> set = new HashSet<String>();
            dbCursor = domain.listCSInDomain();
            while (dbCursor.hasNext()) {
                BSONObject csBson = dbCursor.getNext();
                set.add(BsonUtils.getString(csBson, "Name"));
            }
            return set;
        }
        finally {
            if (null != dbCursor) {
                dbCursor.close();
            }
        }
    }

    public static List<String> getExtraCsList(BSONObject wsRecord) {
        List<String> ret = new ArrayList<String>();
        BasicBSONList extraMetaCS = BsonUtils.getArray(wsRecord,
                FieldName.FIELD_CLWORKSPACE_EXTRA_META_CS);
        if (extraMetaCS == null) {
            return ret;
        }
        for (Object csName : extraMetaCS) {
            ret.add((String) csName);
        }
        return ret;
    }

    public static BSONObject genBucketTableOption() {
        BSONObject clOption = new BasicBSONObject();
        clOption.put("ShardingType", "hash");
        clOption.put("AutoSplit", true);
        BSONObject clShardingKey = new BasicBSONObject(FieldName.BucketFile.FILE_NAME, 1);
        clOption.put("ShardingKey", clShardingKey);
        return clOption;
    }

    private static String getDomain(BSONObject wsRecord) {
        BSONObject metaLocation = BsonUtils.getBSONChecked(wsRecord,
                FieldName.FIELD_CLWORKSPACE_META_LOCATION);
        String domain = BsonUtils.getStringChecked(metaLocation,
                FieldName.FIELD_CLWORKSPACE_LOCATION_DOMAIN);
        return domain;
    }

    public static TableCreatedResult createTable(Sequoiadb sdb, BSONObject wsRecord, String wsName,
            String clName, BSONObject clOptions, boolean isIgnoreClExistErr) throws Exception {
        List<String> extraCsList = getExtraCsList(wsRecord);
        String domain = getDomain(wsRecord);
        Set<String> csInDomain = listCsInDomain(sdb, domain);
        List<String> conformCsList = getConformCsList(csInDomain, extraCsList,
                wsName + CS_METADATA_EXTRA);
        for (String cs : conformCsList) {
            if (createCl(sdb, cs, clName, clOptions, isIgnoreClExistErr)) {
                return new TableCreatedResult(cs, true, clName);
            }
        }
        String newExtraCsName = getNextExtraCsName(extraCsList, wsName + CS_METADATA_EXTRA);
        boolean isCreateCs = createExtraMetaCs(sdb, wsRecord, newExtraCsName);
        try {
            if (!createCl(sdb, newExtraCsName, clName, clOptions, isIgnoreClExistErr)) {
                throw new Exception("failed to create cl in extra meta cs,cs: " + newExtraCsName
                        + ", cl: " + clName + ", clOptions: " + clOptions);
            }
            return new TableCreatedResult(newExtraCsName, false, clName);
        }
        catch (Exception e) {
            if (isCreateCs) {
                // 在新的CS上创建集合失败，删除CS
                dropCSSilence(sdb, newExtraCsName, true);
            }
            throw e;
        }
    }

    private static boolean createCl(Sequoiadb sdb, String csName, String clName,
            BSONObject clOptions, boolean isIgnoreClExistErr) {
        try {
            logger.info(
                    "creating cl:cl=" + csName + "." + clName + ",options=" + clOptions.toString());
            CollectionSpace collectionSpace = sdb.getCollectionSpace(csName);
            collectionSpace.createCollection(clName, clOptions);
            return true;
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_DMS_NOSPC.getErrorCode()) {
                // 无空间
                return false;
            }
            if (e.getErrorCode() == SDBError.SDB_DMS_EXIST.getErrorCode() && isIgnoreClExistErr) {
                // collection already exist
                return true;
            }
            throw e;
        }
    }

    private static void dropCSSilence(Sequoiadb db, String cs, boolean skipRecycleBin) {
        try {
            dropCSWithSkipRecycleBin(db, cs, skipRecycleBin);
        }
        catch (Exception e) {
            logger.warn("failed to drop cs:{}", cs, e);
        }
    }

    public static void dropCSWithSkipRecycleBin(Sequoiadb db, String cs, boolean skipRecycleBin) {
        BSONObject options = new BasicBSONObject("SkipRecycleBin", skipRecycleBin);
        try {
            db.dropCollectionSpace(cs, options);
        }
        catch (BaseException e) {
            // 部分 SDB 版本 dropCS 不支持 SkipRecycleBin 参数
            if (e.getErrorCode() != SDBError.SDB_INVALIDARG.getErrorCode()) {
                throw e;
            }
            logger.warn("Failed to drop cs:{}, try to drop it again", cs, e);
            db.dropCollectionSpace(cs);
        }
    }

    public static void dropCLWithSkipRecycleBin(CollectionSpace cs, String clName,
            boolean skipRecycleBin) {
        cs.dropCollection(clName);
// 回退sdb驱动至349，dropCollection不支持SkipRecycleBin参数：SEQUOIACM-1411
//        BSONObject options = new BasicBSONObject("SkipRecycleBin", skipRecycleBin);
//        try {
//            cs.dropCollection(clName, options);
//        }
//        catch (BaseException e) {
//            // 部分 SDB 版本 dropCL 不支持 SkipRecycleBin 参数
//            if (e.getErrorCode() != SDBError.SDB_INVALIDARG.getErrorCode()) {
//                throw e;
//            }
//            logger.warn("Failed to drop cl:{}, try to drop it again", cs.getName() + "." + clName,
//                    e);
//            cs.dropCollection(clName);
//        }
    }
}
