package com.sequoiacm.contentserver.tag;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.module.TagInfo;
import com.sequoiacm.common.module.TagName;
import com.sequoiacm.contentserver.common.IDGeneratorDao;
import com.sequoiacm.contentserver.config.TagConfig;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.ConcurrentLruMap;
import com.sequoiacm.infrastructure.common.ConcurrentLruMapFactory;
import com.sequoiacm.infrastructure.lock.ScmLock;
import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class TagLibMgr {
    private final TagConfig config;
    // 100 个工作区，每个 1000 个标签缓存，两个 map 占用 62MB 内存
    private final ConcurrentLruMap<String, ConcurrentLruMap<TagName, TagInfo>> tagLibCacheNameMapping;
    private final ConcurrentLruMap<String, ConcurrentLruMap<Long, TagInfo>> tagLibCacheIdMapping;

    @Autowired
    private TagLibDao tagLibDao;

    @Autowired
    private IDGeneratorDao idGeneratorDao;

    @Autowired
    public TagLibMgr(TagConfig config) {
        tagLibCacheNameMapping = ConcurrentLruMapFactory.create(config.getMaxTagLibCacheCount());
        tagLibCacheIdMapping = ConcurrentLruMapFactory.create(config.getMaxTagLibCacheCount());
        this.config = config;
    }

    public TagInfo getTagInfo(ScmWorkspaceInfo ws, TagName tagName) throws ScmServerException {
        List<TagInfo> ret = getTagInfo(ws, Collections.singletonList(tagName));
        if (ret.isEmpty()) {
            return null;
        }
        return ret.get(0);
    }

    public List<TagInfo> getTagInfo(ScmWorkspaceInfo ws, List<TagName> targetTagNames)
            throws ScmServerException {
        QueryTagResult<TagName> result = internalGetTagInfoInCache(ws, targetTagNames);
        if (result.notFoundTagList.isEmpty()) {
            return result.foundTagList;
        }
        List<TagInfo> ret = internalGetTagInfoInTableLock(ws, result.notFoundTagList);
        ret.addAll(result.foundTagList);
        return ret;
    }

    public TagInfoCursor queryTag(ScmWorkspaceInfo ws, BSONObject tagCondition)
            throws ScmServerException {
        return tagLibDao.query(ws, tagCondition);
    }

    public List<TagInfo> getTagInfoById(ScmWorkspaceInfo ws, List<Long> tagIdList)
            throws ScmServerException {
        if (tagIdList == null || tagIdList.size() == 0) {
            return new ArrayList<>();
        }


        QueryTagResult<Long> queryCacheResult = internalGetTagInfoInCacheById(ws, tagIdList);
        if (queryCacheResult.notFoundTagList.isEmpty()) {
            return queryCacheResult.foundTagList;
        }

        List<TagInfo> tagInfoInTable = null;
        ScmLock lock = ScmLockManager.getInstance()
                .acquiresReadLock(ScmLockPathFactory.createTagLibLockPath(ws.getTagLibTable()));
        try {
            tagInfoInTable = tagLibDao.queryById(ws, queryCacheResult.notFoundTagList);
            for (TagInfo tagInfo : tagInfoInTable) {
                refreshIdMappingCache(ws.getName(), tagInfo);
            }
        }
        finally {
            lock.unlock();
        }
        tagInfoInTable.addAll(queryCacheResult.foundTagList);
        return tagInfoInTable;
    }

    private void refreshIdMappingCache(String ws, TagInfo tagInfo) {
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance().getWorkspaceInfo(ws);
        if (wsInfo == null) {
            return;
        }
        ConcurrentLruMap<Long, TagInfo> tagLibCache = tagLibCacheIdMapping
                .get(wsInfo.getTagLibTable());
        if (tagLibCache == null) {
            synchronized (tagLibCacheIdMapping) {
                tagLibCache = tagLibCacheIdMapping.get(wsInfo.getTagLibTable());
                if (tagLibCache == null) {
                    tagLibCache = ConcurrentLruMapFactory.create(config.getTagLibCacheMaxSize());
                    tagLibCacheIdMapping.put(wsInfo.getTagLibTable(), tagLibCache);
                }
            }
        }
        tagLibCache.put(tagInfo.getTagId(), tagInfo);
    }

    private QueryTagResult<Long> internalGetTagInfoInCacheById(ScmWorkspaceInfo ws,
            List<Long> targetTagId) {
        QueryTagResult<Long> ret = new QueryTagResult<>();
        if (targetTagId == null || targetTagId.isEmpty()) {
            return ret;
        }

        ConcurrentLruMap<Long, TagInfo> tagLibCache = tagLibCacheIdMapping.get(ws.getTagLibTable());
        if (tagLibCache == null) {
            ret.notFoundTagList = targetTagId;
            return ret;
        }

        List<TagInfo> tagInCache = new ArrayList<>();
        List<Long> tagNotInCache = new ArrayList<>();
        for (Long tagId : targetTagId) {
            TagInfo tagInfo = tagLibCache.get(tagId);
            if (tagInfo == null) {
                tagNotInCache.add(tagId);
            }
            else {
                tagInCache.add(tagInfo);
            }
        }
        ret.notFoundTagList = tagNotInCache;
        ret.foundTagList = tagInCache;
        return ret;
    }

    // 返回值有两个，第一个是缓存中找到的，第二个是缓存中没找到的
    private QueryTagResult<TagName> internalGetTagInfoInCache(ScmWorkspaceInfo ws,
            List<TagName> targetTagNames) {
        QueryTagResult<TagName> ret = new QueryTagResult<>();
        if (targetTagNames == null || targetTagNames.isEmpty()) {
            return ret;
        }

        ConcurrentLruMap<TagName, TagInfo> tagLibCache = tagLibCacheNameMapping
                .get(ws.getTagLibTable());
        if (tagLibCache == null) {
            ret.notFoundTagList = targetTagNames;
            return ret;
        }

        List<TagInfo> tagInCache = new ArrayList<>();
        List<TagName> tagNotInCache = new ArrayList<>();
        for (TagName tagName : targetTagNames) {
            TagInfo tagInfo = tagLibCache.get(tagName);
            if (tagInfo == null) {
                tagNotInCache.add(tagName);
            }
            else {
                tagInCache.add(tagInfo);
            }
        }
        ret.notFoundTagList = tagNotInCache;
        ret.foundTagList = tagInCache;
        return ret;
    }

    // 必须在锁内使用这个函数
    private List<TagInfo> internalGetTagInfoInTableNoLock(ScmWorkspaceInfo ws, List<TagName> targetTagNames)
            throws ScmServerException {
        if (targetTagNames == null || targetTagNames.isEmpty()) {
            return new ArrayList<>();
        }
        List<TagInfo> queriesTagList = tagLibDao.queryByName(ws, targetTagNames);
        refreshNameMappingCache(ws.getName(), queriesTagList);
        return queriesTagList;
    }

    private List<TagInfo> internalGetTagInfoInTableLock(ScmWorkspaceInfo ws,
            List<TagName> targetTagNames) throws ScmServerException {
        // 这把锁保证查询/创建标签库，加入缓存的过程中，没有删除标签操作进来，导致缓存了脏数据
        ScmLock lock = ScmLockManager.getInstance()
                .acquiresReadLock(ScmLockPathFactory.createTagLibLockPath(ws.getTagLibTable()));
        try {
            return internalGetTagInfoInTableNoLock(ws, targetTagNames);
        }
        finally {
            lock.unlock();
        }
    }

    public TagInfo createTag(ScmWorkspaceInfo ws, TagName tagName) throws ScmServerException {
        List<TagInfo> ret = createTag(ws, Collections.singletonList(tagName));
        if (ret.size() != 1) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR,
                    "create tag failed: " + tagName + ", internal ret is " + ret);
        }
        return ret.get(0);
    }

    public List<TagInfo> createTag(ScmWorkspaceInfo ws, List<TagName> targetTagNames)
            throws ScmServerException {
        QueryTagResult<TagName> result = internalGetTagInfoInCache(ws, targetTagNames);
        if (result.notFoundTagList.isEmpty()) {
            return result.foundTagList;
        }
        List<TagInfo> ret = new ArrayList<>(result.foundTagList);

        ScmLock lock = ScmLockManager.getInstance()
                .acquiresReadLock(ScmLockPathFactory.createTagLibLockPath(ws.getTagLibTable()));
        try {

            List<TagInfo> queriesTagList = internalGetTagInfoInTableNoLock(ws, result.notFoundTagList);
            ret.addAll(queriesTagList);
            if (queriesTagList.size() == result.notFoundTagList.size()) {
                return ret;
            }

            for (TagInfo tagInfo : queriesTagList) {
                result.notFoundTagList.remove(tagInfo.getTagName());
            }

            List<TagInfo> createdTagSet = internalCreateTag(ws, result.notFoundTagList);
            ret.addAll(createdTagSet);
            refreshNameMappingCache(ws.getName(), createdTagSet);
        }
        finally {
            lock.unlock();
        }
        return ret;
    }

    private void refreshNameMappingCache(String wsName, List<TagInfo> tagInfoSet) {
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance().getWorkspaceInfo(wsName);
        if (wsInfo == null) {
            return;
        }

        ConcurrentLruMap<TagName, TagInfo> tagLibCache = tagLibCacheNameMapping
                .get(wsInfo.getTagLibTable());
        if (tagLibCache == null) {
            synchronized (tagLibCacheNameMapping) {
                tagLibCache = tagLibCacheNameMapping.get(wsInfo.getTagLibTable());
                if (tagLibCache == null) {
                    tagLibCache = ConcurrentLruMapFactory.create(config.getTagLibCacheMaxSize());
                    tagLibCacheNameMapping.put(wsInfo.getTagLibTable(), tagLibCache);
                }
            }
        }

        for (TagInfo tagInfo : tagInfoSet) {
            tagLibCache.put(tagInfo.getTagName(), tagInfo);
        }
    }

    private List<TagInfo> internalCreateTag(ScmWorkspaceInfo ws, List<TagName> notExistTag)
            throws ScmServerException {
        List<Long> idList = idGeneratorDao.getNewIds(
                CommonDefine.IdGenerator.ID_TYPE_TAG + "_" + ws.getTagLibTable(),
                notExistTag.size());
        List<TagInfo> inserters = new ArrayList<>();
        for (TagName tagName : notExistTag) {
            TagInfo tagInfo = new TagInfo();
            tagInfo.setTagId(idList.remove(0));
            tagInfo.setTagName(tagName);
            inserters.add(tagInfo);
        }
        return tagLibDao.insertOrQuery(ws, inserters);
    }

    public void invalidateTagCacheByWs(ScmWorkspaceInfo ws) throws ScmServerException {
        if (ws == null || ws.getTagLibTable() == null) {
            return;
        }

        ScmLock lock = ScmLockManager.getInstance()
                .acquiresReadLock(ScmLockPathFactory.createTagLibLockPath(ws.getTagLibTable()));
        try {
            tagLibCacheNameMapping.remove(ws.getTagLibTable());
            tagLibCacheIdMapping.remove(ws.getTagLibTable());
        }
        finally {
            lock.unlock();
        }
    }

    private static class QueryTagResult<NotFoundType> {
        private List<TagInfo> foundTagList = new ArrayList<>();
        private List<NotFoundType> notFoundTagList = new ArrayList<>();

        public QueryTagResult(List<TagInfo> foundTagList, List<NotFoundType> notFoundTagList) {
            this.foundTagList = foundTagList;
            this.notFoundTagList = notFoundTagList;
        }

        public QueryTagResult() {
        }
    }
}
