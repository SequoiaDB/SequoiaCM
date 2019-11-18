package com.sequoiacm.perf.driver;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Driver  {
    private String url;
    private String user;
    private String password;

    public Driver(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public String upload(String workspace, String fileName, InputStream fileStream) throws ScmException {
        ScmSession session = ScmFactory.Session.createSession(
                ScmType.SessionType.AUTH_SESSION, new ScmConfigOption(url, user, password));

        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspace, session);
            ScmFile f = ScmFactory.File.createInstance(ws);
            f.setFileName(fileName);
            f.setContent(fileStream);
            return f.save().get();
        } finally {
            session.close();
        }
    }

    public void download(String workspace, String fileId, OutputStream fileStream) throws ScmException {
        ScmSession session = ScmFactory.Session.createSession(
                ScmType.SessionType.AUTH_SESSION, new ScmConfigOption(url, user, password));
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspace, session);
            ScmFile f = ScmFactory.File.getInstance(ws, new ScmId(fileId));
            f.getContent(fileStream);
        } finally {
            session.close();
            try {
                fileStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}