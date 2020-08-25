package com.sequoiacm.contentserver.service;

import java.io.InputStream;
import java.util.List;

import org.bson.BSONObject;

import com.sequoiacm.common.checksum.ChecksumType;
import com.sequoiacm.contentserver.exception.ScmServerException;
import com.sequoiacm.contentserver.model.BreakpointFile;

public interface IBreakpointFileService {

    BreakpointFile getBreakpointFile(String workspaceName, String fileName)
            throws ScmServerException;

    List<BreakpointFile> getBreakpointFiles(String workspaceName, BSONObject filter)
            throws ScmServerException;

    BreakpointFile createBreakpointFile(String createUser, String workspaceName, String fileName,
            long createTime, ChecksumType checksumType, InputStream fileStream,
            boolean isLastContent, boolean isNeedMd5) throws ScmServerException;

    BreakpointFile uploadBreakpointFile(String uploadUser, String workspaceName, String file,
            InputStream fileStream, long offset, boolean isLastContent) throws ScmServerException;

    void deleteBreakpointFile(String workspaceName, String fileName) throws ScmServerException;

    String calcBreakpointFileMd5(String workspaceName, String fileName) throws ScmServerException;
}
