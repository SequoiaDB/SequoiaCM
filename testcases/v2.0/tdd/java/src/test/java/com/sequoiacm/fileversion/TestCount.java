package com.sequoiacm.fileversion;

import java.io.ByteArrayInputStream;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;

public class TestCount extends ScmTestMultiCenterBase {
    private ScmSession ss;
    private ScmWorkspace ws;
    private ScmFile f1;
    private ScmFile f2;

    @BeforeClass
    public void init() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
        clearEnv();
    }

    private void clearEnv() throws ScmException {
        ScmCursor<ScmFileBasicInfo> c = ScmFactory.File.listInstance(ws, ScopeType.SCOPE_CURRENT,
                ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR)
                .is(this.getClass().getSimpleName()).get());
        try {
            while (c.hasNext()) {
                ScmFactory.File.deleteInstance(ws, c.getNext().getFileId(), true);
            }
        }
        finally {
            c.close();
        }
    }

    @Test
    public void test() throws ScmException {
        f1 = ScmFactory.File.createInstance(ws);
        f1.setFileName(this.getClass().getSimpleName() + "1");
        f1.setAuthor(this.getClass().getSimpleName());
        f1.save();
        f1.updateContent(new ByteArrayInputStream("version2".getBytes()));
        f1.updateContent(new ByteArrayInputStream("version3".getBytes()));

        f2 = ScmFactory.File.createInstance(ws);
        f2.setFileName(this.getClass().getSimpleName() + "2");
        f2.setAuthor(this.getClass().getSimpleName());
        f2.save();
        f2.updateContent(new ByteArrayInputStream("version2".getBytes()));
        f2.updateContent(new ByteArrayInputStream("version3".getBytes()));

        long allCount = ScmFactory.File.countInstance(ws, ScopeType.SCOPE_ALL,
                ScmQueryBuilder.start()
                .or(ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID)
                        .is(f1.getFileId().get()).get(),
                        ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID)
                        .is(f2.getFileId().get()).get())
                .get());
        Assert.assertEquals(allCount, 6);

        long historyCount = ScmFactory.File.countInstance(ws, ScopeType.SCOPE_HISTORY,
                ScmQueryBuilder.start()
                .or(ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID)
                        .is(f1.getFileId().get()).get(),
                        ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID)
                        .is(f2.getFileId().get()).get())
                .get());
        Assert.assertEquals(historyCount, 4);

        long currentCount = ScmFactory.File.countInstance(ws, ScopeType.SCOPE_CURRENT,
                ScmQueryBuilder.start()
                .or(ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID)
                        .is(f1.getFileId().get()).get(),
                        ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID)
                        .is(f2.getFileId().get()).get())
                .get());
        Assert.assertEquals(currentCount, 2);
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            f1.delete(true);
            f2.delete(true);
        }
        finally {
            ss.close();
        }
    }
}
