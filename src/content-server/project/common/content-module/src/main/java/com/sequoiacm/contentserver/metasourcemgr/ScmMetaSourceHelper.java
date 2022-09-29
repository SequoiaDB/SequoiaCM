package com.sequoiacm.contentserver.metasourcemgr;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentServerMapping;
import com.sequoiacm.contentserver.site.ScmSite;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.ScmIdParser;
import com.sequoiacm.infrastructure.security.sign.SignUtil;
import com.sequoiacm.metasource.ContentModuleMetaSource;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import java.util.*;

public class ScmMetaSourceHelper {
    public static final String SEQUOIADB_MATCHER_DOLLAR0 = "$0";
    public static final String SEQUOIADB_MATCHER_AND = "$and";
    public static final String SEQUOIADB_MATCHER_OR = "$or";
    public static final String SEQUOIADB_MATCHER_IN = "$in";
    public static final String SEQUOIADB_MATCHER_ISNULL = "$isnull";
    public static final String SEQUOIADB_MATCHER_ET = "$et";
    public static final String SEQUOIADB_MATCHER_NOT = "$not";
    public static final String SEQUOIADB_MATCHER_NE = "$ne";

    public static final String SEQUOIADB_MODIFIER_PUSH = "$push";
    public static final String SEQUOIADB_MODIFIER_PULL = "$pull";
    public static final String SEQUOIADB_MODIFIER_SET = "$set";
    public static final String SEQUOIADB_MODIFIER_REPLACE = "$replace";
    public static final String SEQUOIADB_MODIFIER_UNSET = "$unset";

    public static final int QUERY_IN_FILE_TABLE = 1;
    public static final int QUERY_IN_RELATION_TABLE = 2;

    public static final Map<String, String> FILE_FIELD_MAP_REL_FIELD = new HashMap<>();
    static {
        FILE_FIELD_MAP_REL_FIELD.put(FieldName.FIELD_CLFILE_DIRECTORY_ID,
                FieldName.FIELD_CLREL_DIRECTORY_ID);
        FILE_FIELD_MAP_REL_FIELD.put(FieldName.FIELD_CLFILE_NAME, FieldName.FIELD_CLREL_FILENAME);
        FILE_FIELD_MAP_REL_FIELD.put(FieldName.FIELD_CLFILE_ID, FieldName.FIELD_CLREL_FILEID);
        FILE_FIELD_MAP_REL_FIELD.put(FieldName.FIELD_CLFILE_INNER_USER, FieldName.FIELD_CLREL_USER);
        FILE_FIELD_MAP_REL_FIELD.put(FieldName.FIELD_CLFILE_INNER_UPDATE_USER,
                FieldName.FIELD_CLREL_UPDATE_USER);
        FILE_FIELD_MAP_REL_FIELD.put(FieldName.FIELD_CLFILE_INNER_CREATE_TIME,
                FieldName.FIELD_CLREL_CREATE_TIME);
        FILE_FIELD_MAP_REL_FIELD.put(FieldName.FIELD_CLFILE_INNER_UPDATE_TIME,
                FieldName.FIELD_CLREL_UPDATE_TIME);
        FILE_FIELD_MAP_REL_FIELD.put(FieldName.FIELD_CLFILE_MAJOR_VERSION,
                FieldName.FIELD_CLREL_MAJOR_VERSION);
        FILE_FIELD_MAP_REL_FIELD.put(FieldName.FIELD_CLFILE_MINOR_VERSION,
                FieldName.FIELD_CLREL_MINOR_VERSION);
        FILE_FIELD_MAP_REL_FIELD.put(FieldName.FIELD_CLFILE_FILE_SIZE,
                FieldName.FIELD_CLREL_FILE_SIZE);
        FILE_FIELD_MAP_REL_FIELD.put(FieldName.FIELD_CLFILE_FILE_MIME_TYPE,
                FieldName.FIELD_CLREL_FILE_MIME_TYPE);
        FILE_FIELD_MAP_REL_FIELD.put(FieldName.FIELD_CLFILE_DELETE_MARKER,
                FieldName.FIELD_CLREL_FILE_DELETE_MARKER);
    }

