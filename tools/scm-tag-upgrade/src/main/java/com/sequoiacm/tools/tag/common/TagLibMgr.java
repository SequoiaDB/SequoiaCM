package com.sequoiacm.tools.tag.common;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.module.TagInfo;
import com.sequoiacm.common.module.TagName;
import com.sequoiacm.common.module.TagType;
import com.sequoiacm.infrastructure.common.ConcurrentLruMap;
import com.sequoiacm.infrastructure.common.ConcurrentLruMapFactory;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceUpdator;
import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TagLibMgr {
    private static final TagLibMgr instance = new TagLibMgr();

    private final ConcurrentLruMap<TagNameKey, TagInfo> tagInfoCache;

    private TagLibMgr() {
        tagInfoCache = ConcurrentLruMapFactory
                .create(UpgradeConfig.getInstance().getIntConf("tagLibCacheSize", 50000));
    }

    public static TagLibMgr getInstance() {
        return instance;
    }

    public void createTagLibAndMarkUpgrading(String ws, String tagLibDomain)
            throws ScmToolsException {
        TagLibDao.getInstance().createTagLibAndMarkUpgrading(ws, tagLibDomain);
    }

    public List<TagInfo> createTag(WsBasicInfo wsBasicInfo, List<TagName> tagNames)
            throws ScmToolsException {
        List<TagName> notExistTag = new ArrayList<>();
        List<TagInfo> ret = new ArrayList<>();
        for (TagName tagName : tagNames) {
            TagInfo tagInfo = tagInfoCache
                    .get(new TagNameKey(tagName, wsBasicInfo.getTagLibTableFullName()));
            if (tagInfo != null) {
                ret.add(tagInfo);
            }
            else {
                notExistTag.add(tagName);
            }
        }
        if (notExistTag.isEmpty()) {
            return ret;
        }

        List<TagInfo> tagInfoList = TagLibDao.getInstance().queryTagInfo(wsBasicInfo, notExistTag);
        refreshCache(wsBasicInfo, tagInfoList);

        ret.addAll(tagInfoList);
        if (ret.size() == tagNames.size()) {
            return ret;
        }

        for (TagInfo tagInfo : tagInfoList) {
            notExistTag.remove(tagInfo.getTagName());
        }

        List<TagInfo> createdTagList = createTagInternal(wsBasicInfo, notExistTag);
        refreshCache(wsBasicInfo, createdTagList);

        ret.addAll(createdTagList);
        return ret;
    }

    private List<TagInfo> createTagInternal(WsBasicInfo ws, List<TagName> notExistTag)
            throws ScmToolsException {
        List<Long> idList = TagIdGenerator.getInstance().genTagId(ws.getTagLibTableFullName(),
                notExistTag.size());
        List<TagInfo> inserters = new ArrayList<>();
        for (TagName tagName : notExistTag) {
            TagInfo tagInfo = new TagInfo();
            tagInfo.setTagId(idList.remove(0));
            tagInfo.setTagName(tagName);
            inserters.add(tagInfo);
        }

        return TagLibDao.getInstance().insertIgnoreExist(ws, inserters);
    }

    private void refreshCache(WsBasicInfo ws, List<TagInfo> tagInfoList) {
        for (TagInfo tagInfo : tagInfoList) {
            tagInfoCache.put(new TagNameKey(tagInfo.getTagName(), ws.getTagLibTableFullName()),
                    tagInfo);
        }
    }

    public void markWorkspaceUpgradeComplete(String ws) throws ScmToolsException {
        TagLibDao.getInstance().markWorkspaceUpgradeComplete(ws);
    }
}

class TagNameKey {
    private final TagName tagName;
    private final String tagLibTable;

    public TagNameKey(TagName tagName, String tagLibTable) {
        this.tagName = tagName;
        this.tagLibTable = tagLibTable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TagNameKey that = (TagNameKey) o;
        return Objects.equals(tagName, that.tagName)
                && Objects.equals(tagLibTable, that.tagLibTable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tagName, tagLibTable);
    }
}

class TagLibDao {
    private static final Logger logger = LoggerFactory.getLogger(TagLibDao.class);
    private static final TagLibDao instance = new TagLibDao();

    private static final int SDB_IXM_CREATING = -387; // DB error code
    private static final int SDB_IXM_COVER_CREATING = -389; // DB error code

    public static TagLibDao getInstance() {
        return instance;
    }

    private TagLibDao() {
    }

