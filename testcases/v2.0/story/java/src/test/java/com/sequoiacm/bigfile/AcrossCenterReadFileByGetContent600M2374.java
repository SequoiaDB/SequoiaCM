package com.sequoiacm.bigfile;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * @Description:跨中心读取600文件
 * @author fanyu
 * @Date:2019年02月14日
 * @version:1.0
 */

public class AcrossCenterReadFileByGetContent600M2374 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List<SiteWrapper> branSites = null;
    private final int branSitesNum = 2;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String name = "readCacheFile600M";
    private ScmId fileId = null;
    private int fileSize = 1024 * 1024 * 600;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
        filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
        TestTools.LocalFile.removeFile(localPath);
        TestTools.LocalFile.createDir(localPath.toString());
        TestTools.LocalFile.createFile(filePath, fileSize);
        System.out.println("fileSize = " + new File(filePath).length());
        rootSite = ScmInfo.getRootSite();
        branSites = ScmInfo.getBranchSites(branSitesNum);
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession(branSites.get(0));
        ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
        BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(name).get();
        ScmFileUtils.cleanFile(wsp, cond);
    }

    @Test(groups = {"fourSite"})//SEQUOIACM-415
    private void test() throws Exception {
        this.writeFileFromA();
        this.readFileFromB();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if (runSuccess || forceClear) {
                ScmFactory.File.getInstance(ws, fileId).delete(true);
                TestTools.LocalFile.removeFile(localPath);
            }
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private void writeFileFromA() throws Exception {
        // write scmfile
        fileId = ScmFileUtils.create(ws, name, filePath);
        System.out.println("fileId = " + fileId.get() + ",writeFileFromA file size = " + new File(filePath).length());
        // check results
        SiteWrapper[] expSites = {branSites.get(0)};
        ScmFileUtils.checkMetaAndData(wsp, fileId, expSites, localPath, filePath);
    }

    private void readFileFromB() throws Exception {
        ScmSession session = null;
        try {
            // login
            session = TestScmTools.createSession(branSites.get(1));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
            // read content
            ScmFile scmfile = ScmFactory.File.getInstance(ws, fileId);
            String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId());
            OutputStream fos = new FileOutputStream(new File(downloadPath));
            scmfile.getContent(fos);
            fos.close();
            System.out.println("fileId = " + fileId.get() + "readFileFromB downloadPath size = " + new File(downloadPath).length());

            // check results
            Assert.assertEquals(TestTools.getMD5(filePath), TestTools.getMD5(downloadPath));
            SiteWrapper[] expSites = {rootSite, branSites.get(0), branSites.get(1)};
            ScmFileUtils.checkMetaAndData(wsp, fileId, expSites, localPath, filePath);
        } finally {
            if (session != null)
                session.close();
        }
    }
}