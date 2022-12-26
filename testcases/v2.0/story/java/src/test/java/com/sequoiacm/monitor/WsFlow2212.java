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
 * @Description:SCM-2212 :: 多个工作区对应不同的站点,获取工作区域的上传下载流量
 * @author fanyu
 * @Date:2018年9月11日
 * @version:1.0
 */
public class WsFlow2212 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 100;
    private String name = "WsFlow2212";
    private String[] wsNames = new String[] { "ws2212A", "ws2212B", "ws2212C" };
    private String uploadKey = "uploadFlow";
    private String downloadKey = "downloadFlow";

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
            ScmWorkspaceUtil.deleteWs( "ws2", session );
            ScmWorkspaceUtil.deleteWs( "ws3", session );

            for ( String wsName : wsNames ) {
                ScmWorkspaceUtil.deleteWs( wsName, session );
            }
            ScmWorkspaceUtil.createWS( session, wsNames[ 0 ], 1 );
            ScmWorkspaceUtil.createWS( session, wsNames[ 1 ], 2 );
            ScmWorkspaceUtil.createWS( session, wsNames[ 2 ],
                    ScmInfo.getSiteNum() );

            for ( String wsName : wsNames ) {
                ScmWorkspaceUtil.wsSetPriority( session, wsName );
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        // do nothing
        Assert.assertEquals( getFlowByWsName( wsNames[ 0 ] ), null );
        Assert.assertEquals( getFlowByWsName( wsNames[ 1 ] ), null );
        Assert.assertEquals( getFlowByWsName( wsNames[ 2 ] ), null );

        // upload file in wsNames[0] in rootsite
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsNames[ 0 ],
                session );
        ScmId fileId = ScmFileUtils.create( ws, name + "_" + UUID.randomUUID(),
                filePath );
        BasicBSONObject ws1_flow = getFlowByWsName( wsNames[ 0 ] );
        // check
        Assert.assertEquals( ws1_flow.getLong( uploadKey ), fileSize );
        Assert.assertEquals( ws1_flow.getLong( downloadKey ), 0 );
        Assert.assertEquals( getFlowByWsName( wsNames[ 1 ] ), null );
        Assert.assertEquals( getFlowByWsName( wsNames[ 2 ] ), null );

        // upload file in wsNames[2] in branchsite
        SiteWrapper siteA = ScmInfo.getBranchSite();
        ScmSession sessionA = TestScmTools.createSession( siteA );
        ScmWorkspace ws2 = ScmFactory.Workspace.getWorkspace( wsNames[ 2 ],
                sessionA );
        ScmId fileId1 = ScmFileUtils.create( ws2,
                name + "_" + UUID.randomUUID(), filePath );
        // check
        BasicBSONObject ws3_flow = getFlowByWsName( wsNames[ 2 ] );
        Assert.assertEquals( ws3_flow.getLong( uploadKey ), fileSize );
        Assert.assertEquals( ws3_flow.getLong( downloadKey ), 0 );
        ws1_flow = getFlowByWsName( wsNames[ 0 ] );
        Assert.assertEquals( ws1_flow.getLong( uploadKey ), fileSize );
        Assert.assertEquals( ws1_flow.getLong( downloadKey ), 0 );
        Assert.assertEquals( getFlowByWsName( wsNames[ 1 ] ), null );

        // upload file and download file in wsNames[1] in rootsite
        ScmWorkspace ws1 = ScmFactory.Workspace.getWorkspace( wsNames[ 1 ],
                session );
        ScmId fileId2 = ScmFileUtils.create( ws1,
                name + "_" + UUID.randomUUID(), filePath );
        downloadFile( ws1, fileId2 );
        // check
        BasicBSONObject ws2_flow = getFlowByWsName( wsNames[ 1 ] );
        Assert.assertEquals( ws2_flow.getLong( uploadKey ), fileSize );
        Assert.assertEquals( ws2_flow.getLong( downloadKey ), fileSize );
        ws3_flow = getFlowByWsName( wsNames[ 2 ] );
        Assert.assertEquals( ws3_flow.getLong( uploadKey ), fileSize );
        Assert.assertEquals( ws3_flow.getLong( downloadKey ), 0 );
        ws1_flow = getFlowByWsName( wsNames[ 0 ] );
        Assert.assertEquals( ws1_flow.getLong( uploadKey ), fileSize );
        Assert.assertEquals( ws1_flow.getLong( downloadKey ), 0 );

        // upload file and download file in all workspaces
        downloadFile( ws, fileId );
        downloadFile( ws2, fileId1 );
        // check
        ws2_flow = getFlowByWsName( wsNames[ 1 ] );
        Assert.assertEquals( ws2_flow.getLong( uploadKey ), fileSize );
        Assert.assertEquals( ws2_flow.getLong( downloadKey ), fileSize );
        ws3_flow = getFlowByWsName( wsNames[ 2 ] );
        Assert.assertEquals( ws3_flow.getLong( uploadKey ), fileSize );
        Assert.assertEquals( ws3_flow.getLong( downloadKey ), fileSize );
        ws1_flow = getFlowByWsName( wsNames[ 0 ] );
        Assert.assertEquals( ws1_flow.getLong( uploadKey ), fileSize );
        Assert.assertEquals( ws1_flow.getLong( downloadKey ), fileSize );
        sessionA.close();
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        for ( String wsName : wsNames ) {
            ScmWorkspaceUtil.deleteWs( wsName, session );
        }
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
