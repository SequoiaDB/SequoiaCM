package com.sequoiacm.breakpointfile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * 创建普通文件，断点文件时，调用setCreateTime接口，验证接口参数合法值和非法值：
 * 
 * @author yangjianbo
 * @Description:
 * @Date: 2023年02月23日
 * @version:1.0
 */
public class SetCreateTimeByFileAndBreakpointFile1816 extends TestScmBase {
    private static WsWrapper wsp = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private File localPath = null;
    private String filePath = null;
    private String fileAuthor = "file1816";
    private BSONObject queryCond;
    private boolean runTestFileSuccess = false;
    private String fileName = "file1816_";
    private int fileSize = 1024 * 100;
    private byte[] filedata = new byte[ fileSize ];
    private long acceptableOffset = 180 * 1000; // 3m
    private final static Long CREATE_TIME_MAX_VALUE = 253339200000L * 1000L; // 253339200000s

    @BeforeClass
    private void setUp() throws IOException, ScmException {

        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        List< SiteWrapper > DBSites = ScmBreakpointFileUtils
                .checkDBAndCephS3DataSource();
        site = DBSites.get( new Random().nextInt( DBSites.size() ) );
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
    }

    // 问题单SEQUOIACM-1321未修改，用例暂时屏蔽
    @Test(groups = { "twoSite", "fourSite" }, enabled = false)
    private void test() throws Exception {
        testFileSetCreateTime();
        runTestFileSuccess = true;
        testBreakpointFileSetCreateTime();
    }

    private void testFileSetCreateTime() throws ScmException, IOException {
        // Date current time
        Date date = new Date();
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setCreateTime( date );
        file.setFileName( fileName + "fileSetCreateTime" + date.getTime() );
        file.setContent( filePath );
        file.setAuthor( fileAuthor );
        ScmId scmId = file.save();
        Date createTime = ScmFactory.File.getInstance( ws, scmId )
                .getCreateTime();
        Assert.assertEquals( createTime.getTime(), date.getTime() );

        // null
        date = new Date();
        file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName + "fileSetCreateTime" + date.getTime() );
        file.setContent( filePath );
        file.setCreateTime( null );
        file.setAuthor( fileAuthor );
        scmId = file.save();
        createTime = ScmFactory.File.getInstance( ws, scmId ).getCreateTime();
        Assert.assertTrue( Math.abs(
                createTime.getTime() - date.getTime() ) < acceptableOffset );

