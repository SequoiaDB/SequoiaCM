package com.sequoiacm.scmfile;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @Description: SCM-2965:指定计算md5,创建文件
 * @author fanyu
 * @Date:2020年8月26日
 * @version:1.0
 */
public class ScmFileMD5Calc2965 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileNameBase = "file2965";
    private List< ScmId > fileIdList = new ArrayList<>();
    private int[] fileSizes = { 0, 200 * 1024 + 1 };
    private File localPath = null;
    private List< String > filePathList = new ArrayList<>();

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        for ( int i = 0; i < fileSizes.length; i++ ) {
            String filePath = localPath + File.separator + "localFile_"
                    + fileSizes[ i ] + ".txt";
            TestTools.LocalFile.createFile( filePath, fileSizes[ i ] );
            filePathList.add( filePath );
        }
        site = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        // 指定计算md5,创建文件
        for ( int i = 0; i < fileSizes.length; i++ ) {
            createFileByFilePath( fileNameBase + "_" + UUID.randomUUID(),
                    filePathList.get( i ) );
        }

        for ( int i = 0; i < fileSizes.length; i++ ) {
            createFileByOutputstream( fileNameBase + "_" + UUID.randomUUID(),
                    filePathList.get( i ) );
        }

        // 获取文件，检查文件属性和文件内容
        for ( int i = 0; i < fileIdList.size(); i++ ) {
            getAndCheckFile( fileIdList.get( i ), filePathList.get( i % fileSizes.length ) );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void createFileByFilePath( String fileName, String filePath )
            throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        ScmUploadConf scmUploadConf = new ScmUploadConf( true, true );
        file.setFileName( fileName );
        file.setContent( filePath );
        fileIdList.add( file.save( scmUploadConf ) );
    }

    private void createFileByOutputstream( String fileName, String filePath )
            throws ScmException, IOException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        file.setTitle( fileName );
        file.setMimeType( fileName );
        ScmUploadConf scmUploadConf = new ScmUploadConf( true, true );
        ScmOutputStream sos = ScmFactory.File.createOutputStream( file,
                scmUploadConf );
        byte[] buffer = TestTools.getBuffer( filePath );
        sos.write( buffer );
        sos.commit();
        fileIdList.add( file.getFileId() );
    }

    private void getAndCheckFile( ScmId fileId, String expFilePath )
            throws Exception {
        ScmFile scmFile = ScmFactory.File.getInstance( ws, fileId );
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        scmFile.getContent( downloadPath );
        // 检查
        File expFile = new File( expFilePath );
        String expMd5 = TestTools.getMD5AsBase64( expFilePath );
        Assert.assertEquals( scmFile.getSize(), expFile.length(),
                fileId.get() );
        Assert.assertEquals( scmFile.getMd5(), expMd5, fileId.get() );
        Assert.assertEquals( TestTools.getMD5AsBase64( downloadPath ), expMd5,
                fileId.get() );
    }
}