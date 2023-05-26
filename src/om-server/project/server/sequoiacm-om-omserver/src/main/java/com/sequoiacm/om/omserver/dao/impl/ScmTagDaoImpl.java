package com.sequoiacm.om.omserver.dao.impl;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.tag.ScmCustomTag;
import com.sequoiacm.client.element.tag.ScmTag;
import com.sequoiacm.client.element.tag.ScmTagCondition;
import com.sequoiacm.client.element.tag.ScmTagConditionBuilder;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.om.omserver.common.CommonUtil;
import com.sequoiacm.om.omserver.common.ScmFileUtil;
import com.sequoiacm.om.omserver.dao.ScmTagDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.module.OmFileBasic;
import com.sequoiacm.om.omserver.module.tag.OmCustomTagDetail;
import com.sequoiacm.om.omserver.module.tag.OmTagBasic;
import com.sequoiacm.om.omserver.module.tag.OmTagFilter;
import com.sequoiacm.om.omserver.module.tag.OmTagType;
import com.sequoiacm.om.omserver.module.tag.OmTagsDetail;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BasicBSONObject;

public class ScmTagDaoImpl implements ScmTagDao {

    private ScmOmSession session;

    public ScmTagDaoImpl(ScmOmSession session) {
        this.session = session;
    }

    @Override
    public List<OmTagBasic> listTag(String wsName, String tagType, OmTagFilter omTagFilter,
            long skip, int limit) throws ScmInternalException {
        ScmSession con = session.getConnection();
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, con);
            if (tagType.equals(OmTagType.TAGS.getType())) {
                String tagMatcher = omTagFilter.getTagMatcher();
                return listTags(ws, tagMatcher, skip, limit);
            }
            String keyMatcher = omTagFilter.getKeyMatcher();
            String valueMatcher = omTagFilter.getValueMatcher();
            return listCustomTag(ws, keyMatcher, valueMatcher, skip, limit);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), "failed to list tag, " + e.getMessage(),
                    e);
        }
    }

    private List<OmTagBasic> listTags(ScmWorkspace ws, String tagMatcher, long skip, int limit)
            throws ScmException {
        List<OmTagBasic> tagList = new ArrayList<>();
        ScmCursor<ScmTag> cursor = null;
        try {
            cursor = ScmFactory.Tag.listTags(ws, tagMatcher, new BasicBSONObject(), skip, limit);
            while (cursor.hasNext()) {
                ScmTag scmTag = cursor.getNext();
                tagList.add(new OmTagsDetail(scmTag));
            }
        }
        finally {
            CommonUtil.closeResource(cursor);
        }
        return tagList;
    }

    private List<OmTagBasic> listCustomTag(ScmWorkspace ws, String keyMatcher, String valueMatcher,
            long skip, int limit) throws ScmException {
        List<OmTagBasic> tagList = new ArrayList<>();
        ScmCursor<ScmCustomTag> cursor = null;
        try {
            cursor = ScmFactory.CustomTag.listCustomTag(ws, keyMatcher, valueMatcher,
                    new BasicBSONObject(), skip, limit);
            while (cursor.hasNext()) {
                ScmCustomTag customTag = cursor.getNext();
                tagList.add(new OmCustomTagDetail(customTag));
            }
        }
        finally {
            CommonUtil.closeResource(cursor);
        }
        return tagList;
    }

    @Override
    public long countTag(String wsName, String tagType, OmTagFilter omTagFilter)
            throws ScmInternalException {
        ScmSession con = session.getConnection();
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, con);
            if (tagType.equals(OmTagType.TAGS.getType())) {
                return ScmFactory.Tag.countTags(ws, omTagFilter.getTagMatcher());
            }
            return ScmFactory.CustomTag.countCustomTag(ws, omTagFilter.getKeyMatcher(),
                    omTagFilter.getValueMatcher());
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), "failed to count tag, " + e.getMessage(),
                    e);
        }
    }

    @Override
    public List<String> listCustomTagKey(String wsName, String keyMatcher, long skip, int limit)
            throws ScmInternalException {
        ScmSession con = session.getConnection();
        ScmCursor<String> cursor = null;
        try {
            List<String> res = new ArrayList<>();
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, con);
            cursor = ScmFactory.CustomTag.listCustomTagKey(ws, keyMatcher, true, skip, limit);
            while (cursor.hasNext()) {
                res.add(cursor.getNext());
            }
            return res;
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to list key of custom tag, " + e.getMessage(), e);
        }
        finally {
            CommonUtil.closeResource(cursor);
        }
    }

    @Override
    public List<OmFileBasic> searchFileWithTag(String wsName, int scope, BSONObject tagCondition,
            BSONObject fileCondition, BSONObject orderBy, long skip, long limit)
            throws ScmInternalException {
        ScmSession con = session.getConnection();
        List<OmFileBasic> res = new ArrayList<>();
        ScmCursor<ScmFileBasicInfo> cursor = null;
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, con);
            ScmTagCondition scmTagCondition = ScmTagConditionBuilder.builder()
                    .fromBson(tagCondition);
            cursor = ScmFactory.Tag.searchFile(ws, ScmType.ScopeType.getScopeType(scope),
                    scmTagCondition, fileCondition, orderBy, skip, limit);
            while (cursor.hasNext()) {
                ScmFileBasicInfo basicInfo = cursor.getNext();
                if (!basicInfo.isDeleteMarker()) {
                    ScmFile file = ScmFactory.File.getInstance(ws, basicInfo.getFileId(),
                            basicInfo.getMajorVersion(), basicInfo.getMinorVersion());
                    res.add(ScmFileUtil.transformToFileBasicInfo(file));
                    continue;
                }
                res.add(ScmFileUtil.transformToFileBasicInfo(basicInfo));
            }
            return res;
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "Failed to search file with tag, " + e.getMessage(), e);
        }
        finally {
            CommonUtil.closeResource(cursor);
        }
    }

    @Override
    public long countFileWithTag(String wsName, int scope, BSONObject tagCondition,
            BSONObject fileCondition) throws ScmInternalException {
        ScmSession con = session.getConnection();
        try {
            ScmType.ScopeType scopeType = ScmType.ScopeType.getScopeType(scope);
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, con);
            ScmTagCondition scmTagCondition = ScmTagConditionBuilder.builder()
                    .fromBson(tagCondition);
            return ScmFactory.Tag.countFile(ws, scopeType, scmTagCondition, fileCondition);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "Failed to count file with tag, " + e.getMessage(), e);
        }
    }
}
