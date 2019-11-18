package com.sequoiacm.net.asynctask;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-487:分中心不存在但主中心存在
 * @Author fanyu
 * @Date 2017-06-28
 * @Version 1.00
 */

/*
 * 1、在分中心B异步迁移单个文件； 2、检查执行结果正确性；
 */
public class AsyncTransfer_inRootSite487 extends TestScmBase {

    private boolean runSuccess = false;
    private int fileSize = 100;
    private File localPath = null;
    private String filePath = null;
    private ScmId fileId = null;
    private String fileName = "AsyncTransferWhenMExistFile487";
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    private SiteWrapper sourceSite = null;
    private SiteWrapper targetSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {

        localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
        filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";

        try {
            // ready file
            TestTools.LocalFile.removeFile(localPath);
            TestTools.LocalFile.createDir(localPath.toString());
            TestTools.LocalFile.createFile(filePath, fileSize);

            ws_T = ScmInfo.getWs();
			List<SiteWrapper> siteList = ScmNetUtils.getSortSites(ws_T);
			sourceSite = siteList.get(0);
			targetSite = siteList.get(1); 

            BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
            ScmFileUtils.cleanFile(ws_T,cond);

            // login in
            session = TestScmTools.createSession(sourceSite);
            ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), session);

            writeFileFromMainCenter(ws, filePath);
        } catch (ScmException | IOException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        try {
            asyncTransferFromSubCenterA();
            checkResult();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if (runSuccess || forceClear) {
                ScmFactory.File.deleteInstance(ws, fileId, true);
                TestTools.LocalFile.removeFile(localPath);
            }
        } catch (BaseException | ScmException e) {
            Assert.fail(e.getMessage());
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private void writeFileFromMainCenter(ScmWorkspace ws, String filePath) {
        try {
            ScmFile scmfile = ScmFactory.File.createInstance(ws);
            scmfile.setContent(filePath);
            scmfile.setFileName(fileName+"_"+UUID.randomUUID());
            fileId = scmfile.save();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    private void asyncTransferFromSubCenterA() throws ScmException {
        ScmWorkspace wsB = ScmFactory.Workspace.getWorkspace(ws_T.getName(), session);
        ScmFactory.File.asyncTransfer(wsB, fileId);
    }

    private void checkResult() {
        try {
            SiteWrapper[] expSiteList = { sourceSite, targetSite };
            ScmTaskUtils.waitAsyncTaskFinished(ws, fileId, expSiteList.length);
            ScmFileUtils.checkMetaAndData(ws_T,fileId, expSiteList, localPath, filePath);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