    public void createTagLibAndMarkUpgrading(String wsName, String tagLibDomain)
            throws ScmToolsException {

        Sequoiadb sdb = null;
        try {
            sdb = SequoiadbDataSourceWrapper.getInstance().getConnection();
            if (!sdb.isDomainExist(tagLibDomain)) {
                throw new ScmToolsException("tagLibDomain not exist: " + tagLibDomain,
                        ScmBaseExitCode.SYSTEM_ERROR);
            }

            BasicBSONObject wsMatcher = new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_NAME,
                    wsName);
            DBCollection wsCl = sdb.getCollectionSpace("SCMSYSTEM").getCollection("WORKSPACE");
            BSONObject wsRecord = wsCl.queryOne(wsMatcher, null, null, null, 0);
            if (wsRecord == null) {
                throw new ScmToolsException(
                        "failed to create tagLib for ws, workspace not found: " + wsName,
                        ScmBaseExitCode.INVALID_ARG);
            }

            String tagLibTable = BsonUtils.getString(wsRecord,
                    FieldName.FIELD_CLWORKSPACE_TAG_LIB_TABLE);
            if (tagLibTable != null && !tagLibTable.isEmpty()) {
                logger.info("workspace already has tagLib: " + wsName);
                WorkspaceUpdator wsUpdater = new WorkspaceUpdator(wsName);
                wsUpdater.setTagUpgrading(true);
                updateWorkspace(sdb, wsUpdater);
                return;
            }

            BasicBSONObject tagLibCsOption = new BasicBSONObject("Domain", tagLibDomain);
            String tagLibCsName = CommonDefine.TagLib.TAG_LIB_CS_PREFIX + tagLibDomain;
            String tagLibClName = wsName + CommonDefine.TagLib.TAG_LIB_CL_TAIL;
            CollectionSpace tagLibCs;
            try {
                tagLibCs = sdb.createCollectionSpace(tagLibCsName, tagLibCsOption);
            }
            catch (BaseException e) {
                if (e.getErrorCode() != SDBError.SDB_DMS_CS_EXIST.getErrorCode()) {
                    throw e;
                }
                tagLibCs = sdb.getCollectionSpace(tagLibCsName);
            }
            DBCollection tagLibCl;
            try {
                tagLibCl = tagLibCs.createCollection(tagLibClName);
            }
            catch (BaseException e) {
                if (e.getErrorCode() != SDBError.SDB_DMS_EXIST.getErrorCode()) {
                    throw e;
                }
                tagLibCl = tagLibCs.getCollection(tagLibClName);
            }
            BasicBSONObject tagUniqueIdx = new BasicBSONObject(FieldName.TagLib.TAG, 1);
            ensureUniqueIndex(tagLibCl, "tag_unique_idx", tagUniqueIdx);
            BasicBSONObject customTagUniqueIdx = new BasicBSONObject(FieldName.TagLib.CUSTOM_TAG,
                    1);
            ensureUniqueIndex(tagLibCl, "custom_tag_unique_idx", customTagUniqueIdx);

            WorkspaceUpdator wsUpdater = new WorkspaceUpdator(wsName);
            wsUpdater.setTagUpgrading(true);
            wsUpdater.setTagLibTable(tagLibCl.getFullName());
            updateWorkspace(sdb, wsUpdater);
        }
        finally {
            SequoiadbDataSourceWrapper.getInstance().releaseConnection(sdb);
        }
    }

    private void updateWorkspace(Sequoiadb sdb, WorkspaceUpdator wsUpdater)
            throws ScmToolsException {
        List<String> configServerUrls = getConfigServerUrl(sdb);
        if (configServerUrls.isEmpty()) {
            throw new IllegalStateException("config server instance not found");
        }
        for (String configServerUrl : configServerUrls) {
            try {
                RestTools.updateWorkspace(configServerUrl, wsUpdater);
                logger.info("create tagLib for ws success: " + wsUpdater.getWsName());
                return;
            }
            catch (Exception e) {
                logger.warn("failed to update workspace: configServerInstance={}, ws={}",
                        configServerUrl, wsUpdater.getWsName(), e);
            }
        }
        throw new ScmToolsException("request config server to update workspace failed",
                ScmBaseExitCode.SYSTEM_ERROR);
    }

    private List<String> getConfigServerUrl(Sequoiadb sdb) {
        DBCollection eurekaInstance = sdb.getCollectionSpace("SCMSYSTEM")
                .getCollection("EUREKA_INSTANCE");
        BasicBSONObject matcher = new BasicBSONObject();
        matcher.put("service_name", "config-server");
        matcher.put("is_manual_stopped", false);

        ArrayList<String> urls = new ArrayList<>();
        DBCursor cursor = eurekaInstance.query(matcher, null, null, null);
        try {
            while (cursor.hasNext()) {
                BSONObject record = cursor.getNext();
                urls.add(BsonUtils.getStringChecked(record, "host_name") + ":"
                        + BsonUtils.getNumberChecked(record, "port"));
            }
        }
        finally {
            cursor.close();
        }
        return urls;
    }

    private void ensureUniqueIndex(DBCollection cl, String idxName, BSONObject key)
            throws ScmToolsException {
        try {
            cl.createIndex(idxName, key, true, false);
        }
        catch (BaseException e) {
            if (e.getErrorCode() != SDBError.SDB_IXM_EXIST.getErrorCode()
                    && e.getErrorCode() != SDBError.SDB_IXM_REDEF.getErrorCode()
                    && e.getErrorCode() != SDBError.SDB_IXM_EXIST_COVERD_ONE.getErrorCode()
                    && e.getErrorCode() != SDB_IXM_CREATING
                    && e.getErrorCode() != SDB_IXM_COVER_CREATING) {
                throw new ScmToolsException(
                        "failed to create cl index:" + cl.getFullName() + ", idxName=" + idxName
                                + ", idxDef=" + key + ", isUnique=" + true,
                        ScmBaseExitCode.SYSTEM_ERROR, e);
            }
        }
    }

    public List<TagInfo> queryTagInfo(WsBasicInfo ws, List<TagName> tagNameList)
            throws ScmToolsException {
        Sequoiadb sdb = null;

        try {
            sdb = SequoiadbDataSourceWrapper.getInstance().getConnection();
            DBCollection tagLibCl = sdb.getCollectionSpace(ws.getTagLibTableCsName())
                    .getCollection(ws.getTagLibTableClName());

            List<TagInfo> ret = new ArrayList<>();
            BasicBSONList tagsCondition = new BasicBSONList();
            BasicBSONList customTagCondition = new BasicBSONList();
            for (TagName tagName : tagNameList) {
                if (tagName.getTagType() == TagType.TAGS) {
                    tagsCondition.add(tagName.getTag());
                }
                else if (tagName.getTagType() == TagType.CUSTOM_TAG) {
                    customTagCondition.add(new BasicBSONObject(FieldName.TagLib.CUSTOM_TAG_TAG_KEY,
                            tagName.getTagKey()).append(FieldName.TagLib.CUSTOM_TAG_TAG_VALUE,
                                    tagName.getTagValue()));
                }
                else {
                    throw new IllegalArgumentException("unknown tag type: " + tagName.getTagType());
                }
            }
            if (!tagsCondition.isEmpty()) {
                ret.addAll(queryTagInfo(tagLibCl, new BasicBSONObject(FieldName.TagLib.TAG,
                        new BasicBSONObject("$in", tagsCondition))));
            }

            if (!customTagCondition.isEmpty()) {
                ret.addAll(queryTagInfo(tagLibCl, new BasicBSONObject(FieldName.TagLib.CUSTOM_TAG,
                        new BasicBSONObject("$in", customTagCondition))));
            }
            return ret;
        }
        finally {
            SequoiadbDataSourceWrapper.getInstance().releaseConnection(sdb);
        }
    }

    private List<TagInfo> queryTagInfo(DBCollection tagLibCl, BSONObject condition) {
        List<TagInfo> ret = new ArrayList<>();
        DBCursor cursor = tagLibCl.query(condition, null, null, null);
        try {
            while (cursor.hasNext()) {
                ret.add(new TagInfo(cursor.getNext()));
            }
        }
        finally {
            cursor.close();
        }

        return ret;

    }

    public List<TagInfo> insertIgnoreExist(WsBasicInfo ws, List<TagInfo> inserters)
            throws ScmToolsException {
        List<BSONObject> inserterBSON = new ArrayList<>();
        for (TagInfo inserter : inserters) {
            inserterBSON.add(inserter.toBson());
        }
        Sequoiadb sdb = null;
        try {
            sdb = SequoiadbDataSourceWrapper.getInstance().getConnection();

            DBCollection tagLibCl = sdb.getCollectionSpace(ws.getTagLibTableCsName())
                    .getCollection(ws.getTagLibTableClName());
            boolean allInserted = bulkInsert(tagLibCl, inserterBSON);
            if (allInserted) {
                return inserters;
            }

            List<TagName> tagName = new ArrayList<>();
            for (TagInfo inserter : inserters) {
                tagName.add(inserter.getTagName());
            }

            List<TagInfo> tagInfo = queryTagInfo(ws, tagName);
            if (tagInfo.size() == inserters.size()) {
                return tagInfo;
            }

            throw new ScmToolsException("failed to insert tagInfo: " + inserters
                    + ", query result when conflict: " + tagInfo, ScmBaseExitCode.SYSTEM_ERROR);
        }
        finally {
            SequoiadbDataSourceWrapper.getInstance().releaseConnection(sdb);
        }
    }

    // 返回false表示存在冲突记录
    private boolean bulkInsert(DBCollection cl, List<BSONObject> records) throws ScmToolsException {
        BSONObject ret = cl.insertRecords(records, DBCollection.FLG_INSERT_CONTONDUP);
        if (ret == null) {
            throw new ScmToolsException("failed to bulk insert, ret is null: cl=" + cl.getFullName()
                    + ", inserters=" + records, ScmBaseExitCode.SYSTEM_ERROR);
        }
        return BsonUtils.getNumber(ret, "DuplicatedNum").longValue() <= 0;
    }

    public void markWorkspaceUpgradeComplete(String ws) throws ScmToolsException {
        Sequoiadb sdb = SequoiadbDataSourceWrapper.getInstance().getConnection();
        try {
            WorkspaceUpdator wsUpdater = new WorkspaceUpdator(ws);
            wsUpdater.setTagUpgrading(false);

            List<String> configServerUrls = getConfigServerUrl(sdb);
            if (configServerUrls.isEmpty()) {
                throw new IllegalStateException("config server instance not found");
            }
            for (String configServerUrl : configServerUrls) {
                try {
                    RestTools.updateWorkspace(configServerUrl, wsUpdater);
                    logger.info("mark workspace upgrade complete: " + ws);
                    return;
                }
                catch (Exception e) {
                    logger.warn(
                            "failed to mark workspace upgrade complete: configServerInstance={}, ws={}",
                            configServerUrl, ws, e);
                }
            }
            throw new ScmToolsException("request config server to update workspace failed",
                    ScmBaseExitCode.SYSTEM_ERROR);
        }
        finally {
            SequoiadbDataSourceWrapper.getInstance().releaseConnection(sdb);
        }
    }
}

