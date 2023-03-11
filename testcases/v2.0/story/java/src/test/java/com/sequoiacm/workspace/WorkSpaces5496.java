package com.sequoiacm.workspace;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @descreption SCM-5496:修改数据源分区规则与桶内文件删除验证
 * @author ZhangYanan
 * @date 2022/11/29
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5496 extends TestScmBase {
    private ScmSession session = null;
    private SiteWrapper site = null;
    private String wsName = "ws5496";
    private String key1 = "key5496a";
    private String key2 = "key5496b";
    private String bucketName = "bucket5496";
    private ScmBucket bucket = null;
    private ArrayList< SiteWrapper > siteList = new ArrayList<>();
    private int fileSize = 1024 * 1024 * 10;
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

        site = ScmInfo.getRootSite();
        session = TestScmTools.createSession( site );
        siteList.add( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createDisEnableDirectoryWS( session, wsName,
                ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test() throws Exception {
        ScmFile file = bucket.createFile( key1 );
        file.setContent( filePath1 );
        file.save();

        List< ScmDataLocation > dataLocation = ScmWorkspaceUtil
                .prepareWsDataLocation( siteList, ScmShardingType.YEAR );
        ws.updateDataLocation( dataLocation );
        ScmWorkspaceUtil.checkWsUpdate( session, wsName, dataLocation );

        checkFileDelete( key1 );

        file = bucket.createFile( key2 );
        file.setContent( filePath2 );
        file.save();
        checkFileDelete( key2 );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Bucket.deleteBucket( session, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            ScmWorkspaceUtil.deleteWs( wsName, session );
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void checkFileDelete( String key ) throws Exception {
        ScmFile file = bucket.getFile( key );
        file.delete( true );
        try {
            bucket.getFile( key );
            Assert.fail( "预期失败，实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.FILE_NOT_FOUND.getErrorCode() ) {
                throw e;
            }
        }
    }
}