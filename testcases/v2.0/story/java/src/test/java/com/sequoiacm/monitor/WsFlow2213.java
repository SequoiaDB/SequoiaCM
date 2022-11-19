package com.sequoiacm.monitor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.UUID;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmFlow;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description: SCM-2213:删除工作区,新增同名工作区,获取工作区的上传下载流量
 * @author fanyu
 * @Date:2018年9月11日
 * @version:1.0
 */
public class WsFlow2213 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 0;
    private String wsName = "ws2213";
    private String uploadKey = "uploadFlow";
    private String downloadKey = "downloadFlow";
    private String name = "WsFlow2213";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getRootSite();
        try {
            localPath = new File( TestScmBase.dataDirectory + File.separator
                    + TestTools.getClassName() );
            filePath = localPath + File.separator + "localFile_" + fileSize
                    + ".txt";
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );
            session = TestScmTools.createSession( site );
            ScmWorkspaceUtil.deleteWs( wsName, session );
            ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
            ScmWorkspaceUtil.wsSetPriority( session, wsName );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    // 问题单SEQUOIACM-1149未修改暂时屏蔽用例
    @Test(groups = { "fourSite" }, enabled = false)
    private void test() throws Exception {
        // upload
        ScmWorkspace ws1 = ScmFactory.Workspace.getWorkspace( wsName, session );
        ScmFileUtils.create( ws1, name + "_" + UUID.randomUUID(), filePath );
        BasicBSONObject ws2213_flow_before = getFlowByWsName( wsName );

        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createWS( session, wsName, 2 );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ScmWorkspace ws2 = ScmFactory.Workspace.getWorkspace( wsName, session );
        ScmId fileId2 = ScmFileUtils.create( ws2,
                name + "_" + UUID.randomUUID(), filePath );
        downloadFile( ws2, fileId2 );
        BasicBSONObject ws2213_flow_after = getFlowByWsName( wsName );

        // check
        Assert.assertEquals( ws2213_flow_after.getLong( uploadKey )
                - ws2213_flow_before.getLong( uploadKey ), fileSize );
        Assert.assertEquals( ws2213_flow_after.getLong( downloadKey )
                - ws2213_flow_before.getLong( downloadKey ), fileSize );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, session );
        if ( session != null ) {
            session.close();
        }
    }

    private BasicBSONObject getFlowByWsName( String wsName ) {
        ScmCursor< ScmFlow > info = null;
        BasicBSONObject obj = null;
        try {
            info = ScmSystem.Monitor.showFlow( session );
            while ( info.hasNext() ) {
                ScmFlow flow = info.getNext();
                if ( flow.getWorkspaceName().equals( wsName ) ) {
                    obj = new BasicBSONObject();
                    obj.put( uploadKey, flow.getUploadFlow() );
                    obj.put( downloadKey, flow.getDownloadFlow() );
                }
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
        } finally {
            if ( info != null ) {
                info.close();
            }
        }
        return obj;
    }

    private void downloadFile( ScmWorkspace ws, ScmId fileId )
            throws Exception {
        ScmFile file;
        OutputStream fileOutputStream = null;
        try {
            file = ScmFactory.File.getInstance( ws, fileId );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            fileOutputStream = new FileOutputStream( new File( downloadPath ) );
            file.getContent( fileOutputStream );
            fileOutputStream.close();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileOutputStream != null ) {
                fileOutputStream.close();
            }
        }
    }
}
