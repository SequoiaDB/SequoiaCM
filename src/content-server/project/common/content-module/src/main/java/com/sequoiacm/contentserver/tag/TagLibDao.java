package com.sequoiacm.contentserver.tag;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.module.TagInfo;
import com.sequoiacm.common.module.TagName;
import com.sequoiacm.common.module.TagType;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TagLibDao {

    public MetaCursor query(ScmWorkspaceInfo ws, BSONObject matcher, long skip, long limit,
            BSONObject orderby) throws ScmServerException {
        try {
            MetaAccessor tagLibMetaAccessor = ScmContentModule.getInstance().getMetaService()
                    .getMetaSource().createMetaAccessor(ws.getTagLibTable());
            return tagLibMetaAccessor.query(matcher, null, orderby, skip, limit);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to query tag: tagLib=" + ws.getTagLibTable() + ", matcher=" + matcher,
                    e);
        }
    }

    public TagInfoCursor query(ScmWorkspaceInfo ws, BSONObject matcher) throws ScmServerException {
        MetaCursor cursor = null;
        try {
            MetaAccessor tagLibMetaAccessor = ScmContentModule.getInstance().getMetaService()
                    .getMetaSource().createMetaAccessor(ws.getTagLibTable());
            cursor = tagLibMetaAccessor.query(matcher, null, null);
            return new TagInfoCursorMetaSourceImpl(cursor);
        }
        catch (Exception e) {
            if (cursor != null) {
                cursor.close();
            }
            if (e instanceof ScmMetasourceException) {
                throw new ScmServerException(((ScmMetasourceException) e).getScmError(),
                        "failed to query tag: tagLib=" + ws.getTagLibTable() + ", matcher="
                                + matcher,
                        e);
            }
            throw new ScmSystemException(
                    "failed to query tag: tagLib=" + ws.getTagLibTable() + ", matcher=" + matcher,
                    e);
        }
    }

    public List<TagInfo> queryById(ScmWorkspaceInfo ws, List<Long> tagIdList)
            throws ScmServerException {
        return queryTagInfo(ws, new BasicBSONObject(FieldName.TagLib.TAG_ID,
                new BasicBSONObject("$in", tagIdList)));
    }

    public List<TagInfo> queryByName(ScmWorkspaceInfo ws, List<TagName> tagNameList)
            throws ScmServerException {
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
                throw new ScmInvalidArgumentException("unknown tag type: " + tagName.getTagType());
            }
        }
        if (!tagsCondition.isEmpty()) {
            ret.addAll(queryTagInfo(ws, new BasicBSONObject(FieldName.TagLib.TAG,
                    new BasicBSONObject("$in", tagsCondition))));
        }

        if (!customTagCondition.isEmpty()) {
            ret.addAll(queryTagInfo(ws, new BasicBSONObject(FieldName.TagLib.CUSTOM_TAG,
                    new BasicBSONObject("$in", customTagCondition))));
        }
        return ret;
    }

    private List<TagInfo> queryTagInfo(ScmWorkspaceInfo ws, BSONObject condition)
            throws ScmServerException {
        List<TagInfo> ret = new ArrayList<>();
        TagInfoCursor cursor = query(ws, condition);
        try {
            while (cursor.hasNext()) {
                ret.add(cursor.getNext());
            }
        }
        finally {
            cursor.close();
        }

        return ret;

    }

    public List<TagInfo> insertOrQuery(ScmWorkspaceInfo ws, List<TagInfo> inserters)
            throws ScmServerException {
        List<BSONObject> inserterBSON = new ArrayList<>();
        for (TagInfo inserter : inserters) {
            inserterBSON.add(inserter.toBson());
        }
        try {
            MetaAccessor tagLibMetaAccessor = ScmContentModule.getInstance().getMetaService()
                    .getMetaSource().createMetaAccessor(ws.getTagLibTable());
            boolean allInserted = tagLibMetaAccessor.bulkInsert(inserterBSON);
            if (allInserted) {
                return inserters;
            }

            List<TagName> tagName = new ArrayList<>();
            for (TagInfo inserter : inserters) {
                tagName.add(inserter.getTagName());
            }

            List<TagInfo> tagInfo = queryByName(ws, tagName);
            if (tagInfo.size() == inserters.size()) {
                return tagInfo;
            }

            // inserters - tagInfo = notFoundTag
            List<TagName> notFoundTag = TagUtil.tagNameComplementarySet(inserters, tagInfo);
            // 插入时报标签已经存在，但是查询时又不存在，对外报标签暂时不可用
            throw new ScmServerException(ScmError.RESOURCE_CONFLICT, "tag is busy: " + notFoundTag);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to insert or query tag: ws=" + ws.getName(), e);
        }
    }

    public MetaCursor queryCustomTagKey(ScmWorkspaceInfo wsInfo, BSONObject condition,
            boolean ascending, long skip, long limit) throws ScmServerException {
        try {
            MetaAccessor tagLibMetaAccessor = ScmContentModule.getInstance().getMetaService()
                    .getMetaSource().createMetaAccessor(wsInfo.getTagLibTable());
            List<BSONObject> aggregateOps = new ArrayList<>();

            BasicBSONList andArr = new BasicBSONList();
            andArr.add(condition);
            andArr.add(new BasicBSONObject(FieldName.TagLib.TAG_TYPE,
                    TagType.CUSTOM_TAG.getFileField()));
            aggregateOps.add(new BasicBSONObject("$match", new BasicBSONObject("$and", andArr)));

            BSONObject groupObj = new BasicBSONObject();
            groupObj.put("_id",
                    "$" + FieldName.TagLib.CUSTOM_TAG + "." + FieldName.TagLib.CUSTOM_TAG_TAG_KEY);
            groupObj.put(FieldName.TagLib.CUSTOM_TAG_TAG_KEY,
                    "$" + FieldName.TagLib.CUSTOM_TAG + "." + FieldName.TagLib.CUSTOM_TAG_TAG_KEY);
            aggregateOps.add(new BasicBSONObject("$group", groupObj));

            aggregateOps.add(new BasicBSONObject("$sort",
                    new BasicBSONObject(FieldName.TagLib.CUSTOM_TAG_TAG_KEY, ascending ? 1 : -1)));

            if (skip > 0) {
                aggregateOps.add(new BasicBSONObject("$skip", skip));
            }

            if (limit >= 0) {
                aggregateOps.add(new BasicBSONObject("$limit", limit));
            }

            return tagLibMetaAccessor.aggregate(aggregateOps);
        }
        catch (Exception e) {
            if (e instanceof ScmMetasourceException) {
                throw new ScmServerException(((ScmMetasourceException) e).getScmError(),
                        "failed to query custom tag key: tagLib=" + wsInfo.getTagLibTable()
                                + ", matcher=" + condition,
                        e);
            }
            throw new ScmSystemException("failed to query custom tag key: tagLib="
                    + wsInfo.getTagLibTable() + ", matcher=" + condition, e);
        }

    }
}
