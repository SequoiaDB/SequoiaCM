package com.sequoiacm.contentserver.metasourcemgr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.bson.util.JSON;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.exception.ScmServerException;
import com.sequoiacm.contentserver.site.ScmContentServerMapping;
import com.sequoiacm.contentserver.site.ScmSite;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.common.ScmIdParser;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.MetaSource;
import com.sequoiacm.metasource.ScmMetasourceException;

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
    }

    public static void closeCursor(MetaCursor cursor) {
        if (null != cursor) {
            cursor.close();
        }
    }

    public static List<ScmSite> getSiteList(MetaSource metasource) throws ScmServerException {
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

    public static List<ScmContentServerMapping> getServerList(MetaSource metasource)
            throws ScmServerException {
        MetaCursor cursor = null;
        try {
            MetaAccessor serverAccesor = metasource.getServerAccessor();
            cursor = serverAccesor.query(null, null, null);

            List<ScmContentServerMapping> serverList = new ArrayList<>();
            while (cursor.hasNext()) {
                BSONObject bo = cursor.getNext();
                ScmContentServerMapping contentServer = new ScmContentServerMapping(bo);
                serverList.add(contentServer);
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

    public static List<BSONObject> getWorkspaceList(MetaSource metasource)
            throws ScmServerException {

        MetaCursor cursor = null;
        try {
            MetaAccessor workspaceAccesor = metasource.getWorkspaceAccessor();
            cursor = workspaceAccesor.query(null, null, new BasicBSONObject(
                    FieldName.FIELD_CLWORKSPACE_ID, 1));

            List<BSONObject> workspaceList = new ArrayList<>();
            while (cursor.hasNext()) {
                BSONObject bo = cursor.getNext();
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
        String listSiteKey = FieldName.FIELD_CLFILE_FILE_SITE_LIST + "."
                + SEQUOIADB_MATCHER_DOLLAR0 + "." + FieldName.FIELD_CLFILE_FILE_SITE_LIST_ID;

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
            throw new ScmServerException(ScmError.ATTRIBUTE_FORMAT_ERROR, "field[" + fieldName
                    + "] is not exist!");
        }

        if (!(value instanceof String)) {
            throw new ScmServerException(ScmError.ATTRIBUTE_FORMAT_ERROR, "field[" + fieldName
                    + "] is not String format!");
        }

        String valueStr = (String) value;
        if (valueStr.length() == 0) {
            throw new ScmServerException(ScmError.ATTRIBUTE_FORMAT_ERROR, "field[" + fieldName
                    + "]'s length can't be 0");
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
        return relInsertor;
    }

    public static int parseFileSelector(BSONObject fileSelector) {
        if (fileSelector == null) {
            //selector is null, we need a complete file record
            return QUERY_IN_FILE_TABLE;
        }
        return parseFileOderby(fileSelector);
    }

    public static int parseFileOderby(BSONObject fileOderby) {
        ParseContext parseContext = new ParseContext();
        parseFileMatcher(fileOderby, parseContext);
        if (parseContext.isAllFieldInRelTable()) {
            return QUERY_IN_RELATION_TABLE;
        }
        return QUERY_IN_FILE_TABLE;
    }

    public static int parseFileMatcher(BSONObject fileMatcher) {
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
                BSONObject relBSON = getRelBSONFromFileBSON((BSONObject) copyFileTableBSON.get(key));
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

    public static void main(String[] args) {
        BSONObject b = (BSONObject) JSON.parse("{author:'sadas'}");
        System.out.println(parseFileOderby(b));

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