    public static final Map<String, String> BUCKET_FILE_REL_FIELD = new HashMap<>();
    static {
        BUCKET_FILE_REL_FIELD.put(FieldName.FIELD_CLFILE_NAME, FieldName.BucketFile.FILE_NAME);
        BUCKET_FILE_REL_FIELD.put(FieldName.FIELD_CLFILE_ID, FieldName.BucketFile.FILE_ID);
        BUCKET_FILE_REL_FIELD.put(FieldName.FIELD_CLFILE_INNER_CREATE_TIME,
                FieldName.BucketFile.FILE_CREATE_TIME);
        BUCKET_FILE_REL_FIELD.put(FieldName.FIELD_CLFILE_FILE_SIZE, FieldName.BucketFile.FILE_SIZE);
        BUCKET_FILE_REL_FIELD.put(FieldName.FIELD_CLFILE_MINOR_VERSION,
                FieldName.BucketFile.FILE_MINOR_VERSION);
        BUCKET_FILE_REL_FIELD.put(FieldName.FIELD_CLFILE_MAJOR_VERSION,
                FieldName.BucketFile.FILE_MAJOR_VERSION);
        BUCKET_FILE_REL_FIELD.put(FieldName.FIELD_CLFILE_FILE_MIME_TYPE,
                FieldName.BucketFile.FILE_MIME_TYPE);
        BUCKET_FILE_REL_FIELD.put(FieldName.FIELD_CLFILE_INNER_UPDATE_TIME,
                FieldName.BucketFile.FILE_UPDATE_TIME);
        BUCKET_FILE_REL_FIELD.put(FieldName.FIELD_CLFILE_INNER_USER,
                FieldName.BucketFile.FILE_CREATE_USER);
        BUCKET_FILE_REL_FIELD.put(FieldName.FIELD_CLFILE_FILE_ETAG, FieldName.BucketFile.FILE_ETAG);
        BUCKET_FILE_REL_FIELD.put(FieldName.FIELD_CLFILE_VERSION_SERIAL,
                FieldName.BucketFile.FILE_VERSION_SERIAL);
        BUCKET_FILE_REL_FIELD.put(FieldName.FIELD_CLFILE_DELETE_MARKER,
                FieldName.BucketFile.FILE_DELETE_MARKER);
    }

    public static void closeCursor(MetaCursor cursor) {
        if (null != cursor) {
            cursor.close();
        }
    }

