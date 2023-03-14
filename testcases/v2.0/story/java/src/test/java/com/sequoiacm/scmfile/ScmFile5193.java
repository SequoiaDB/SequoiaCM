package com.sequoiacm.scmfile;

import java.io.File;
import java.io.IOException;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;

/**
 * @descreption SCM-5193:按文件路径删除文件，文件存在不同站点
 * @author ZhangYanan
 * @date 2022/09/06
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScmFile5193 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "file5193";
    private int fileSize = 1024 * 1024;
    private String filePath1 = null;
    private String filePath2 = null;
    private File localPath = null;
    private BSONObject queryCond = null;

    @BeforeClass()
    private void setUp() throws ScmException, IOException {
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
        branchSite = ScmInfo.getBranchSite();

        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( rootSite );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
    }

    @Test(groups = { GroupTags.twoSite, GroupTags.fourSite })
    public void test() throws Exception {
        // 文件存在单个站点
        ScmFile file1 = ScmFactory.File.createInstance( ws );
        file1.setContent( filePath1 );
        file1.setFileName( fileName );
        file1.setAuthor( fileName );
        file1.save();
        ScmFactory.File.deleteInstanceByPath( ws, "/" + fileName, true );
        try {
            ScmFactory.File.getInstanceByPath( ws, "/" + fileName );
            Assert.fail( "the file expected not exist" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.FILE_NOT_FOUND.getErrorCode() ) {
                throw e;
            }
        }

        // 文件存在多个站点
        ScmFile file2 = ScmFactory.File.createInstance( ws );
        file2.setContent( filePath2 );
        file2.setFileName( fileName );
        file2.setAuthor( fileName );
        file2.save();

        ScmId taskId = ScmSystem.Task.startTransferTask( ws, queryCond,
                ScmType.ScopeType.SCOPE_CURRENT, branchSite.getSiteName() );
        ScmTaskUtils.waitTaskStop( session, taskId );

        ScmFactory.File.deleteInstanceByPath( ws, "/" + fileName, true );
        try {
            ScmFactory.File.getInstanceByPath( ws, "/" + fileName );
            Assert.fail( "the file expected not exist" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.FILE_NOT_FOUND.getErrorCode() ) {
                throw e;
            }
        }
        runSuccess = true;
    }

    @AfterClass()
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}