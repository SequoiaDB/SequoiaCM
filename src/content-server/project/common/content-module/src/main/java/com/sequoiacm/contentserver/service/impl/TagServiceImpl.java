package com.sequoiacm.contentserver.service.impl;

import com.sequoiacm.common.ScmWorkspaceTagRetrievalStatus;
import com.sequoiacm.contentserver.exception.ScmOperationUnsupportedException;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.privilege.ScmFileServicePriv;
import com.sequoiacm.contentserver.service.IFileService;
import com.sequoiacm.contentserver.service.ITagService;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.contentserver.tag.TagLibDao;
import com.sequoiacm.contentserver.tag.syntaxtree.TagSyntaxTree;
import com.sequoiacm.contentserver.tag.syntaxtree.TagSyntaxTreeConverter;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.metasource.MetaCursor;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TagServiceImpl implements ITagService {
    private static final Logger logger = LoggerFactory.getLogger(TagServiceImpl.class);
    @Autowired
    private TagSyntaxTreeConverter tagSyntaxTreeConverter;

    @Autowired
    private IFileService fileService;

    @Autowired
    private TagLibDao tagLibDao;

    public MetaCursor searchFile(String ws, ScmUser user, TagSyntaxTree tagSyntaxTree,
            BSONObject fileCondition, BSONObject orderBy, int scope, long skip, long limit,
            boolean isResContainsDeleteMarker) throws ScmServerException {
        BSONObject fileMatcher = checkAndGenFileMatcher(ws, tagSyntaxTree, fileCondition);

        return fileService.getFileList(user, ws, fileMatcher, scope, orderBy, skip, limit, null,
                isResContainsDeleteMarker);
    }

    private BSONObject checkAndGenFileMatcher(String ws, TagSyntaxTree tagSyntaxTree,
            BSONObject fileCondition) throws ScmServerException {
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance().getWorkspaceInfoCheckExist(ws);
        if (wsInfo.getTagRetrievalStatus() != ScmWorkspaceTagRetrievalStatus.ENABLED) {
            throw new ScmOperationUnsupportedException(
                    "Tag retrieval is not enabled for workspace: ws=" + ws + ", status="
                            + wsInfo.getTagRetrievalStatus());
        }

        logger.debug(
                "gen file condition by tagSyntaxTree: ws={}, tagSyntaxTree={}, fileCondition={}",
                ws, tagSyntaxTree, fileCondition);
        BSONObject fileTagMatcher = tagSyntaxTreeConverter.convertToSdbFileMatcher(wsInfo,
                tagSyntaxTree);
        logger.debug("change tag syntax tree to sdb file condition: {}", fileTagMatcher);

        BSONObject fileMatcher = fileTagMatcher;
        if (fileCondition != null && !fileCondition.isEmpty()) {
            BasicBSONList andArr = new BasicBSONList();
            andArr.add(fileTagMatcher);
            andArr.add(fileCondition);
            fileMatcher = new BasicBSONObject("$and", andArr);
        }
        return fileMatcher;
    }

    @Override
    public long countFile(String ws, ScmUser user, TagSyntaxTree tagSyntaxTree,
            BSONObject fileCondition, int scope, boolean isResContainsDeleteMarker)
            throws ScmServerException {
        BSONObject fileMatcher = checkAndGenFileMatcher(ws, tagSyntaxTree, fileCondition);

        return fileService.countFiles(user, ws, scope, fileMatcher, isResContainsDeleteMarker);
    }

    @Override
    public MetaCursor queryTag(String ws, ScmUser user, BSONObject condition, BSONObject orderBy,
            long skip, long limit) throws ScmServerException {
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance().getWorkspaceInfoCheckExist(ws);
        ScmFileServicePriv.getInstance().checkWsPriority(user, ws, ScmPrivilegeDefine.READ,
                "query tag lib");
        return tagLibDao.query(wsInfo, condition, skip, limit, orderBy);
    }

    @Override
    public MetaCursor queryCustomTagKey(String workspaceName, ScmUser user, BSONObject condition,
            boolean ascending, long skip, long limit) throws ScmServerException {
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                .getWorkspaceInfoCheckExist(workspaceName);
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.READ, "query tag lib");
        return tagLibDao.queryCustomTagKey(wsInfo, condition, ascending, skip, limit);
    }

    @Override
    public long countTag(String workspaceName, ScmUser user, BSONObject condition)
            throws ScmServerException {
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                .getWorkspaceInfoCheckExist(workspaceName);
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.READ, "count tag lib");
        return tagLibDao.countTag(wsInfo, condition);
    }
}