class TagIdGenerator {

    private static final TagIdGenerator instance = new TagIdGenerator();

    public static TagIdGenerator getInstance() {
        return instance;
    }

    public List<Long> genTagId(String tagLib, int count) throws ScmToolsException {
        long latestId = queryAndUpdateId(tagLib, count);
        List<Long> ret = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ret.add(latestId - i);
        }

        return ret;
    }

    private long queryAndUpdateId(String tagLib, int inc) throws ScmToolsException {
        Sequoiadb sdb = null;

        try {
            sdb = SequoiadbDataSourceWrapper.getInstance().getConnection();
            DBCollection idCl = sdb
                    .getCollectionSpace(CommonDefine.IdGenerator.ID_GENERATOR_TABLE_CS)
                    .getCollection(CommonDefine.IdGenerator.ID_GENERATOR_TABLE_CL);

            BSONObject matcher = new BasicBSONObject();
            matcher.put(CommonDefine.IdGenerator.ID_TYPE,
                    CommonDefine.IdGenerator.ID_TYPE_TAG + "_" + tagLib);

            BSONObject update = new BasicBSONObject();
            update.put(CommonDefine.IdGenerator.ID_ID, inc);
            BSONObject updateId = new BasicBSONObject();
            updateId.put("$inc", update);

            BSONObject hint = new BasicBSONObject();
            hint.put("", "");

            BSONObject record = queryAndUpdate(idCl, matcher, updateId);

            if (record == null) {
                if (initId(idCl, tagLib, inc)) {
                    return inc;
                }
                record = queryAndUpdate(idCl, matcher, updateId);
            }
            return BsonUtils.getNumberChecked(record, CommonDefine.IdGenerator.ID_ID).longValue();
        }
        finally {
            SequoiadbDataSourceWrapper.getInstance().releaseConnection(sdb);
        }
    }

    private boolean initId(DBCollection idCl, String tagLib, long id) {
        BSONObject insert = new BasicBSONObject();
        insert.put(CommonDefine.IdGenerator.ID_TYPE,
                CommonDefine.IdGenerator.ID_TYPE_TAG + "_" + tagLib);
        insert.put(CommonDefine.IdGenerator.ID_ID, id);
        try {
            idCl.insert(insert);
            return true;
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_IXM_DUP_KEY.getErrorCode()) {
                return false;
            }
            throw e;
        }
    }

    private BSONObject queryAndUpdate(DBCollection cl, BSONObject matcher, BSONObject updater) {
        DBCursor cursor = cl.queryAndUpdate(matcher, null, null, null, updater, 0, 1, 0, true);
        try {
            if (cursor.hasNext()) {
                return cursor.getNext();
            }
            return null;
        }
        finally {
            cursor.close();
        }
    }
}
