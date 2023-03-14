package com.sequoiacm.scmfile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmBreakpointFileOption;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-2968 :: 指定计算md5，创建断点文件
 * @author fanyu
 * @Date:2020年8月27日
 * @version:1.0
 */
public class ScmFileMD5Calc2968 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileNameBase = "file2968_";
    private List< String > fileNameList = new ArrayList<>();
    private int[] fileSizes = new int[] { 0, 200 * 1024, 1024 * 1024 * 5 };
    private List< String > filePathList = new ArrayList<>();
    private File localPath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, IOException {
        List< SiteWrapper > sites = ScmBreakpointFileUtils.checkDBAndCephS3DataSource();
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
        site = sites.get( new Random().nextInt( sites.size() ) );
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test
    private void test() throws Exception {
        List< String > expMd5 = new ArrayList<>();
        for ( int i = 0; i < fileSizes.length; i++ ) {
            expMd5.add( TestTools.getMD5AsBase64( filePathList.get( i ) ) );
        }

        // 创建带有md5的断点文件,一次性上传
        for ( int i = 0; i < fileSizes.length; i++ ) {
            String fileName = fileNameBase + UUID.randomUUID() + "_A"
                    + fileSizes[ i ];
            fileNameList.add( fileName );
            ScmBreakpointFile breakpointFile = createBreakpointFile( fileName,
                    filePathList.get( i ) );
            Assert.assertEquals( breakpointFile.getMd5(), expMd5.get( i ),
                    breakpointFile.getFileName() );
        }

        // 创建带有md5的断点文件，增量上传
        for ( int i = 0; i < fileSizes.length; i++ ) {
            String fileName = fileNameBase + UUID.randomUUID() + "_B"
                    + fileSizes[ i ];
            fileNameList.add( fileName );
            ScmBreakpointFile breakpointFile = incrementalUpload( fileName,
                    filePathList.get( i ) );
            Assert.assertEquals( breakpointFile.getMd5(), expMd5.get( i ),
                    breakpointFile.getFileName() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( String fileName : fileNameList ) {
                    ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
                }
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private ScmBreakpointFile createBreakpointFile( String fileName,
            String filePath ) throws ScmException, IOException {
        ScmBreakpointFileOption option = new ScmBreakpointFileOption();
        option.setNeedMd5( true );
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, option );
        InputStream inputStream = new FileInputStream( filePath );
        breakpointFile.upload( inputStream );
        inputStream.close();
        return breakpointFile;
    }

    private ScmBreakpointFile incrementalUpload( String fileName,
            String filePath ) throws ScmException, IOException {
        ScmBreakpointFileOption option = new ScmBreakpointFileOption();
        option.setNeedMd5( true );
        option.setChecksumType( ScmChecksumType.ADLER32 );
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, option );
        InputStream inputStream = new FileInputStream( new File( filePath ) );
        byte[] buffer = new byte[ 4 * 1024 * 1024 ];
        breakpointFile.incrementalUpload( inputStream, true );
        int bytesRead = 0;
        int sumRead = 0;
        while ( ( bytesRead = inputStream.read( buffer, 0,
                buffer.length ) ) != -1 ) {
            sumRead += bytesRead;
            if ( sumRead == new File( filePath ).length() ) {
                breakpointFile.incrementalUpload(
                        new ByteArrayInputStream( buffer, 0, bytesRead ),
                        true );
            } else {
                breakpointFile.incrementalUpload(
                        new ByteArrayInputStream( buffer, 0, bytesRead ),
                        false );
            }
        }
        return breakpointFile;
    }
}