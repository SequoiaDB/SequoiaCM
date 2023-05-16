package com.sequoiacm.contentserver.pipeline.file.module;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.module.TagInfo;
import com.sequoiacm.common.module.TagName;
import com.sequoiacm.common.module.TagType;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.common.ScmArgumentChecker;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.contentserver.tag.TagLibMgr;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class FileMetaV2 extends FileMeta {
    private static final Logger logger = LoggerFactory.getLogger(FileMetaV2.class);
    private TagLibMgr tagLibMgr;
    private ScmWorkspaceInfo ws;
    private List<Long> tagsIdList = new ArrayList<>();
    private List<Long> customTagIdList = new ArrayList<>();

    public FileMetaV2(TagLibMgr tagLibMgr, BucketInfoManager bucketInfoManager,
            ScmWorkspaceInfo ws) {
        super(bucketInfoManager);
        this.tagLibMgr = tagLibMgr;
        this.ws = ws;
    }

    @Override
    public void loadInfoFromRecord(BSONObject record) {
        super.loadBasicInfoFromRecord(record);

        tagsIdList = BsonUtils.getLongArray(record, FieldName.FIELD_CLFILE_TAGS);
        if (tagsIdList == null) {
            tagsIdList = Collections.emptyList();
        }

        customTagIdList = BsonUtils.getLongArray(record, FieldName.FIELD_CLFILE_CUSTOM_TAG);
        if (customTagIdList == null) {
            customTagIdList = Collections.emptyList();
        }
    }

    @Override
    public void loadInfoFromUserInfo(String ws, BSONObject userFileInfo, String user,
            boolean checkProps) throws ScmServerException {
        super.loadBasicInfoFromUserInfo(ws, userFileInfo, user, checkProps);

        // 处理标签（明文 => 标签ID）
        List<TagName> tagNameList = new ArrayList<>();
        BSONObject tagsValue = BsonUtils.getBSON(userFileInfo, FieldName.FIELD_CLFILE_TAGS);
        Set<String> tags = ScmArgumentChecker.checkAndCorrectTagsAsSet(tagsValue);
        for (String tag : tags) {
            tagNameList.add(TagName.tags(tag));
        }

        BSONObject customTag = BsonUtils.getBSON(userFileInfo, FieldName.FIELD_CLFILE_CUSTOM_TAG);
        Map<String, String> customTagMap = ScmArgumentChecker.checkAndCorrectCustomTag(customTag);

        for (Map.Entry<String, String> entry : customTagMap.entrySet()) {
            tagNameList.add(TagName.customTag(entry.getKey(), entry.getValue()));
        }

        customTagIdList = new ArrayList<>();
        tagsIdList = new ArrayList<>();
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance().getWorkspaceInfoCheckExist(ws);
        List<TagInfo> tagInfoList = tagLibMgr.createTag(wsInfo, tagNameList);
        for (TagInfo tagInfo : tagInfoList) {
            if (tagInfo.getTagType() == TagType.CUSTOM_TAG) {
                customTagIdList.add(tagInfo.getTagId());
            }
            else if (tagInfo.getTagType() == TagType.TAGS) {
                tagsIdList.add(tagInfo.getTagId());
            }
            else {
                throw new ScmSystemException("tag type is not supported: " + tagInfo);
            }
        }
    }

    @Override
    public BSONObject toRecordBSON() {
        BSONObject bson = super.asRecordBSON();

        BasicBSONList idList = new BasicBSONList();
        idList.addAll(tagsIdList);
        bson.put(FieldName.FIELD_CLFILE_TAGS, idList);

        idList = new BasicBSONList();
        idList.addAll(customTagIdList);
        bson.put(FieldName.FIELD_CLFILE_CUSTOM_TAG, idList);
        return bson;
    }

    @Override
    public BSONObject toUserInfoBSON() throws ScmServerException {
        List<Long> allTagIdList = new ArrayList<>();
        allTagIdList.addAll(customTagIdList);
        allTagIdList.addAll(tagsIdList);

        List<TagInfo> tagInfoList = tagLibMgr.getTagInfoById(ws, allTagIdList);
        TreeMap<String, String> customTag = new TreeMap<>();
        List<String> tags = new ArrayList<>();
        for (TagInfo tagInfo : tagInfoList) {
            if (tagInfo.getTagType() == TagType.CUSTOM_TAG) {
                customTag.put(tagInfo.getTagName().getTagKey(), tagInfo.getTagName().getTagValue());
            }
            else if (tagInfo.getTagType() == TagType.TAGS) {
                tags.add(tagInfo.getTagName().getTag());
            }
            else {
                throw new ScmSystemException("unknown tag type: " + tagInfo + ", ws=" + ws.getName()
                        + " ,file=" + super.getSimpleDesc());
            }
        }

        if (allTagIdList.size() != tagInfoList.size()) {
            logger.warn(
                    "some tag not found: ws={}, file=[{}], customTagId={}, tagsId={}, getTagById={}",
                    ws.getName(), getSimpleDesc(), customTagIdList, tagsIdList, tagInfoList);
        }

        BSONObject bson = super.asUserInfoBson();
        BasicBSONList tagsBson = new BasicBSONList();
        tagsBson.addAll(tags);
        bson.put(FieldName.FIELD_CLFILE_TAGS, tagsBson);

        BasicBSONObject customTagBson = new BasicBSONObject(true);
        customTagBson.putAll(customTag);
        bson.put(FieldName.FIELD_CLFILE_CUSTOM_TAG, customTagBson);
        return bson;
    }

    public List<Long> getTagsIdList() {
        return tagsIdList;
    }

    public void setTagsIdList(List<Long> tagsIdList) {
        this.tagsIdList = tagsIdList;
    }

    public List<Long> getCustomTagIdList() {
        return customTagIdList;
    }

    public void setCustomTagIdList(List<Long> customTagIdList) {
        this.customTagIdList = customTagIdList;
    }

    @Override
    public FileMeta clone() {
        FileMetaV2 clone = (FileMetaV2) super.clone();
        clone.tagLibMgr = this.tagLibMgr;
        clone.ws = this.ws;
        clone.tagsIdList = new ArrayList<>(this.tagsIdList);
        clone.customTagIdList = new ArrayList<>(this.customTagIdList);
        return clone;
    }
}
