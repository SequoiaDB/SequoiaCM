package com.sequoiacm.client.core;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.bson.BSONObject;
import org.bson.util.JSON;

import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmSystemException;

class ScmOutputStreamImpl extends ScmHttpOutputStreamBase {
    private ScmFile scmFile;
    private ScmSession session;
    private ScmUploadConf conf;

    public ScmOutputStreamImpl(ScmFile scmFile, ScmUploadConf conf) throws ScmException {
        this.scmFile = scmFile;
        this.session = scmFile.getSession();
        this.conf = conf;
    }

    @Override
    public void processAfterCommit() throws IOException, ScmException {
        String respFile = readStringFromStream(getConnection().getInputStream());

        BSONObject resp = (BSONObject) JSON.parse(respFile);
        BSONObject fileInfo = (BSONObject) resp.get("file");

        scmFile.refresh(fileInfo);
        scmFile.setExist(true);
    }

    @Override
    protected HttpURLConnection createHttpUrlConnection() throws ScmException {
        return scmFile.httpURLConnectionForSave(conf);
    }

    @Override
    protected ScmException _handleErrorResp(IOException cause) {
        return new ScmSystemException("failed to create file:file=" + scmFile.toString(), cause);
    }

}
