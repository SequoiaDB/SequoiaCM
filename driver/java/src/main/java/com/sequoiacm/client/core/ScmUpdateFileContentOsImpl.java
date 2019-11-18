package com.sequoiacm.client.core;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLDecoder;

import org.bson.BSONObject;
import org.bson.util.JSON;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmSystemException;
import com.sequoiacm.common.CommonDefine;

class ScmUpdateFileContentOsImpl extends ScmHttpOutputStreamBase {
    private ScmFile scmFile;
    private ScmSession session;

    public ScmUpdateFileContentOsImpl(ScmFile scmFile) throws ScmException {
        this.scmFile = scmFile;
        session = scmFile.getSession();
    }

    @Override
    protected void processAfterCommit() throws IOException, ScmException {
        // read body, for check no errResp.
        readStringFromStream(getConnection().getInputStream());

        String newInfo = getConnection().getHeaderField(CommonDefine.RestArg.FILE_INFO);
        newInfo = URLDecoder.decode(newInfo, "utf-8");
        BSONObject newFileInfo = (BSONObject) JSON.parse(newInfo);
        scmFile.refresh(newFileInfo);
    }

    @Override
    protected HttpURLConnection createHttpUrlConnection() throws ScmException {
        return session.getDispatcher().getUpdateFileContentConn(scmFile.getWorkspaceName(),
                scmFile.getFileId().get(), scmFile.getMajorVersion(), scmFile.getMinorVersion());
    }

    @Override
    protected ScmException _handleErrorResp(IOException cause) {
        return new ScmSystemException(
                "failed to update file content:fileId=" + scmFile.getFileId().get() + ",version="
                        + scmFile.getMajorVersion() + "." + scmFile.getMinorVersion(),
                cause);
    }

}
