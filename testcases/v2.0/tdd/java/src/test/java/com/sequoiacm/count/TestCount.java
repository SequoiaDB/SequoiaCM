package com.sequoiacm.count;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
/**
 * 创建10个文件，带条件count，查看count结果
 *
 * @author huangqiaohui
 *
 */
public class TestCount extends ScmTestMultiCenterBase {
    private String srcFile;
    private ScmId fileID;
    private ScmSession ss;
    private ScmWorkspace ws;
    private List<ScmFile> fileList = new ArrayList<>();

    @BeforeClass
    public void setUp() throws ScmException {
        srcFile = getDataDirectory() + File.separator + "test.txt";
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer2().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);

        // server2 create 10 File
        for (int i = 0; i < 10; i++) {
            ScmFile file = ScmFactory.File.createInstance(ws);
            file.setFileName(System.currentTimeMillis()+"_"+i);
            file.setAuthor("test");
            file.setTitle("sequoiacm");
            file.setMimeType(MimeType.PLAIN);
            file.save();
            fileList.add(file);
        }
    }

    @Test
    public void count() throws ScmException {
        List<String> list = new ArrayList<>();
        list.add(fileList.get(0).getFileName());
        list.add(fileList.get(1).getFileName());
        list.add(fileList.get(2).getFileName());
        list.add(fileList.get(3).getFileName());
        list.add(fileList.get(4).getFileName());

        BSONObject obj = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).in(list).get();
        long count = ScmFactory.File.countInstance(ws, ScopeType.SCOPE_CURRENT, obj);//only support SCOPE_CURRENT
        Assert.assertEquals(count, 5);
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            for (ScmFile f : fileList) {
                f.delete(true);
            }
        }
        finally {
            ss.close();
        }
    }
}
