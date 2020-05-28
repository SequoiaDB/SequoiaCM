package com.sequoiacm.readcachefile;

import java.io.File;
import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @Testcase:SCM-275 read偏移+长度读取文件，参数校验（A/B网络不通） read(byte[] b,int off,int
 *                   len)测试： 有效参数： off+len < byte.lenght; b长度>0 off有效边界：0
 *                   len有效边界：1 无效参数： off+len >= byte.lenght; b:null off无效边界：-1
 *                   len无效边界：0
 * @author fanyu init in 2017.05.10; huangxiaoni modify in 2017.6.6
 */

public class Param_readByOff275 extends TestScmBase {
    private int runSuccessFlag = 0;
    private int expRunSuccessFlag = 4;

    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    private String fileName = "scmfile275";
    private ScmId fileId = null;
    private int fileSize = 10;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            // ready file
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            // login
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession();
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

            // write file
            fileId = ScmFileUtils.create( ws, fileName, filePath );
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    /*
     * invalid param: off+len >= byte.lenght; 1
     */
    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testOffAddLenGteByteSize() throws ScmException {
        ScmInputStream sis = null;
        try {
            // read content
            ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
            sis = ScmFactory.File.createInputStream( scmfile );
            byte[] buffer = new byte[ fileSize ];
            int off = 5;
            int len = 6;
            sis.read( buffer, off, len );
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT || !e.getMessage()
                    .contains( "indexOutOfBound,arraySize:10,off:5,len:6" ) ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( sis != null )
                sis.close();
        }
        runSuccessFlag++;
    }

    /*
     * byte[] b is null;2
     */
    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testBIsNull() throws ScmException {
        ScmInputStream sis = null;
        try {
            // read content
            ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
            sis = ScmFactory.File.createInputStream( scmfile );
            int off = 0;
            int len = 1;
            sis.read( null, off, len );
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT
                    || !e.getMessage().contains( "byteArray is null" ) ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( sis != null )
                sis.close();
        }
        runSuccessFlag++;
    }

    /*
     * invalid param: off is -1;3
     */
    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testOffIsNegative() throws ScmException {
        ScmInputStream sis = null;
        try {
            // read content
            ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
            sis = ScmFactory.File.createInputStream( scmfile );
            byte[] buffer = new byte[ fileSize ];
            int off = -1;
            int len = 1;
            sis.read( buffer, off, len );
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT || !e.getMessage()
                    .contains( "indexOutOfBound,arraySize:10,off:-1,len:1" ) ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( sis != null )
                sis.close();
        }
        runSuccessFlag++;
    }

    /*
     * invalid param: len is 0;4
     */
    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testLenIs0() throws ScmException {
        ScmInputStream sis = null;
        try {
            // read content
            ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
            sis = ScmFactory.File.createInputStream( scmfile );
            byte[] buffer = new byte[ fileSize ];
            int off = 1;
            int len = 0;
            sis.read( buffer, off, len );
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT || !e.getMessage()
                    .contains( "len must be greater than zero:0" ) ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( sis != null )
                sis.close();
        }
        runSuccessFlag++;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccessFlag == expRunSuccessFlag || forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }

        }
    }

}
