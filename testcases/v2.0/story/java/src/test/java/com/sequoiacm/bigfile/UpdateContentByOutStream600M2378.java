package com.sequoiacm.bigfile;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmOutputStream;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @Description：通过输出流的方式更新文件
 * @author fanyu
 * @Date:2019年02月14日
 * @version:1.0
 */

public class UpdateContentByOutStream600M2378 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String fileName = "updateStream600M";
    private int fileSize = 1024 * 800;
    private long updateFileSize = 1024 * 1024 * 600;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        fileId = ScmFileUtils.create( ws, fileName, filePath );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        // test a:scmfile outputStream update Content
        updateContentByAllOutputStream();
        // check result
        int currentVersion = 2;
        int historyVersion1 = 1;

        VersionUtils.CheckFileContentByFile( ws, fileName, historyVersion1,
                filePath, localPath );
        VersionUtils.checkFileCurrentVersion( ws, fileId, currentVersion );
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            ScmFactory.File.deleteInstance( ws, fileId, true );
            TestTools.LocalFile.removeFile( localPath );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    // test a
    private void updateContentByAllOutputStream() throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmOutputStream fileOutStream = ScmFactory.File
                .createUpdateOutputStream( file );
        long written = 0;
        byte[] fileBlock = new byte[ 1024 ];
        while ( written < updateFileSize ) {
            new Random().nextBytes( fileBlock );
            long toWrite = updateFileSize - written;
            long len = fileBlock.length < toWrite ? fileBlock.length : toWrite;
            fileOutStream.write( fileBlock, 0, ( int ) len );
            written += len;
        }
        fileOutStream.commit();
    }
}