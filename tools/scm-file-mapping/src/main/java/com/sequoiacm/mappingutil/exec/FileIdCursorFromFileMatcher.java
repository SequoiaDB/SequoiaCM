package com.sequoiacm.mappingutil.exec;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.mappingutil.config.ScmResourceMgr;
import com.sequoiacm.mappingutil.exception.ScmExitCode;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class FileIdCursorFromFileMatcher implements FileIdCursor {

    private ScmSession session;
    private ScmCursor<ScmFileBasicInfo> innerCursor;
    private long lastCreateTime;

    public FileIdCursorFromFileMatcher(String workspace, BSONObject fileMatcher, long createTime)
            throws ScmToolsException {
        this.lastCreateTime = createTime;
        this.session = ScmResourceMgr.getInstance().getSession();
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspace, session);
            BSONObject orderBy = new BasicBSONObject();
            orderBy.put(FieldName.FIELD_CLFILE_INNER_CREATE_TIME, 1);

            if (createTime > 0) {
                BSONObject condition = ScmQueryBuilder
                        .start(FieldName.FIELD_CLFILE_INNER_CREATE_TIME)
                        .greaterThanEquals(createTime).get();
                fileMatcher.putAll(condition);
            }
            innerCursor = ScmFactory.File.listInstance(ws, ScmType.ScopeType.SCOPE_CURRENT,
                    fileMatcher, orderBy, 0, -1);
        }
        catch (ScmException e) {
            ScmCommon.closeResource(innerCursor, session);
            throw new ScmToolsException("Failed to init id cursor", ScmExitCode.SYSTEM_ERROR, e);
        }
    }

    public long getMarker() {
        return this.lastCreateTime;
    }

    @Override
    public ScmId getNext() throws ScmToolsException {
        try {
            ScmFileBasicInfo fileBasicInfo = innerCursor.getNext();
            this.lastCreateTime = fileBasicInfo.getCreateDate().getTime();
            return fileBasicInfo.getFileId();
        }
        catch (ScmException e) {
            throw new ScmToolsException("Failed to get next file", ScmExitCode.SYSTEM_ERROR, e);
        }
    }

    @Override
    public boolean hasNext() {
        return innerCursor.hasNext();
    }

    @Override
    public void close() {
        ScmCommon.closeResource(innerCursor, session);
    }
}
