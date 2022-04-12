package com.sequoiacm.contentserver.service;

import com.sequoiacm.common.checksum.ChecksumType;
import com.sequoiacm.contentserver.model.BreakpointFile;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import org.bson.BSONObject;

import java.io.InputStream;
import java.util.List;

public interface IBreakpointFileService {

    BreakpointFile getBreakpointFile(ScmUser user, String workspaceName, String fileName)
            throws ScmServerException;

    List<BreakpointFile> getBreakpointFiles(ScmUser user, String workspaceName, BSONObject filter)
            throws ScmServerException;

    BreakpointFile createBreakpointFile(ScmUser user, String workspaceName, String fileName,
            long createTime, ChecksumType checksumType, InputStream fileStream,
            boolean isLastContent, boolean isNeedMd5) throws ScmServerException;

    BreakpointFile uploadBreakpointFile(ScmUser user, String workspaceName, String file,
            InputStream fileStream, long offset, boolean isLastContent) throws ScmServerException;

    void deleteBreakpointFile(ScmUser user, String workspaceName, String fileName) throws ScmServerException;

    String calcBreakpointFileMd5(ScmUser user, String workspaceName, String fileName) throws ScmServerException;
}
