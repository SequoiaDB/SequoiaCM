package com.sequoiacm.contentserver.controller;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.module.TagType;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmOperationUnsupportedException;
import com.sequoiacm.contentserver.service.ITagService;
import com.sequoiacm.contentserver.service.impl.ServiceUtils;
import com.sequoiacm.contentserver.tag.syntaxtree.TagSyntaxTree;
import com.sequoiacm.contentserver.tag.syntaxtree.TagSyntaxTreeBuilder;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructure.common.ScmStringUtil;
import com.sequoiacm.metasource.MetaCursor;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1")
public class TagController {
    @Autowired
    private TagSyntaxTreeBuilder tagSyntaxTreeBuilder;

    @Autowired
    private ITagService tagService;

    private static final Set<String> tagSearchFileConditionInvalidKeys = new HashSet<>();
    static {
        tagSearchFileConditionInvalidKeys.add(FieldName.FIELD_CLFILE_CUSTOM_TAG);
        tagSearchFileConditionInvalidKeys.add(FieldName.FIELD_CLFILE_TAGS);
    }

    @GetMapping(path = "/tag", params = { "action=search_file" })
    public void searchFile(@RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(CommonDefine.RestArg.TAG_CONDITION) BSONObject tagCondition,
            @RequestParam(value = CommonDefine.RestArg.FILE_CONDITION, required = false) BSONObject fileCondition,
            @RequestParam(value = CommonDefine.RestArg.ORDER_BY, required = false) BSONObject orderBy,
            @RequestParam(value = CommonDefine.RestArg.SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = CommonDefine.RestArg.LIMIT, required = false, defaultValue = "-1") long limit,
            @RequestParam(value = CommonDefine.RestArg.FILE_LIST_SCOPE, defaultValue = CommonDefine.Scope.SCOPE_CURRENT
                    + "") int scope,
            HttpServletResponse response, Authentication auth) throws ScmServerException {
        // 拒绝通过标签排序
        if (orderBy != null) {
            if (orderBy.containsField(FieldName.FIELD_CLFILE_TAGS)
                    || orderBy.containsField(FieldName.FIELD_CLFILE_CUSTOM_TAG)) {
                throw new ScmOperationUnsupportedException("can not order by tag: " + orderBy);
            }
        }
        // 普通属性不允许携带标签
        checkConditionBsonKey(fileCondition, tagSearchFileConditionInvalidKeys);

        ScmUser user = (ScmUser) auth.getPrincipal();
        TagSyntaxTree tagSyntaxTree = tagSyntaxTreeBuilder.buildSyntaxTree(tagCondition);
        boolean isResContainsDeleteMarker = ScmSystemUtils.isDeleteMarkerRequired(scope);
        response.setHeader("Content-Type", "application/json;charset=utf-8");
        MetaCursor cursor = tagService.searchFile(workspaceName, user, tagSyntaxTree, fileCondition,
                orderBy, scope, skip, limit, isResContainsDeleteMarker);

        ServiceUtils.putCursorToResponse(cursor, response);
    }

