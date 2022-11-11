package com.sequoiacm.scmfile;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.sequoiacm.exception.ScmError;
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
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @descreption SCM-5362:指定空FileId
 * @author ZhangYanan
 * @date 2022/11/02
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScmFile5362 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "file5362";
    private int fileSize = 1024 * 1024;
    private String filePath = null;
    private File localPath = null;
    private ScmId fileID1 = null;
    private ScmId fileID2 = null;
    private BSONObject queryCond = null;

    @BeforeClass()
    private void setUp() throws ScmException, IOException, ParseException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile1_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        rootSite = ScmInfo.getRootSite();

        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( rootSite );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );

    }

    @Test(groups = { GroupTags.oneSite, GroupTags.twoSite, GroupTags.fourSite })
    public void test() throws Exception {
        // a、fileId 为空字符串
        try {
            fileID2 = new ScmId( "" );
            Assert.fail( "预期失败实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.INVALID_ID.getErrorCode() ) {
                throw e;
            }
        }

        // b、fileId为null
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setContent( filePath );
            file.setFileName( fileName );
            file.setAuthor( fileName );
            file.setFileId( fileID1 );
            file.save();
            Assert.fail( "预期失败实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.INVALID_ARGUMENT.getErrorCode() ) {
                throw e;
            }
        }
        runSuccess = true;
    }

    @AfterClass()
    private void tearDown(){
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