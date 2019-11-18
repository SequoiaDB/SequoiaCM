package com.sequoiacm.fileversion;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

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

public class TestList extends ScmTestMultiCenterBase {
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

        testCurrentList();
        testHistoryList();
        testAllList();
    }

    private void testHistoryList() throws ScmException {
        ScmCursor<ScmFileBasicInfo> c = ScmFactory.File.listInstance(ws, ScopeType.SCOPE_HISTORY,
                ScmQueryBuilder.start().or(ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID)
                        .is(f1.getFileId().get()).get(), ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID)
                        .is(f2.getFileId().get()).get()).get());
        ArrayList<ScmFileBasicInfo> res = new ArrayList<>();
        try {
            while (c.hasNext()) {
                ScmFileBasicInfo f = c.getNext();
                res.add(f);
            }
        }
        finally {
            c.close();
        }

        Assert.assertEquals(res.size(), 4, res.toString());
        ArrayList<ScmFileBasicInfo> f1Files = new ArrayList<>();
        ArrayList<ScmFileBasicInfo> f2Files = new ArrayList<>();
        for (ScmFileBasicInfo f : res) {
            if (f.getFileId().get().equals(f1.getFileId().get())) {
                f1Files.add(f);
            }
            else if (f.getFileId().get().equals(f2.getFileId().get())) {
                f2Files.add(f);
            }
            else {
                Assert.fail(f.toString());
            }
        }
        Assert.assertEquals(f1Files.size(), 2, res.toString());
        Assert.assertEquals(f2Files.size(), 2, res.toString());
        List<Integer> versionList1 = new ArrayList<>();
        versionList1.add(1);
        versionList1.add(2);
        List<Integer> versionList2 = new ArrayList<>(versionList1);
        for (ScmFileBasicInfo f1File : f1Files) {
            Assert.assertTrue(versionList1.contains(f1File.getMajorVersion()), res.toString());
            Assert.assertTrue(f1File.getMinorVersion() == 0, res.toString());
            versionList1.remove(Integer.valueOf(f1File.getMajorVersion()));
        }
        Assert.assertEquals(versionList1.size(), 0, res.toString());

        for (ScmFileBasicInfo f2File : f2Files) {
            Assert.assertTrue(versionList2.contains(f2File.getMajorVersion()), res.toString());
            Assert.assertTrue(f2File.getMinorVersion() == 0, res.toString());
            versionList2.remove(Integer.valueOf(f2File.getMajorVersion()));
        }
        Assert.assertEquals(versionList2.size(), 0, res.toString());
    }

    private void testAllList() throws ScmException {
        ScmCursor<ScmFileBasicInfo> c = ScmFactory.File.listInstance(ws, ScopeType.SCOPE_ALL,
                ScmQueryBuilder.start().or(ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID)
                        .is(f1.getFileId().get()).get(), ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID)
                        .is(f2.getFileId().get()).get()).get());
        ArrayList<ScmFileBasicInfo> res = new ArrayList<>();
        try {
            while (c.hasNext()) {
                ScmFileBasicInfo f = c.getNext();
                res.add(f);
            }
        }
        finally {
            c.close();
        }

        Assert.assertEquals(res.size(), 6, res.toString());
        ArrayList<ScmFileBasicInfo> f1Files = new ArrayList<>();
        ArrayList<ScmFileBasicInfo> f2Files = new ArrayList<>();
        for (ScmFileBasicInfo f : res) {
            if (f.getFileId().get().equals(f1.getFileId().get())) {
                f1Files.add(f);
            }
            else if (f.getFileId().get().equals(f2.getFileId().get())) {
                f2Files.add(f);
            }
            else {
                Assert.fail(f.toString());
            }
        }
        Assert.assertEquals(f1Files.size(), 3, res.toString());
        Assert.assertEquals(f2Files.size(), 3, res.toString());
        List<Integer> versionList1 = new ArrayList<>();
        versionList1.add(1);
        versionList1.add(2);
        versionList1.add(3);
        List<Integer> versionList2 = new ArrayList<>(versionList1);
        for (ScmFileBasicInfo f1File : f1Files) {
            Assert.assertTrue(versionList1.contains(f1File.getMajorVersion()), res.toString());
            Assert.assertTrue(f1File.getMinorVersion() == 0, res.toString());
            versionList1.remove(Integer.valueOf(f1File.getMajorVersion()));
        }
        Assert.assertEquals(versionList1.size(), 0, res.toString());

        for (ScmFileBasicInfo f2File : f2Files) {
            Assert.assertTrue(versionList2.contains(f2File.getMajorVersion()), res.toString());
            Assert.assertTrue(f2File.getMinorVersion() == 0, res.toString());
            versionList2.remove(Integer.valueOf(f2File.getMajorVersion()));
        }
        Assert.assertEquals(versionList2.size(), 0, res.toString());
    }

    private void testCurrentList() throws ScmException {
        ScmCursor<ScmFileBasicInfo> c = ScmFactory.File.listInstance(ws, ScopeType.SCOPE_CURRENT,
                ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR)
                .is(this.getClass().getSimpleName()).get());
        ArrayList<ScmFileBasicInfo> res = new ArrayList<>();
        try {
            while (c.hasNext()) {
                ScmFileBasicInfo f = c.getNext();
                Assert.assertEquals(f.getMajorVersion(), 3);
                Assert.assertEquals(f.getMinorVersion(), 0);
                res.add(f);
            }
        }
        finally {
            c.close();
        }

        if (res.size() != 2) {
            Assert.fail("list current file,expect size 2,but:" + res);
        }

        Assert.assertNotEquals(res.get(0).getFileId().get(), res.get(1).getFileId().get(),
                res.toString());
        Assert.assertEquals(true, res.get(0).getFileId().equals(f1.getFileId())
                || res.get(0).getFileId().equals(f2.getFileId()));
        Assert.assertEquals(true, res.get(1).getFileId().equals(f1.getFileId())
                || res.get(1).getFileId().equals(f2.getFileId()));

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
