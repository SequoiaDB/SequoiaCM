package com.sequoiacm.client.core;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.util.JSON;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmSystemException;

class ScmOutputStreamImpl extends ScmHttpOutputStreamBase {
    private ScmFile scmFile;
    private ScmSession session;

    public ScmOutputStreamImpl(ScmFile scmFile) throws ScmException {
        this.scmFile = scmFile;
        this.session = scmFile.getSession();
    }

    @Override
    public void processAfterCommit() throws IOException, ScmException {
        String respFile = readStringFromStream(getConnection().getInputStream());

        BSONObject resp = (BSONObject) JSON.parse(respFile);
        String fileId = (String) ((BSONObject) resp.get("file")).get("id");

        BasicBSONObject fileInfo = (BasicBSONObject) session.getDispatcher()
                .getFileInfo(scmFile.getWorkspaceName(), fileId, null, -1, -1);
        scmFile.refresh(fileInfo);
        scmFile.setExist(true);
    }

    @Override
    protected HttpURLConnection createHttpUrlConnection() throws ScmException {
        return session.getDispatcher().getFileUploadConnection(scmFile.getWorkspaceName(),
                scmFile.toBSONObject());
    }

    @Override
    protected ScmException _handleErrorResp(IOException cause) {
        return new ScmSystemException("failed to create file:file=" + scmFile.toString(), cause);
    }

}
