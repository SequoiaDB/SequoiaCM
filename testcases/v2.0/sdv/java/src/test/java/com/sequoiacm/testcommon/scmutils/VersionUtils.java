/**
 *
 */
package com.sequoiacm.testcommon.scmutils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.sequoiacm.testresource.SkipTestException;
import org.testng.Assert;
import org.testng.SkipException;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;

/**
 * @Description VersionUtil.java
 * @author wuyan
 * @date 2018.6.2
 */
public class VersionUtils extends TestScmBase {
    // 5min
    private static final int defaultTimeOut = 5 * 60;

    public static void checkDBDataSource() {
        List< SiteWrapper > sites = ScmInfo.getAllSites();
        for ( SiteWrapper site : sites ) {
            DatasourceType dsType = site.getDataType();
            if ( !dsType.equals( DatasourceType.SEQUOIADB ) ) {
                throw new SkipTestException(
                        "breakpoint file only support sequoiadb datasourse, "
                                + "skip!" );
            }
        }
    }

    /**
     * create file, the file content are randomly generated character
     *
     * @param filePath
     * @param size
     * @throws IOException
     */
    public static void createFile( String filePath, int size )
            throws IOException {
        FileOutputStream fos = null;
        try {
            TestTools.LocalFile.createFile( filePath );
            File file = new File( filePath );
            fos = new FileOutputStream( file );

            byte[] fileBlock = new byte[ size ];
            new Random().nextBytes( fileBlock );
            fos.write( fileBlock );
        } catch ( IOException e ) {
            System.out.println( "create file failed, file=" + filePath );
            throw e;
        } finally {
            if ( fos != null ) {
                fos.close();
            }
        }
    }

