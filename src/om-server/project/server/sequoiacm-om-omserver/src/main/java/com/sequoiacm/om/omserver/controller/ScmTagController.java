package com.sequoiacm.om.omserver.controller;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.sequoiacm.om.omserver.exception.ScmOmServerError;
import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.tag.OmTagBasic;
import com.sequoiacm.om.omserver.module.tag.OmTagFilter;
import com.sequoiacm.om.omserver.service.ScmTagService;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@RestController
@RequestMapping("/api/v1")
public class ScmTagController {

    @Autowired
    private ScmTagService tagService;

    @GetMapping("/tags/{tag_type}")
    public List<OmTagBasic> listTag(@PathVariable("tag_type") String tagType,
            @RequestParam(value = RestParamDefine.WORKSPACE) String wsName,
            @RequestParam(value = RestParamDefine.SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = RestParamDefine.LIMIT, required = false, defaultValue = "1000") int limit,
            @RequestParam(value = RestParamDefine.TAG_FILTER, required = false, defaultValue = "{}") BSONObject tagFilter,
            ScmOmSession session, HttpServletResponse response)
            throws ScmInternalException, ScmOmServerException {
        OmTagFilter omTagFilter = new OmTagFilter(tagFilter);
        long tagCount = tagService.countTag(session, wsName, tagType, omTagFilter);
        response.setHeader(RestParamDefine.X_RECORD_COUNT, String.valueOf(tagCount));
        return tagService.listTag(session, wsName, tagType, omTagFilter, skip, limit);
    }

    @GetMapping("/tags/custom_tag/key")
    public List<String> listCustomTagKey(
            @RequestParam(value = RestParamDefine.WORKSPACE) String wsName,
            @RequestParam(value = RestParamDefine.KEY_MATCHER, required = false) String keyMatcher,
            @RequestParam(value = RestParamDefine.SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = RestParamDefine.LIMIT, required = false, defaultValue = "1000") int limit,
            ScmOmSession session, HttpServletResponse response)
            throws ScmInternalException, ScmOmServerException {
        if (skip != 0) {
            // 暂无分页需求，先不做实现
            throw new ScmOmServerException(ScmOmServerError.INVALID_ARGUMENT,
                    "skip is not supported.");
        }
        List<String> keys = tagService.listCustomTagKey(session, wsName, keyMatcher, skip, limit);
        response.setHeader(RestParamDefine.X_RECORD_COUNT, String.valueOf(keys.size()));
        return keys;
    }
}
