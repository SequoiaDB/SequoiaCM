package com.sequoiacm.scmfile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;

/**
 * @Testcase: SCM-936:ScmOutputStream.write偏移写文件
 * @author huangxiaoni init
 * @date 2017.9.21
 */

public class Scmfile936_writeByOutputStream_byOff02 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess1 = false;
    private boolean runSuccess2 = false;
    private boolean runSuccess3 = false;
    private ScmWorkspace ws = null;

    private String fileName = "Scmfile936_02";
    private List< ScmId > fileIdList = new ArrayList<>();
    private int fileSize = 200 * 1024;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = ScmSessionUtils.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    /*
     * 2）off<文件大小,len<文件大小-off；
     */
    @Test(groups = { GroupTags.base })
    private void testOffLtFileSize01() throws Exception {
        int off = fileSize / 2;
        int len = ( fileSize - off ) - 1;
        ScmId fileId = this.writeScmFile( off, len );

        int expScmfileSize = len;
        this.readAndCheckResults( fileId, off, len, expScmfileSize );

        runSuccess1 = true;
    }

    /*
     * 3）off<文件大小,len=文件大小-off；
     */
    @Test(groups = { GroupTags.base })
    private void testOffLtFileSize02() throws Exception {
        int off = fileSize / 2;
        int len = fileSize - off;
        ScmId fileId = this.writeScmFile( off, len );

        int expScmfileSize = len;
        this.readAndCheckResults( fileId, off, len, expScmfileSize );

        runSuccess2 = true;
    }

    /*
     * 4）off<文件大小,len>文件大小-off；
     */
    @Test(groups = { GroupTags.base })
    private void testOffLtFileSize03() throws IOException {
        int off = fileSize / 2;
        int len = ( fileSize - off ) + 1;
        try {
            this.writeScmFile( off, len );
            Assert.fail(
                    "expect fail, actual success, when off < fileSize and len"
                            + " > remainSize" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        runSuccess3 = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( ( runSuccess1 && runSuccess2 && runSuccess3 )
                    || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private ScmId writeScmFile( int off, int len )
            throws IOException, ScmException {
        ScmId fileId = null;
        ScmOutputStream sos = null;
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName + "_" + UUID.randomUUID() );
            sos = ScmFactory.File.createOutputStream( file );
            byte[] buffer = TestTools.getBuffer( filePath );
            sos.write( buffer, off, len );
            sos.commit();
            fileId = file.getFileId();
            fileIdList.add( fileId );
        } catch ( ScmException e ) {
            throw e;
        }
        return fileId;
    }

    private void readAndCheckResults( ScmId fileId, int size, int len,
            int expScmfileSize ) throws Exception {
        // read scmfile
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        file.getContent( downloadPath );

        // check content's length
        Assert.assertEquals( new File( downloadPath ).length(),
                expScmfileSize );

        // read content
        String downloadPath2 = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        TestTools.LocalFile.readFile( filePath, size, len, downloadPath2 );

        Assert.assertEquals( TestTools.getMD5( downloadPath ),
                TestTools.getMD5( downloadPath2 ) );
        Assert.assertNotEquals( TestTools.getMD5( downloadPath ),
                TestTools.getMD5( filePath ) );
    }

}