    /**
     * create File by stream
     *
     * @param ws
     * @param fileName
     * @param data
     * @param authorName
     * @throws ScmException
     */
    public static ScmId createFileByStream( ScmWorkspace ws, String fileName,
            byte[] data, String authorName ) throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        new Random().nextBytes( data );
        file.setContent( new ByteArrayInputStream( data ) );
        file.setFileName( fileName );
        file.setAuthor( authorName );
        file.setTitle( "sequoiacm" );
        file.setMimeType( fileName + ".txt" );
        ScmId fileId = file.save();
        return fileId;
    }

    /**
     * create File by stream
     *
     * @param ws
     * @param fileName
     * @param data
     * @throws ScmException
     */
    public static ScmId createFileByStream( ScmWorkspace ws, String fileName,
            byte[] data ) throws ScmException {
        return createFileByStream( ws, fileName, data, fileName );
    }

    /**
     * create File by the local file *
     * 
     * @param ws
     * @param fileName
     * @param filePath
     * @throws ScmException
     */
    public static ScmId createFileByFile( ScmWorkspace ws, String fileName,
            String filePath ) throws ScmException {
        return createFileByFile( ws, fileName, filePath, fileName );
    }

    /**
     * create File by the local file *
     * 
     * @param ws
     * @param fileName
     * @param filePath
     * @param authorName
     * @throws ScmException
     */
    public static ScmId createFileByFile( ScmWorkspace ws, String fileName,
            String filePath, String authorName ) throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( fileName );
        file.setAuthor( authorName );
        file.setTitle( "sequoiacm" );
        file.setMimeType( fileName + ".txt" );
        ScmId fileId = file.save();
        return fileId;
    }

    /*
     *
     */
    public static ScmBreakpointFile createBreakpointFileByStream(
            ScmWorkspace ws, String fileName, byte[] data )
            throws ScmException {
        ScmBreakpointFile sbFile = ScmFactory.BreakpointFile.createInstance( ws,
                fileName );
        sbFile.upload( new ByteArrayInputStream( data ) );
        return sbFile;
    }

    /**
     * updateContent File by the local file
     */
    public static void updateContentByFile( ScmWorkspace ws, String fileName,
            ScmId fileId, String filePath ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        file.updateContent( filePath );
        file.setFileName( fileName );
    }

    /**
     * updateContent File by stream
     */
    public static void updateContentByStream( ScmWorkspace ws, ScmId fileId,
            byte[] updateData ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        new Random().nextBytes( updateData );
        file.updateContent( new ByteArrayInputStream( updateData ) );
    }

    /**
     * check the file context by stream
     */
    public static void CheckFileContentByStream( ScmWorkspace ws,
            String fileName, int version, byte[] filedata ) throws Exception {
        ScmFile file = ScmFactory.File.getInstanceByPath( ws, fileName, version,
                0 );
        // down file
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        file.getContent( outputStream );
        byte[] downloadData = outputStream.toByteArray();

        // check results
        assertByteArrayEqual( downloadData, filedata );
    }

    public static void assertByteArrayEqual( byte[] actual, byte[] expect ) {
        assertByteArrayEqual( actual, expect, "" );
    }

    public static void assertByteArrayEqual( byte[] actual, byte[] expect,
            String msg ) {
        if ( !Arrays.equals( actual, expect ) ) {
            String workDirPath = TestScmBase.dataDirectory;
            File workDir = new File( workDirPath );
            if ( !workDir.isDirectory() )
                throw new RuntimeException(
                        "the path can not use: " + workDirPath );

            String callerClassName = getCallerName();
            File fileActual = new File( workDirPath + File.separator
                    + callerClassName + "_actual" );
            File fileExpect = new File( workDirPath + File.separator
                    + callerClassName + "_expect" );
            try {
                if ( fileActual.exists() ) {
                    fileActual.delete();
                    fileActual.createNewFile();
                }
                if ( fileExpect.exists() ) {
                    fileExpect.delete();
                    fileExpect.createNewFile();
                }

                try ( FileOutputStream out = new FileOutputStream(
                        fileActual )) {
                    out.write( actual );
                    out.flush();
                }
                try ( FileOutputStream out = new FileOutputStream(
                        fileExpect )) {
                    out.write( expect );
                    out.flush();
                }

                Assert.fail( msg + "; data is written into files in "
                        + workDirPath );
            } catch ( FileNotFoundException e ) {
                e.printStackTrace();
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }

    private static String getCallerName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String thisClassName = stackTrace[ 1 ].getClassName();
        String currClassName = null;
        for ( int i = 2; i < stackTrace.length; ++i ) {
            currClassName = stackTrace[ i ].getClassName();
            if ( !currClassName.equals( thisClassName ) ) {
                break;
            }
        }
        String classFullName = currClassName;
        String[] classNameArr = classFullName.split( "\\." );
        String simpleClassName = classNameArr[ classNameArr.length - 1 ];
        return simpleClassName;
    }

    /**
     * check the file context by file,, use fileName
     */
    public static void CheckFileContentByFile( ScmWorkspace ws, String fileName,
            int version, String filePath, File localPath ) throws Exception {
        ScmFile file = ScmFactory.File.getInstanceByPath( ws, fileName, version,
                0 );
        // down file
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        file.getContent( downloadPath );

        // check results
        Assert.assertEquals( TestTools.getMD5( downloadPath ),
                TestTools.getMD5( filePath ) );
        TestTools.LocalFile.removeFile( downloadPath );
    }

    /**
     * check the file context by file, use fileId
     */
    public static void CheckFileContentByFile( ScmWorkspace ws, ScmId fileId,
            int version, String filePath, File localPath ) throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId, version, 0 );
        // down file
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        file.getContent( downloadPath );

        // check results
        Assert.assertEquals( TestTools.getMD5( downloadPath ),
                TestTools.getMD5( filePath ) );
        TestTools.LocalFile.removeFile( downloadPath );
    }

    /**
     * @param ws
     * @param fileName
     * @param filePath
     * @param checkFilePath
     * @throws ScmException
     * @throws IOException
     */
    public static void checkScmFile( ScmWorkspace ws, String fileName,
            String filePath, String checkFilePath )
            throws ScmException, IOException {
        TestTools.LocalFile.removeFile( checkFilePath );
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName );
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( breakpointFile );
        file.setFileName( fileName );
        file.setTitle( fileName );
        ScmId fielId = file.save();

        ScmFile file1 = ScmFactory.File.getInstance( ws, fielId );
        file1.getContent( checkFilePath );
        Assert.assertEquals( TestTools.getMD5( checkFilePath ),
                TestTools.getMD5( filePath ),
                "check breakpointFile to ScmFile" );

        ScmFactory.File.deleteInstance( ws, fielId, true );
        TestTools.LocalFile.removeFile( checkFilePath );
    }

    /**
     * check scmfile's meta of sitelist
     *
     * @param ws
     * @param fileId
     * @param major_version
     * @param expSiteIdArr
     * @throws ScmException
     */
    public static void checkSite( ScmWorkspace ws, ScmId fileId,
            int major_version, SiteWrapper[] expSites ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId, major_version,
                0 );

        // sort the actual siteId
        int actSiteNum = file.getLocationList().size();

        if ( actSiteNum != expSites.length ) {
            Assert.fail( "the sites num diff, actSiteNum=" + actSiteNum );
        }
        List< Integer > actIdList = new ArrayList<>();
        for ( int i = 0; i < actSiteNum; i++ ) {
            int siteId = file.getLocationList().get( i ).getSiteId();
            actIdList.add( siteId );
        }
        Collections.sort( actIdList );

        // sort the expect siteId
        List< Integer > expIdList = new ArrayList<>();
        for ( int i = 0; i < expSites.length; i++ ) {
            expIdList.add( expSites[ i ].getSiteId() );
        }
        Collections.sort( expIdList );
        Assert.assertEquals( actIdList, expIdList, "actsiteId:"
                + actIdList.toString() + ";expsiteId:" + expIdList.toString() );

    }

    public static void checkScheTaskFileSites( ScmWorkspace ws,
            List< ScmId > fileIds, int major_version, SiteWrapper[] expSites )
            throws Exception {
        checkScheTaskFileSites( ws, fileIds, major_version, 0, fileIds.size(),
                expSites, defaultTimeOut );
    }

    public static void checkScheTaskFileSites( ScmWorkspace ws,
            List< ScmId > fileIds, int major_version, SiteWrapper[] expSites,
            int timeOutSec ) throws Exception {
        checkScheTaskFileSites( ws, fileIds, major_version, 0, fileIds.size(),
                expSites, timeOutSec );
    }

    public static void checkScheTaskFileSites( ScmWorkspace ws,
            List< ScmId > fileIds, int major_version, int startNum, int endNum,
            SiteWrapper[] expSites ) throws Exception {
        checkScheTaskFileSites( ws, fileIds, major_version, startNum, endNum,
                expSites, defaultTimeOut );
    }

    public static void checkScheTaskFileSites( ScmWorkspace ws,
            List< ScmId > fileIds, int major_version, int startNum, int endNum,
            SiteWrapper[] expSites, int timeOutSec ) throws Exception {
        ScmId fileId = null;
        for ( int i = startNum; i < endNum; i++ ) {
            fileId = fileIds.get( i );
            int sleepTime = 500;
            int maxRetryTimes = timeOutSec * 1000 / sleepTime;
            int retryTimes = 0;
            while ( true ) {
                try {
                    checkScheduleFileSite( ws, fileId, major_version,
                            expSites );
                    break;
                } catch ( Exception e ) {
                    if ( e.getMessage() != null
                            && e.getMessage()
                                    .contains( "Failed to check siteNum" )
                            && retryTimes < maxRetryTimes ) {
                        Thread.sleep( sleepTime );
                        retryTimes++;
                    } else {
                        e.printStackTrace();
                        throw new Exception(
                                "failed to wait task finished, " + "fileId = "
                                        + fileId + ", " + e.getMessage() );
                    }
                }
            }
        }
    }

    /**
     * check the scedule scmfile's meta of sitelist
     *
     * @param ws
     * @param fileId
     * @param major_version
     * @param expSiteIdArr
     * @throws Exception
     */
    public static void checkScheduleFileSite( ScmWorkspace ws, ScmId fileId,
            int major_version, SiteWrapper[] expSites ) throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId, major_version,
                0 );

        // sort the actual siteId
        int actSiteNum = file.getLocationList().size();
        if ( actSiteNum != expSites.length ) {
            throw new Exception( "Failed to check siteNum, ws = " + ws.getName()
                    + ", fileId = " + fileId.get() + ", expSiteNum = "
                    + expSites.length + ", actSiteNum = " + actSiteNum );
        }
        List< Integer > actIdList = new ArrayList<>();
        for ( int i = 0; i < actSiteNum; i++ ) {
            int siteId = file.getLocationList().get( i ).getSiteId();
            actIdList.add( siteId );
        }
        Collections.sort( actIdList );

        // sort the expect siteId
        List< Integer > expIdList = new ArrayList<>();
        for ( int i = 0; i < expSites.length; i++ ) {
            expIdList.add( expSites[ i ].getSiteId() );
        }
        Collections.sort( expIdList );
        if ( !actIdList.equals( expIdList ) ) {
            throw new Exception( "Failed to check siteId, ws = " + ws.getName()
                    + ", fileId = " + fileId.get() + ", actsiteId:"
                    + actIdList.toString() + ";expsiteId:"
                    + expIdList.toString() );
        }
    }

    /**
     * check scmfile's meta of major_version
     * 
     * @param ws
     * @param fileId
     * @param majorVersion
     * @param checkFilePath
     * @throws ScmException
     */
    public static void checkFileCurrentVersion( ScmWorkspace ws, ScmId fileId,
            int majorVersion ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        int fileVersion = file.getMajorVersion();
        Assert.assertEquals( fileVersion, majorVersion );
    }

    /**
     * check scmfile's meta of major_version
     * 
     * @param ws
     * @param fileId
     * @param majorVersion
     * @param checkFilePath
     * @throws ScmException
     */
    public static void checkFileVersion( ScmWorkspace ws, ScmId fileId,
            int majorVersion ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId, majorVersion,
                0 );
        int fileVersion = file.getMajorVersion();
        Assert.assertEquals( fileVersion, majorVersion );
    }

    /**
     * check scmfile's meta of size
     * 
     * @param ws
     * @param fileId
     * @param majorVersion
     * @param checkFilePath
     * @throws ScmException
     */
    public static void checkFileSize( ScmWorkspace ws, ScmId fileId,
            int version, int expSize ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId, version, 0 );
        Assert.assertEquals( file.getSize(), expSize );
        Assert.assertEquals( file.getMajorVersion(), version );

    }

    /**
     * wait asynchronous task finished, default timeOutSec
     *
     * @param ws
     * @param fileId
     * @param expSiteNum
     * @throws Exception
     */
    public static void waitAsyncTaskFinished( ScmWorkspace ws, ScmId fileId,
            int version, int expSiteNum ) throws Exception {
        waitAsyncTaskFinished( ws, fileId, version, expSiteNum,
                defaultTimeOut );
    }

    /**
     * wait asynchronous task finished, specify timeOutSec
     *
     * @param ws
     * @param fileId
     * @param majorVersion
     * @param expSiteNum
     * @param timeOutSec
     *            unit: seconds
     * @throws Exception
     */
    public static void waitAsyncTaskFinished( ScmWorkspace ws, ScmId fileId,
            int majorVersion, int expSiteNum, int timeOutSec )
            throws Exception {
        int sleepTime = 200; // millisecond
        int maxRetryTimes = timeOutSec * 1000 / sleepTime;
        int retryTimes = 0;
        while ( true ) {
            ScmFile file = ScmFactory.File.getInstance( ws, fileId,
                    majorVersion, 0 );
            int size = file.getLocationList().size();
            if ( size == expSiteNum ) {
                break;
            } else if ( retryTimes >= maxRetryTimes ) {
                throw new Exception( "wait async task, retry failed." );
            }
            Thread.sleep( sleepTime );
            retryTimes++;
        }
    }

}
