package com.sequoiacm.config.concurrent;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.config.ConfigCommonDefind;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author fanyu
 * @Description:  SCM-2328 ::并发删除配置和创建ws操作业务
 * @Date:2018年12月04日
 * @version:1.0
 */
public class DeleteConf2328 extends TestScmBase {
    private String fileName = "file2328";
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private int fileSize = 1024 * 200;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
        filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
        TestTools.LocalFile.removeFile(localPath);
        TestTools.LocalFile.createDir(localPath.toString());
        TestTools.LocalFile.createFile(filePath, fileSize);
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        ConfUtil.deleteAuditConf(site.getSiteServiceName());
    }

    @Test(groups = {"twoSite", "fourSite"})
    private void test() throws Exception {
        Delete dThread = new Delete();
        CreateWsAndFile cThread = new CreateWsAndFile();
        dThread.start();
        cThread.start();
        Assert.assertEquals(dThread.isSuccess(), true, dThread.getErrorMsg());
        Assert.assertEquals(cThread.isSuccess(), true, cThread.getErrorMsg());

        CreateWsAndFile cThread1 = new CreateWsAndFile();
        Assert.assertEquals(cThread1.isSuccess(), true, cThread1.getErrorMsg());
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException, InterruptedException {
        ConfUtil.deleteAuditConf(site.getSiteServiceName());
        TestTools.LocalFile.removeFile(localPath);
    }

    private class Delete extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            ScmUpdateConfResultSet result = null;
            try {
                session = TestScmTools.createSession(site);
                ScmConfigProperties confProp = ScmConfigProperties.builder()
                        .service(site.getSiteServiceName())
                        .deleteProperty(ConfigCommonDefind.scm_audit_mask)
                        .deleteProperty(ConfigCommonDefind.scm_audit_userMask)
                        .build();
                result = ScmSystem.Configuration.setConfigProperties(session, confProp);

                List<String> list =  new ArrayList<String>();
                list.add(ConfigCommonDefind.scm_audit_mask);
                list.add(ConfigCommonDefind.scm_audit_userMask);

                for(NodeWrapper node : site.getNodes(site.getNodeNum())) {
                    ConfUtil.checkDeletedConf(node.getUrl(),list);
                }
            }catch(ScmException e){
                e.printStackTrace();
                Assert.fail(e.getMessage()+",result = " + result.toString());
            }finally {
                if (session != null) {
                    session.close();
                }
            }
        }
    }

    private class CreateWsAndFile extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            String wsName = "ws2328_" + UUID.randomUUID();
            try {
                session = TestScmTools.createSession(site);
                ScmWorkspaceUtil.createWS(session,wsName,ScmInfo.getSiteNum());
                ScmWorkspaceUtil.wsSetPriority(session,wsName);
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName,session);
                ScmFile file = ScmFactory.File.createInstance(ws);
                file.setFileName(fileName + "_" + UUID.randomUUID());
                file.setContent(filePath);
                ScmId fileId = file.save();
                SiteWrapper[] expSites = {site};
                ScmFileUtils.checkMeta(ws,fileId,expSites);
                ScmFileUtils.checkData(ws,fileId,localPath,filePath);
            }finally {
                ScmWorkspaceUtil.deleteWs(wsName,session);
                if (session != null) {
                    session.close();
                }
            }
        }
    }
}