    @GetMapping(path = "/tag", params = { "action=count_file" })
    public ResponseEntity<String> countFile(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(CommonDefine.RestArg.TAG_CONDITION) BSONObject tagCondition,
            @RequestParam(value = CommonDefine.RestArg.FILE_CONDITION, required = false) BSONObject fileCondition,
            @RequestParam(value = CommonDefine.RestArg.FILE_LIST_SCOPE, defaultValue = CommonDefine.Scope.SCOPE_CURRENT
                    + "") int scope,
            HttpServletResponse response, Authentication auth) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        TagSyntaxTree tagSyntaxTree = tagSyntaxTreeBuilder.buildSyntaxTree(tagCondition);
        boolean isResContainsDeleteMarker = ScmSystemUtils.isDeleteMarkerRequired(scope);
        long count = tagService.countFile(workspaceName, user, tagSyntaxTree, fileCondition, scope,
                isResContainsDeleteMarker);
        response.setHeader(CommonDefine.RestArg.X_SCM_COUNT, String.valueOf(count));
        return ResponseEntity.ok("");
    }

    @GetMapping(value = "/tag", params = { "action=get_custom_tag" })
    public void getCustomTag(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(value = CommonDefine.RestArg.TAG_KEY, required = false) String tagKey,
            @RequestParam(value = CommonDefine.RestArg.TAG_VALUE, required = false) String tagValue,
            @RequestParam(value = CommonDefine.RestArg.TAG_KEY_WILDCARD, required = false) String tagKeyWildcard,
            @RequestParam(value = CommonDefine.RestArg.TAG_VALUE_WILDCARD, required = false) String tagValueWildcard,
            @RequestParam(value = CommonDefine.RestArg.ORDER_BY, required = false) BSONObject orderBy,
            @RequestParam(value = CommonDefine.RestArg.SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = CommonDefine.RestArg.LIMIT, required = false, defaultValue = "-1") long limit,
            HttpServletResponse response, Authentication auth) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        if (tagKey != null && tagKeyWildcard != null) {
            throw new ScmInvalidArgumentException(
                    "tagKey and tagKeyWildcard cannot be set at the same time");
        }

        if (tagValue != null && tagValueWildcard != null) {
            throw new ScmInvalidArgumentException(
                    "tagValue and tagValueWildcard cannot be set at the same time");
        }

        BSONObject condition = genCustomTagCondition(tagKey, tagKeyWildcard, tagValue,
                tagValueWildcard);

        if (orderBy != null) {
            // 合法的用户 orderby key 是 tag_id、key、value
            checkOrderByBSON(orderBy, Arrays.asList(FieldName.TagLib.TAG_ID,
                    FieldName.TagLib.CUSTOM_TAG_TAG_KEY, FieldName.TagLib.CUSTOM_TAG_TAG_VALUE));
            // 将用户 key 作为转为数据库字段：key=>custom_tag.key、value=>custom_tag.value
            Object obj = orderBy.removeField(FieldName.TagLib.CUSTOM_TAG_TAG_KEY);
            if (obj != null) {
                orderBy.put(FieldName.TagLib.CUSTOM_TAG + "." + FieldName.TagLib.CUSTOM_TAG_TAG_KEY,
                        obj);
            }
            obj = orderBy.removeField(FieldName.TagLib.CUSTOM_TAG_TAG_VALUE);
            if (obj != null) {
                orderBy.put(
                        FieldName.TagLib.CUSTOM_TAG + "." + FieldName.TagLib.CUSTOM_TAG_TAG_VALUE,
                        obj);
            }
        }
        else {
            orderBy = new BasicBSONObject(FieldName.TagLib.CUSTOM_TAG, 1);
        }
        response.setHeader("Content-Type", "application/json;charset=utf-8");
        MetaCursor cursor = tagService.queryTag(workspaceName, user, condition,
                orderBy, skip, limit);
        ServiceUtils.putCursorToResponse(cursor, response);
    }

    @GetMapping(value = "/tag", params = { "action=get_custom_tag_key" })
    public void listCustomTagKey(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(value = CommonDefine.RestArg.TAG_KEY_WILDCARD, required = false) String tagKeyWildcard,
            @RequestParam(value = CommonDefine.RestArg.ASCENDING, required = false, defaultValue = "true") boolean ascending,
            @RequestParam(value = CommonDefine.RestArg.SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = CommonDefine.RestArg.LIMIT, required = false, defaultValue = "-1") long limit,
            HttpServletResponse response, Authentication auth) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        response.setHeader("Content-Type", "application/json;charset=utf-8");
        BSONObject condition = genCustomTagCondition(null, tagKeyWildcard, null, null);
        MetaCursor cursor = tagService.queryCustomTagKey(workspaceName, user, condition, ascending,
                skip, limit);
        ServiceUtils.putCursorToResponse(cursor, response);
    }

    private BSONObject genCustomTagCondition(String tagKey, String tagKeyWildcard, String tagValue,
            String tagValueWildcard) {
        BSONObject ret = new BasicBSONObject();
        BasicBSONList andList = new BasicBSONList();
        ret.put("$and", andList);
        andList.add(
                new BasicBSONObject(FieldName.TagLib.TAG_TYPE, TagType.CUSTOM_TAG.getFileField()));
        if (tagKey != null) {
            andList.add(new BasicBSONObject(
                    FieldName.TagLib.CUSTOM_TAG + "." + FieldName.TagLib.CUSTOM_TAG_TAG_KEY,
                    tagKey));
        }

        if (tagKeyWildcard != null) {
            andList.add(new BasicBSONObject(
                    FieldName.TagLib.CUSTOM_TAG + "." + FieldName.TagLib.CUSTOM_TAG_TAG_KEY,
                    new BasicBSONObject("$regex", ScmStringUtil.wildcardToRegex(tagKeyWildcard))));
        }

        if (tagValue != null) {
            andList.add(new BasicBSONObject(
                    FieldName.TagLib.CUSTOM_TAG + "." + FieldName.TagLib.CUSTOM_TAG_TAG_VALUE,
                    tagValue));
        }

        if (tagValueWildcard != null) {
            andList.add(new BasicBSONObject(
                    FieldName.TagLib.CUSTOM_TAG + "." + FieldName.TagLib.CUSTOM_TAG_TAG_VALUE,
                    new BasicBSONObject("$regex",
                            ScmStringUtil.wildcardToRegex(tagValueWildcard))));
        }

        return ret;
    }

    @GetMapping(value = "/tag", params = { "action=get_tag" })
    public void getTag(@RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(value = CommonDefine.RestArg.TAG, required = false) String tag,
            @RequestParam(value = CommonDefine.RestArg.TAG_WILDCARD, required = false) String tagWildcard,
            @RequestParam(value = CommonDefine.RestArg.ORDER_BY, required = false) BSONObject orderBy,
            @RequestParam(value = CommonDefine.RestArg.SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = CommonDefine.RestArg.LIMIT, required = false, defaultValue = "-1") long limit,
            HttpServletResponse response, Authentication auth) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        if (tagWildcard != null && tag != null) {
            throw new ScmInvalidArgumentException(
                    "tag and tagWildcard cannot be set at the same time");
        }

        BSONObject condition = genTagCondition(tag, tagWildcard);

        if (orderBy != null) {
            checkOrderByBSON(orderBy, Arrays.asList(FieldName.TagLib.TAG_ID, FieldName.TagLib.TAG));
        }
        else {
            orderBy = new BasicBSONObject(FieldName.TagLib.TAG, 1);
        }
        response.setHeader("Content-Type", "application/json;charset=utf-8");
        MetaCursor cursor = tagService.queryTag(workspaceName, user, condition,
                orderBy, skip, limit);
        ServiceUtils.putCursorToResponse(cursor, response);
    }

    @GetMapping(value = "/tag", params = "action=count_tag")
    public ResponseEntity countTag(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(value = CommonDefine.RestArg.TAG_WILDCARD, required = false) String tagWildcard,
            HttpServletResponse resp, Authentication auth) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        BSONObject condition = genTagCondition(null, tagWildcard);
        long count = tagService.countTag(workspaceName, user, condition);
        resp.setHeader(CommonDefine.RestArg.X_SCM_COUNT, String.valueOf(count));
        return ResponseEntity.ok().header(CommonDefine.RestArg.X_SCM_COUNT, String.valueOf(count))
                .build();
    }

    @GetMapping(value = "/tag", params = "action=count_custom_tag")
    public ResponseEntity countCustomTagTag(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(value = CommonDefine.RestArg.TAG_KEY_WILDCARD, required = false) String tagKeyWildcard,
            @RequestParam(value = CommonDefine.RestArg.TAG_VALUE_WILDCARD, required = false) String tagValueWildcard,
            HttpServletResponse resp, Authentication auth) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        BSONObject condition = genCustomTagCondition(null, tagKeyWildcard, null, tagValueWildcard);
        long count = tagService.countTag(workspaceName, user, condition);
        resp.setHeader(CommonDefine.RestArg.X_SCM_COUNT, String.valueOf(count));
        return ResponseEntity.ok().header(CommonDefine.RestArg.X_SCM_COUNT, String.valueOf(count))
                .build();
    }

    private void checkOrderByBSON(BSONObject bson, List<String> validKeys)
            throws ScmInvalidArgumentException {
        for (String key : bson.keySet()) {
            if (!validKeys.contains(key)) {
                throw new ScmInvalidArgumentException("invalid orderBy, invalid key: " + key);
            }
        }
    }
    private BSONObject genTagCondition(String tag, String tagWildcard) {
        BasicBSONObject ret = new BasicBSONObject();
        ret.put(FieldName.TagLib.TAG_TYPE, TagType.TAGS.getFileField());
        if (tag != null) {
            ret.put(FieldName.TagLib.TAG, tag);
        }
        if (tagWildcard != null) {
            ret.put(FieldName.TagLib.TAG,
                    new BasicBSONObject("$regex", ScmStringUtil.wildcardToRegex(tagWildcard)));
        }
        return ret;
    }

    public static void checkConditionBsonKey(BSONObject bson, Set<String> invalidKeys)
            throws ScmInvalidArgumentException {
        if (bson == null) {
            return;
        }
        if (bson instanceof BasicBSONList) {
            BasicBSONList list = (BasicBSONList) bson;
            for (Object ele : list) {
                if (ele instanceof BSONObject) {
                    checkConditionBsonKey((BSONObject) ele, invalidKeys);
                }
            }
        }
        else {
            for (String key : bson.keySet()) {
                if (invalidKeys.contains(key)) {
                    throw new ScmInvalidArgumentException("invalid key:key=" + key);
                }
                Object value = bson.get(key);
                if (value instanceof BSONObject) {
                    checkConditionBsonKey((BSONObject) value, invalidKeys);
                }
            }
        }
    }
}
