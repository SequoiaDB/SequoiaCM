package com.sequoiacm.workspace;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @descreption SCM-5591:修改数据源分区规则后删除多版本文件
 * @author ZhangYanan
 * @date 2023/06/08
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5591 extends TestScmBase {
    private ScmSession rootSession = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branSite = null;
    private String wsName = "ws5591";
    private String fileName1 = "file5591_1";
    private String fileName2 = "file5591_2";
    private ArrayList< SiteWrapper > siteList = new ArrayList<>();
    private int fileSize = 1024 * 1024;
    private String filePath1 = null;
    private String filePath2 = null;
    private File localPath = null;
    private ArrayList< ScmId > fileIdList = new ArrayList<>();
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

        rootSession = ScmSessionUtils.createSession( rootSite );
        siteList.add( rootSite );

        ScmWorkspaceUtil.deleteWs( wsName, rootSession );
        ScmWorkspaceUtil.createWS( rootSession, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( rootSession, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, rootSession );
    }

    @Test
    public void test() throws Exception {
        SiteWrapper[] expSites = { rootSite };
        ScmId fileId = createFile( fileName1, filePath1, filePath2 );
        fileIdList.add( fileId );

        List< ScmDataLocation > dataLocation = ScmWorkspaceUtil
                .prepareWsDataLocation( siteList, ScmShardingType.QUARTER );
        ws.updateDataLocation( dataLocation );
        ScmWorkspaceUtil.checkWsUpdate( rootSession, wsName, dataLocation );

        ScmFileUtils.checkHistoryFileMetaAndData( wsName, fileIdList, expSites,
                localPath, filePath1, 1, 0 );
        ScmFileUtils.checkHistoryFileMetaAndData( wsName, fileIdList, expSites,
                localPath, filePath2, 2, 0 );
        deleteFileAndCheck( fileId );
        fileIdList.clear();

        fileId = createFile( fileName1, filePath2, filePath1 );
        fileIdList.add( fileId );
        ScmFileUtils.checkHistoryFileMetaAndData( wsName, fileIdList, expSites,
                localPath, filePath2, 1, 0 );
        ScmFileUtils.checkHistoryFileMetaAndData( wsName, fileIdList, expSites,
                localPath, filePath1, 2, 0 );
        deleteFileAndCheck( fileId );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            ScmWorkspaceUtil.deleteWs( wsName, rootSession );
            if ( rootSession != null ) {
                rootSession.close();
            }
        }
    }

    public ScmId createFile( String fileName, String filePath1,
            String filePath2 ) throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        file.setContent( filePath1 );
        ScmId scmId = file.save();
        file.updateContent( filePath2 );
        return scmId;
    }

    public void deleteFileAndCheck( ScmId scmId ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, scmId );
        file.delete( true );
        try {
            ScmFactory.File.getInstance( ws, scmId );
            Assert.fail( "file should be deleted" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.FILE_NOT_FOUND.getErrorCode() ) {
                throw e;
            }
        }
    }
}