        // Date new Date(0) 1970
        date = new Date( 0 );
        file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName + "fileSetCreateTime" + date.getTime() );
        file.setContent( filePath );
        file.setCreateTime( date );
        file.setAuthor( fileAuthor );
        scmId = file.save();
        createTime = ScmFactory.File.getInstance( ws, scmId ).getCreateTime();
        Assert.assertEquals( createTime.getTime(), date.getTime() );

        // SEQUOIACM-1303 还在处理
        // Date new Date(-1) 1970 以前
        date = new Date( -1 );
        file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName + "fileSetCreateTime" + date.getTime() );
        file.setContent( filePath );
        file.setCreateTime( date );
        file.setAuthor( fileAuthor );
        scmId = file.save();
        createTime = ScmFactory.File.getInstance( ws, scmId ).getCreateTime();
        Assert.assertEquals( createTime.getTime(), date.getTime() );

        // 9223372036854775807 ms (Long.MAX_VALUE)
        date = new Date( Long.MAX_VALUE );
        file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName + "fileSetCreateTime" + date.getTime() );
        file.setContent( filePath );
        file.setCreateTime( date );
        file.setAuthor( fileAuthor );
        try {
            file.save();
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_INTERNAL_SERVER_ERROR ) {
                throw e;
            }
        }

        // SEQUOIACM-1305
        // -9223372036854775808 ms (Long.MIN_VALUE)
        date = new Date( Long.MIN_VALUE );
        file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName + "fileSetCreateTime" + date.getTime() );
        file.setContent( filePath );
        file.setCreateTime( date );
        file.setAuthor( fileAuthor );
        try {
            file.save();
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_INTERNAL_SERVER_ERROR ) {
                throw e;
            }
        }

        // 2147483647 ms (Integer.MAX_VALUE)
        date = new Date( Integer.MAX_VALUE );
        file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName + "fileSetCreateTime" + date.getTime() );
        file.setContent( filePath );
        file.setCreateTime( date );
        file.setAuthor( fileAuthor );
        scmId = file.save();
        createTime = ScmFactory.File.getInstance( ws, scmId ).getCreateTime();
        Assert.assertEquals( createTime.getTime(), date.getTime() );

        // SEQUOIACM-1303 还在处理
        // -2147483647 ms(Integer.MIN_VALUE)
        date = new Date( Integer.MIN_VALUE );
        file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName + "fileSetCreateTime" + date.getTime() );
        file.setContent( filePath );
        file.setCreateTime( date );
        file.setAuthor( fileAuthor );
        scmId = file.save();
        createTime = ScmFactory.File.getInstance( ws, scmId ).getCreateTime();
        Assert.assertEquals( createTime.getTime(), date.getTime() );

        // 25333920000 s
        date = new Date( CREATE_TIME_MAX_VALUE );
        file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName + "fileSetCreateTime" + date.getTime() );
        file.setContent( filePath );
        file.setCreateTime( date );
        file.setAuthor( fileAuthor );
        scmId = file.save();
        createTime = ScmFactory.File.getInstance( ws, scmId ).getCreateTime();
        Assert.assertEquals( createTime.getTime(), date.getTime() );

        // SEQUOIACM-1303 开发还需与前线讨论是否对最小值限定
        // -25333920001 s
        date = new Date( CREATE_TIME_MAX_VALUE * -1L + 1000L );
        file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName + "fileSetCreateTime" + date.getTime() );
        file.setContent( filePath );
        file.setCreateTime( date );
        file.setAuthor( fileAuthor );
        try {
            file.save();
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_INTERNAL_SERVER_ERROR ) {
                throw e;
            }
        }

        // 25333920001 s
        date = new Date( CREATE_TIME_MAX_VALUE + 1000L );
        file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName + "fileSetCreateTime" + date.getTime() );
        file.setContent( filePath );
        file.setCreateTime( date );
        file.setAuthor( fileAuthor );
        try {
            file.save();
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_INTERNAL_SERVER_ERROR ) {
                throw e;
            }
        }

        // SEQUOIACM-1303 开发还需与前线讨论是否对最小值限定
        // -25333920002 s
        date = new Date( CREATE_TIME_MAX_VALUE * -1L - 2000L );
        file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName + "fileSetCreateTime" + date.getTime() );
        file.setContent( filePath );
        file.setCreateTime( date );
        file.setAuthor( fileAuthor );
        try {
            file.save();
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_INTERNAL_SERVER_ERROR ) {
                throw e;
            }
        }

    }

    private void testBreakpointFileSetCreateTime() throws ScmException {

        // Date current time
        Date date = new Date();
        String name = fileName + "breakpointFileSetCreateTime" + date.getTime();
        createBreakpointFile( name, filedata, date );
        checkBreakpointResult( date, name );

        // date null
        // SEQUOIACM-1321 在处理
        date = null;
        name = fileName + "breakpointFileSetCreateTime_null";
        createBreakpointFile( name, filedata, date );
        checkBreakpointResult( date, name );

        // 已经提单：SEQUOIACM-1305
        // Date(0) 1970
        date = new Date( 0 );
        name = fileName + "breakpointFileSetCreateTime" + date.getTime();
        createBreakpointFile( name, filedata, date );
        // checkBreakpointResult( date, name );

        // Date new Date(-1)
        date = new Date( -1 );
        name = fileName + "breakpointFileSetCreateTime" + date.getTime();
        createBreakpointFile( name, filedata, date );
        checkBreakpointResult( date, name );

        // 9223372036854775807 ms
        date = new Date( Long.MAX_VALUE );
        name = fileName + "breakpointFileSetCreateTime" + date.getTime();
        try {
            createBreakpointFile( name, filedata, date );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_INTERNAL_SERVER_ERROR ) {
                throw e;
            }
        }

        // SEQUOIACM-1305
        // -9223372036854775808 ms
        date = new Date( Long.MIN_VALUE );
        name = fileName + "breakpointFileSetCreateTime" + date.getTime();
        try {
            createBreakpointFile( name, filedata, date );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_INTERNAL_SERVER_ERROR ) {
                throw e;
            }
        }

        // 2147483647 ms
        date = new Date( Integer.MAX_VALUE );
        name = fileName + "breakpointFileSetCreateTime" + date.getTime();
        createBreakpointFile( name, filedata, date );
        checkBreakpointResult( date, name );

        // -2147483648 ms
        date = new Date( Integer.MIN_VALUE );
        name = fileName + "breakpointFileSetCreateTime" + date.getTime();
        createBreakpointFile( name, filedata, date );
        checkBreakpointResult( date, name );

        // 253339200000 s
        date = new Date( CREATE_TIME_MAX_VALUE );
        name = fileName + "breakpointFileSetCreateTime" + date.getTime();
        createBreakpointFile( name, filedata, date );
        checkBreakpointResult( date, name );

        // -253339200001 s
        date = new Date( CREATE_TIME_MAX_VALUE * -1L - 1000L );
        name = fileName + "breakpointFileSetCreateTime" + date.getTime();
        try {
            createBreakpointFile( name, filedata, date );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_INTERNAL_SERVER_ERROR ) {
                throw e;
            }
        }

        // 253339200001 s
        date = new Date( CREATE_TIME_MAX_VALUE + 1000L );
        name = fileName + "breakpointFileSetCreateTime" + date.getTime();
        try {
            createBreakpointFile( name, filedata, date );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_INTERNAL_SERVER_ERROR ) {
                throw e;
            }
        }

        // -253339200002 s
        date = new Date( CREATE_TIME_MAX_VALUE * -1L - 2000L );
        name = fileName + "breakpointFileSetCreateTime" + date.getTime();
        try {
            createBreakpointFile( name, filedata, date );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_INTERNAL_SERVER_ERROR ) {
                throw e;
            }
        }

    }

    private void checkBreakpointResult( Date date, String name )
            throws ScmException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, name );
        Date actDate = breakpointFile.getCreateTime();
        if ( date == null ) {
            date = new Date();
            Assert.assertTrue( Math.abs(
                    actDate.getTime() - date.getTime() ) < acceptableOffset );
        } else {
            Assert.assertEquals( actDate.getTime(), date.getTime() );
        }
        ScmFactory.BreakpointFile.deleteInstance( ws, name );
    }

    private void createBreakpointFile( String fileName, byte[] data, Date date )
            throws ScmException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, ScmChecksumType.CRC32 );
        new Random().nextBytes( data );
        byte[] datapart = new byte[ fileSize ];
        System.arraycopy( data, 0, datapart, 0, fileSize );
        if ( date != null ) {
            breakpointFile.setCreateTime( date );
        }
        breakpointFile.upload( new ByteArrayInputStream( data ) );
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            TestTools.LocalFile.removeFile( localPath );
            if ( runTestFileSuccess == true ) {
                ScmFileUtils.cleanFile( wsp, queryCond );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
