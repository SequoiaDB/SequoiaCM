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
 * @descreption SCM-5364:指定FileId创建文件，FileId携带时间信息
 * @author ZhangYanan
 * @date 2022/11/02
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScmFile5364 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "file5364";
    private int fileSize = 1024 * 1024;
    private String filePath = null;
    private File localPath = null;
    private ScmId fileID1 = null;
    private ScmId fileID2 = null;
    private ScmId fileID3 = null;
    private Date date1 = null;
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
        session = ScmSessionUtils.createSession( rootSite );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        prepareFileId();
    }

    @Test(groups = { GroupTags.oneSite, GroupTags.twoSite, GroupTags.fourSite })
    public void test() throws Exception {
        //场景a：创建文件指定create_time与FileId时间一致
        test1();
        //场景b：创建文件指定create_time与FileId时间不一致
        test2();
        //场景c：创建文件不指定create_time
        test3();
        runSuccess = true;
    }

    @AfterClass()
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    public void prepareFileId() throws ScmException, ParseException {
        date1 = new Date();
        fileID1 = new ScmId( ScmFileUtils.getFileIdByDate( date1 ) );
        DateFormat dateFormat1 = new SimpleDateFormat( "yyyy-MM-dd" );
        fileID2 = new ScmId( ScmFileUtils
                .getFileIdByDate( dateFormat1.parse( "2022-11-01" ) ) );
        DateFormat dateFormat2 = new SimpleDateFormat( "yyyy-MM-dd" );
        fileID3 = new ScmId( ScmFileUtils
                .getFileIdByDate( dateFormat2.parse( "2021-11-01" ) ) );
    }

    public void test1() throws Exception {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        file.setFileId( fileID1 );
        file.setCreateTime( date1 );
        ScmId scmId = file.save();

        Assert.assertEquals( fileID1, scmId,
                "文件id与指定的fileId不一致，指定的id为:" + fileID1 + " ;实际文件id为:" + scmId );

        SiteWrapper[] expSites = { rootSite };
        ScmFileUtils.checkMetaAndData( wsp, fileID1, expSites, localPath,
                filePath );
        Date createTime = file.getCreateTime();
        Assert.assertEquals( date1, createTime, "文件创建时间与指定的创建时间不一致，指定的时间为:"
                + date1 + " ;实际文件创建时间为:" + createTime );

        ScmFileUtils.cleanFile( wsp, queryCond );
    }

    public void test2() throws Exception {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        file.setFileId( fileID2 );
        file.setCreateTime( new Date() );
        try {
            file.save();
            Assert.fail( "预期失败实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.INVALID_ARGUMENT
                    .getErrorCode() ) {
                throw e;
            }
        }

    }

    public void test3() throws Exception {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        file.setFileId( fileID3 );
        ScmId scmId = file.save();

        Assert.assertEquals( fileID3, scmId,
                "文件id与指定的fileId不一致，指定的id为:" + fileID3 + " ;实际文件id为:" + scmId );

        SiteWrapper[] expSites = { rootSite };
        ScmFileUtils.checkMetaAndData( wsp, fileID3, expSites, localPath,
                filePath );

        ScmFileUtils.cleanFile( wsp, queryCond );
    }
}