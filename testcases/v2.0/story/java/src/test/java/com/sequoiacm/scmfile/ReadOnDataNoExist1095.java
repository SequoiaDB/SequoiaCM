
package com.sequoiacm.scmfile;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.exception.BaseException;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description: SCM-1095:部分中心元数据残留,直连残留元数据中心读取文件
 * @author fanyu
 * @Date:2018年2月6日
 * @version:1.0
 */
public class ReadOnDataNoExist1095 extends TestScmBase {
    private boolean runSuccess = false;
    private List<SiteWrapper> siteList = null;
    private SiteWrapper rootSite = null;
    private WsWrapper wsp = null;
    private ScmSession sessionA = null;
    private ScmSession sessionB = null;

    private String fileNameBase = "ReadOnDataNoExist1095";
    private List<ScmId> fileIdList = new ArrayList<ScmId>();
    private int fileSize = 1024 * 200;
    private int fileNum = 4;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
        filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
        TestTools.LocalFile.removeFile(localPath);
        TestTools.LocalFile.createDir(localPath.toString());
        TestTools.LocalFile.createFile(filePath, fileSize);

        rootSite = ScmInfo.getRootSite();
        siteList = ScmInfo.getBranchSites(2);
        wsp = ScmInfo.getWs();
        sessionA = TestScmTools.createSession(siteList.get(0));
        sessionB = TestScmTools.createSession(siteList.get(1));
        ScmWorkspace wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);
        ScmWorkspace wsB = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionB);

        // write in A
        for (int i = 0; i < fileNum; i++) {
            BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileNameBase + "_" + i)
                    .get();
            ScmFileUtils.cleanFile(wsp, cond);
            fileIdList.add(ScmFileUtils.create(wsA, fileNameBase + "_" + i, filePath));
        }
        // read in B
        for (ScmId fileId : fileIdList) {
            read(wsB, fileId);
        }
    }

    @Test(groups = {"fourSite"})
    private void test() throws Exception {
        // delete A's data, A read
        List<SiteWrapper> siteList1 = siteList.subList(0, 1);
        deleteDataAndRead(siteList1, siteList.get(0), wsp, fileIdList.get(0));

        // delete A and M data, A read
        List<SiteWrapper> siteList2 = new ArrayList<SiteWrapper>();
        siteList2.add(siteList.get(0));
        siteList2.add(rootSite);
        deleteDataAndRead(siteList2, siteList.get(0), wsp, fileIdList.get(1));

        // delete A and M data, M read
        deleteDataAndRead(siteList2, rootSite, wsp, fileIdList.get(2));

        // delete A/B/M data,M read
        List<SiteWrapper> siteList3 = new ArrayList<SiteWrapper>();
        siteList3.addAll(siteList);
        siteList3.add(rootSite);
        deleteDataAndRead(siteList3, rootSite, wsp, fileIdList.get(3));
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if (runSuccess || TestScmBase.forceClear) {
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);
                for (ScmId fileId : fileIdList) {
                    ScmFactory.File.deleteInstance(ws, fileId, true);
                }
                TestTools.LocalFile.removeFile(localPath);
            }
        } finally {
            if (sessionA != null) {
                sessionA.close();
            }
            if (sessionB != null) {
                sessionB.close();
            }
        }
    }

    private void read(ScmWorkspace ws, ScmId fileId) throws Exception {
        try {
            ScmFile file = ScmFactory.File.getInstance(ws, fileId);
            String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId());
            file.getContent(downloadPath);
            // check content
            Assert.assertEquals(TestTools.getMD5(filePath), TestTools.getMD5(downloadPath));
        } catch (ScmException e) {
            if (e.getError() != ScmError.DATA_ERROR) {
                throw e;
            }
        }
    }

    private void deleteDataAndRead(List<SiteWrapper> siteList, SiteWrapper site, WsWrapper wsp, ScmId fileId)
            throws Exception {
        for (SiteWrapper dsite : siteList) {
            try {
                TestSdbTools.Lob.removeLob(dsite, wsp, fileId);
            } catch (BaseException e) {
                //for start model and net model
                if (e.getErrorCode() != -4) {
                    e.printStackTrace();
                    Assert.fail(e.getMessage());
                }
            }
        }
        ScmSession session = null;
        try {
            session = TestScmTools.createSession(site);
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
            read(ws, fileId);
        } catch (ScmException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
}