    public static List<ScmSite> getSiteList(ContentModuleMetaSource metasource) throws ScmServerException {
        MetaCursor cursor = null;
        try {
            MetaAccessor siteAccesor = metasource.getSiteAccessor();
            cursor = siteAccesor.query(null, null, null);

            List<ScmSite> siteList = new ArrayList<>();
            while (cursor.hasNext()) {
                BSONObject bo = cursor.getNext();
                ScmSite ssm = new ScmSite(bo);
                siteList.add(ssm);
            }

            return siteList;
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "Failed to get site list", e);
        }
        finally {
            closeCursor(cursor);
        }
    }

    public static List<ScmContentServerMapping> getServerList(ContentModuleMetaSource metasource)
            throws ScmServerException {
        MetaCursor cursor = null;
        try {
            MetaAccessor serverAccesor = metasource.getServerAccessor();
            cursor = serverAccesor.query(null, null, null);

            List<ScmContentServerMapping> serverList = new ArrayList<>();
            while (cursor.hasNext()) {
                BSONObject bo = cursor.getNext();
                ScmContentServerMapping serverMapping = new ScmContentServerMapping(bo);
                serverList.add(serverMapping);
            }

            return serverList;
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "Failed to get server list", e);
        }
        finally {
            closeCursor(cursor);
        }
    }

    public static List<BSONObject> getWorkspaceList(ContentModuleMetaSource metasource)
            throws ScmServerException {

        MetaCursor cursor = null;
        try {
            MetaAccessor workspaceAccesor = metasource.getWorkspaceAccessor();
            cursor = workspaceAccesor.query(null, null,
                    new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_ID, 1));

            List<BSONObject> workspaceList = new ArrayList<>();
            while (cursor.hasNext()) {
                BSONObject bo = cursor.getNext();
                // SEQUOIACM-1055: 解决 3.1.0 版本所创建工作区，可能存在的目录兼容性问题
                if (bo.get(FieldName.FIELD_CLWORKSPACE_ENABLE_DIRECTORY) == null) {
                    BSONObject matcher = new BasicBSONObject();
                    matcher.put(FieldName.FIELD_CLWORKSPACE_ID,
                            bo.get(FieldName.FIELD_CLWORKSPACE_ID));
                    matcher.put(FieldName.FIELD_CLWORKSPACE_ENABLE_DIRECTORY,
                            new BasicBSONObject("$exists", 0));
                    BSONObject updater = new BasicBSONObject();
                    updater.put(FieldName.FIELD_CLWORKSPACE_ENABLE_DIRECTORY, true);
                    bo = workspaceAccesor.queryAndUpdate(matcher,
                            new BasicBSONObject("$set", updater), null, true);
                }
                workspaceList.add(bo);
            }
            return workspaceList;
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "Failed to get workspace list", e);
        }
        finally {
            closeCursor(cursor);
        }
    }

    public static BSONObject createWorkspaceMatcher(String workspace) {
        BSONObject b = new BasicBSONObject();
        b.put(FieldName.FIELD_CLWORKSPACE_NAME, workspace);
        return b;
    }

    public static BSONObject addFileIdAndCreateMonth(BSONObject matcher, String fileId)
            throws ScmServerException {
        ScmIdParser idParser;
        try {
            idParser = new ScmIdParser(fileId);
        }
        catch (Exception e) {
            throw new ScmServerException(ScmError.INVALID_ID, e.getMessage(), e);
        }
        matcher.put(FieldName.FIELD_CLFILE_ID, fileId);
        matcher.put(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH, idParser.getMonth());
        return matcher;
    }

    public static BSONObject notDeleteMarkerMatcher() {
        BasicBSONObject deleteMarkerIsFalse = new BasicBSONObject(
                FieldName.FIELD_CLFILE_DELETE_MARKER, false);
        BasicBSONObject deleteMarkerNotExist = new BasicBSONObject(
                FieldName.FIELD_CLFILE_DELETE_MARKER, new BasicBSONObject("$exists", 0));
        BasicBSONList orArr = new BasicBSONList();
        orArr.add(deleteMarkerIsFalse);
        orArr.add(deleteMarkerNotExist);
        return new BasicBSONObject("$or", orArr);
    }

    public static BSONObject queryOne(MetaAccessor accessor, BSONObject matcher)
            throws ScmServerException {
        try {
            return accessor.queryOne(matcher, null, null);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "Failed to query: " + matcher.toString(),
                    e);
        }
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

    private static BSONObject generateOneSiteInList(int siteId, long time) {
        BSONObject oneSite = new BasicBSONObject();
        oneSite.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST_ID, siteId);
        oneSite.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST_TIME, time);
        oneSite.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST_CREATE_TIME, time);

        return new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_SITE_LIST, oneSite);
    }

    public static BSONObject pushOneSiteToList(int siteId, long time) {
        BSONObject siteInfo = generateOneSiteInList(siteId, time);
        return new BasicBSONObject(SEQUOIADB_MODIFIER_PUSH, siteInfo);
    }

    public static BSONObject pullNullFromList() {
        BSONObject nullInList = new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_SITE_LIST, null);
        BSONObject pull = new BasicBSONObject(SEQUOIADB_MODIFIER_PULL, nullInList);
        return pull;
    }

    public static Object checkExistString(BSONObject obj, String fieldName)
            throws ScmServerException {
        Object value = obj.get(fieldName);
        if (value == null) {
            throw new ScmServerException(ScmError.ATTRIBUTE_FORMAT_ERROR,
                    "field[" + fieldName + "] is not exist!");
        }

        if (!(value instanceof String)) {
            throw new ScmServerException(ScmError.ATTRIBUTE_FORMAT_ERROR,
                    "field[" + fieldName + "] is not String format!");
        }

        String valueStr = (String) value;
        if (valueStr.length() == 0) {
            throw new ScmServerException(ScmError.ATTRIBUTE_FORMAT_ERROR,
                    "field[" + fieldName + "]'s length can't be 0");
        }

        return value;
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

    public static BSONObject createRelUpdatorByFileUpdator(BSONObject fileUpdator) {
        BSONObject relUpdator = new BasicBSONObject();
        for (String key : fileUpdator.keySet()) {
            if (FILE_FIELD_MAP_REL_FIELD.containsKey(key)) {
                relUpdator.put(FILE_FIELD_MAP_REL_FIELD.get(key), fileUpdator.get(key));
            }
        }
        return relUpdator;
    }


    public static BSONObject createBucketFileUpdatorByFileUpdator(BSONObject fileUpdator) {
        BSONObject bucketFileUpdator = new BasicBSONObject();
        for (String key : fileUpdator.keySet()) {
            if (BUCKET_FILE_REL_FIELD.containsKey(key)) {
                bucketFileUpdator.put(BUCKET_FILE_REL_FIELD.get(key), fileUpdator.get(key));
            }
        }
        String md5 = BsonUtils.getString(fileUpdator, FieldName.FIELD_CLFILE_FILE_MD5);
        if (md5 != null) {
            String newEtag = BsonUtils.getString(bucketFileUpdator, FieldName.BucketFile.FILE_ETAG);
            if (newEtag == null || newEtag.length() <= 0) {
                bucketFileUpdator.put(FieldName.BucketFile.FILE_ETAG, SignUtil.toHex(md5));
            }
        }
        return bucketFileUpdator;
    }

    public static BSONObject createRelInsertorByFileInsertor(BSONObject fileInsertor) {
        BSONObject relInsertor = new BasicBSONObject();
        relInsertor.put(FieldName.FIELD_CLREL_CREATE_TIME,
                fileInsertor.get(FieldName.FIELD_CLFILE_INNER_CREATE_TIME));
        relInsertor.put(FieldName.FIELD_CLREL_DIRECTORY_ID,
                fileInsertor.get(FieldName.FIELD_CLFILE_DIRECTORY_ID));
        relInsertor.put(FieldName.FIELD_CLREL_FILE_SIZE,
                fileInsertor.get(FieldName.FIELD_CLFILE_FILE_SIZE));
        relInsertor.put(FieldName.FIELD_CLREL_FILEID, fileInsertor.get(FieldName.FIELD_CLFILE_ID));
        relInsertor.put(FieldName.FIELD_CLREL_FILENAME,
                fileInsertor.get(FieldName.FIELD_CLFILE_NAME));
        relInsertor.put(FieldName.FIELD_CLREL_MAJOR_VERSION,
                fileInsertor.get(FieldName.FIELD_CLFILE_MAJOR_VERSION));
        relInsertor.put(FieldName.FIELD_CLREL_MINOR_VERSION,
                fileInsertor.get(FieldName.FIELD_CLFILE_MINOR_VERSION));
        relInsertor.put(FieldName.FIELD_CLREL_UPDATE_TIME,
                fileInsertor.get(FieldName.FIELD_CLFILE_INNER_UPDATE_TIME));
        relInsertor.put(FieldName.FIELD_CLREL_UPDATE_USER,
                fileInsertor.get(FieldName.FIELD_CLFILE_INNER_UPDATE_USER));
        relInsertor.put(FieldName.FIELD_CLREL_USER,
                fileInsertor.get(FieldName.FIELD_CLFILE_INNER_USER));
        relInsertor.put(FieldName.FIELD_CLREL_FILE_MIME_TYPE,
                fileInsertor.get(FieldName.FIELD_CLFILE_FILE_MIME_TYPE));
        relInsertor.put(FieldName.FIELD_CLREL_FILE_DELETE_MARKER, BsonUtils
                .getBooleanOrElse(fileInsertor, FieldName.FIELD_CLFILE_DELETE_MARKER, false));
        return relInsertor;
    }

    public static int parseFileSelector(ScmWorkspaceInfo ws, BSONObject fileSelector) {
        if (fileSelector == null) {
            // selector is null, we need a complete file record
            return QUERY_IN_FILE_TABLE;
        }
        return parseFileOderby(ws, fileSelector);
    }

    public static int parseFileOderby(ScmWorkspaceInfo ws, BSONObject fileOderby) {
        if(!ws.isEnableDirectory()) {
            return QUERY_IN_FILE_TABLE;
        }
        ParseContext parseContext = new ParseContext();
        parseFileMatcher(fileOderby, parseContext);
        if (parseContext.isAllFieldInRelTable()) {
            return QUERY_IN_RELATION_TABLE;
        }
        return QUERY_IN_FILE_TABLE;
    }

    public static int parseFileMatcher(ScmWorkspaceInfo ws, BSONObject fileMatcher) {
        if(!ws.isEnableDirectory()) {
            return QUERY_IN_FILE_TABLE;
        }
        ParseContext parseContext = new ParseContext();
        parseFileMatcher(fileMatcher, parseContext);
        return parseContext.getParseResult();
    }

    private static void parseFileMatcher(BSONObject fileMatcher, ParseContext context) {
        if (fileMatcher == null) {
            return;
        }
        if (fileMatcher instanceof BasicBSONList) {
            BasicBSONList list = (BasicBSONList) fileMatcher;
            for (Object ele : list) {
                if (ele instanceof BSONObject) {
                    parseFileMatcher((BSONObject) ele, context);
                    if (!context.isAllFieldInRelTable()) {
                        return;
                    }
                }
            }
        }
        else {
            for (String key : fileMatcher.keySet()) {
                if (!FILE_FIELD_MAP_REL_FIELD.containsKey(key) && !key.startsWith("$")) {
                    context.hasFiledNotInRelTable();
                    return;
                }

                Object value = fileMatcher.get(key);
                if (value instanceof BSONObject) {
                    parseFileMatcher((BSONObject) value, context);
                    if (!context.isAllFieldInRelTable()) {
                        return;
                    }
                }
                if (key.equals(FieldName.FIELD_CLFILE_DIRECTORY_ID)) {
                    context.hasDirectoryIdField();
                }
            }
        }
    }

    public static BSONObject getRelBSONFromFileBSON(BSONObject fileTableBSON) {
        if (fileTableBSON == null) {
            return null;
        }

        if (fileTableBSON instanceof BasicBSONList) {
            BasicBSONList copyFileTableBSONList = new BasicBSONList();
            for (Object ele : (Collection<?>) fileTableBSON) {
                if (ele instanceof BSONObject) {
                    copyFileTableBSONList.add(getRelBSONFromFileBSON((BSONObject) ele));
                }
                else {
                    copyFileTableBSONList.add(ele);
                }
            }
            return copyFileTableBSONList;
        }

        BasicBSONObject copyFileTableBSON = new BasicBSONObject();
        copyFileTableBSON.putAll(fileTableBSON);
        Set<String> keyset = copyFileTableBSON.keySet();
        Iterator<String> it = keyset.iterator();
        Map<String, Object> needReplaceMap = new HashMap<>();
        while (it.hasNext()) {
            String key = it.next();
            if (copyFileTableBSON.get(key) instanceof BSONObject) {
                BSONObject relBSON = getRelBSONFromFileBSON(
                        (BSONObject) copyFileTableBSON.get(key));
                if (FILE_FIELD_MAP_REL_FIELD.containsKey(key)) {
                    needReplaceMap.put(FILE_FIELD_MAP_REL_FIELD.get(key), relBSON);
                }
                else {
                    needReplaceMap.put(key, relBSON);
                }
                it.remove();
            }
            else if (FILE_FIELD_MAP_REL_FIELD.containsKey(key)) {
                needReplaceMap.put(FILE_FIELD_MAP_REL_FIELD.get(key), copyFileTableBSON.get(key));
                it.remove();
            }
        }
        copyFileTableBSON.putAll(needReplaceMap);
        return copyFileTableBSON;
    }

}

class ParseContext {
    private boolean hasDirectoryIdField = false;
    private boolean allFieldInRelTable = true;

    public void hasDirectoryIdField() {
        this.hasDirectoryIdField = true;
    }

    public void hasFiledNotInRelTable() {
        this.allFieldInRelTable = false;
    }

    public boolean isAllFieldInRelTable() {
        return allFieldInRelTable;
    }

    public int getParseResult() {
        if (hasDirectoryIdField && allFieldInRelTable) {
            return ScmMetaSourceHelper.QUERY_IN_RELATION_TABLE;
        }
        return ScmMetaSourceHelper.QUERY_IN_FILE_TABLE;
    }
}
