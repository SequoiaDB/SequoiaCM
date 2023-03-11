package com.sequoiacm.workspace;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @descreption SCM-5491:修改数据源分区规则与断点文件上传验证
 * @author ZhangYanan
 * @date 2022/11/29
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5491 extends TestScmBase {
    private ScmSession session = null;
    private SiteWrapper rootSite = null;
    private String wsName = "ws5491";
    private String fileName1 = "breakpointFile5491_1";
    private String fileName2 = "breakpointFile5491_2";
    private ArrayList< SiteWrapper > siteList = new ArrayList<>();
    private int fileSize = 1024 * 1024 * 10;
    private int partSize = 1024 * 1024 * 5;
    private String filePath1 = null;
    private String filePath2 = null;
    private File localPath = null;
    private boolean runSuccess = false;
    private ScmWorkspace ws;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath1 = localPath + File.separator + "localFile1_" + fileSize
                + ".txt";
        filePath2 = localPath + File.separator + "localFile2_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath1, fileSize );
        TestTools.LocalFile.createFile( filePath2, fileSize );

        rootSite = ScmInfo.getRootSite();
        session = TestScmTools.createSession( rootSite );
        siteList.add( rootSite );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test() throws Exception {
        createBreakpointFile( fileName1, filePath1 );
        List< ScmDataLocation > dataLocation = ScmWorkspaceUtil
                .prepareWsDataLocation( siteList, ScmShardingType.YEAR );
        ws.updateDataLocation( dataLocation );
        ScmWorkspaceUtil.checkWsUpdate( session, wsName, dataLocation );

        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName1 );
        breakpointFile.upload( new File( filePath1 ) );
        checkFileData( breakpointFile, filePath1 );

        createBreakpointFile( fileName2, filePath2 );
        breakpointFile = ScmFactory.BreakpointFile.getInstance( ws, fileName2 );
        breakpointFile.upload( new File( filePath2 ) );
        checkFileData( breakpointFile, filePath2 );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            ScmWorkspaceUtil.deleteWs( wsName, session );
            if ( session != null ) {
                session.close();
            }
        }
    }

    public void createBreakpointFile( String fileName, String filePath )
            throws ScmException, IOException {
        BreakpointUtil.createBreakpointFile( ws, filePath, fileName, partSize,
                ScmChecksumType.CRC32 );

    }

    private void checkFileData( ScmBreakpointFile breakpointFile,
            String filePath ) throws Exception {
        // save to file, than down file check the file data
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( breakpointFile );
        file.setFileName( "fileName5491" );
        file.setAuthor( "fileAuthor5491" );
        file.save();

        // down file
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        file.getContent( downloadPath );

        // check results
        Assert.assertEquals( TestTools.getMD5( filePath ),
                TestTools.getMD5( downloadPath ) );
        TestTools.LocalFile.removeFile( downloadPath );
        file.delete( true );
    }
}