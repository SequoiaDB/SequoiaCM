package com.sequoiacm.physicaldelete;

import java.io.File;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

//多中心存在的文件，物理删除，查看删除结果
public class DeleteFileMulticenter extends ScmTestMultiCenterBase {
    private String srcFile;
    private ScmId fileID;
    private ScmSession ss;
    private ScmWorkspace ws;
    private String metaCS;
    private String LobCS;

    @BeforeClass
    public void setUp() throws ScmException {
        metaCS = getWorkspaceName() + "_META";
        LobCS = getWorkspaceName() + "_LOB";
        srcFile = getDataDirectory() + File.separator + "test.txt";
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer2().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
        // server2 createFile
        fileID = ScmTestTools.createScmFile(ws, srcFile, DeleteFileMulticenter.class.getName(), "", "").getFileId();
    }

    @Test
    public void deleteFileMulticenter() throws ScmException {
        // server3 create ScmFileInputStream
        ScmSession session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer3().getUrl(), getScmUser(),
                        getScmPasswd()));
        try {
            ScmFile file = ScmFactory.File.getInstance(ws, fileID);
            ScmInputStream in = ScmFactory.File.createInputStream( file);
            byte[] buf = new byte[1024];
            while(true) {
                int len = in.read(buf, 0, 1024);
                if(len ==-1) {
                    break;
                }
            }
            in.close();

            // now all center have this file,delete it
            file.delete(true);
        }
        finally {
            session.close();
        }

        // check sdb1 META
        String url1 = getSdb1().getUrl();
        Assert.assertFalse(
                ScmTestTools.isRecordExist(url1, getSdbUser(), getSdbPasswd(), metaCS,
                        "FILE", "{id:'" + fileID.get() + "'}"));
    }

    @AfterClass
    public void tearDown() throws ScmException {
        ss.close();
    }
}
