/**
 *
 */
package com.sequoiacm.testcommon.scmutils;

import java.io.*;
import java.util.*;
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

import org.testng.Assert;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testresource.SkipTestException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;

/**
 * @Description VersionUtil.java
 * @author wuyan
 * @date 2018.6.2
 */
public class VersionUtils extends TestScmBase {
    // 5min
    private static final int defaultTimeOut = 5 * 60;

    /**
     * @descreption 校验是否存在db数据源
     * @return
     * @throws Exception
     */
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
     * @descreption updateContent File by the local file
     * @param ws
     * @param fileName
     * @param fileId
     * @param filePath
     * @return
     * @throws Exception
     */
    public static void updateContentByFile( ScmWorkspace ws, String fileName,
            ScmId fileId, String filePath ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        file.updateContent( filePath );
        file.setFileName( fileName );
    }


    /**
     * @descreption updateContent File by stream
     * @param ws
     * @param fileId
     * @param updateData
     * @return
     * @throws ScmException
     */
    public static void updateContentByStream( ScmWorkspace ws, ScmId fileId,
            byte[] updateData ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        new Random().nextBytes( updateData );
        file.updateContent( new ByteArrayInputStream( updateData ) );
    }


    /**
     * @descreption check the file context by stream
     * @param ws
     * @param fileName
     * @param version
     * @param filedata
     * @return
     * @throws ScmException
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
        // Assert.assertEquals(downloadData, filedata,
        // "act:"+new String(downloadData)+"exp="+new String(filedata));
        assertByteArrayEqual( downloadData, filedata );
    }

    /**
     * @descreption 断言两字节数组相等
     * @param actual
     * @param expect
     * @return
     * @throws ScmException
     */
    public static void assertByteArrayEqual( byte[] actual, byte[] expect ) {
        assertByteArrayEqual( actual, expect, "" );
    }

    /**
     * @descreption 断言两字节数组相等
     * @param actual
     * @param expect
     * @param msg
     * @return
     * @throws ScmException
     */
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

    /**
     * @descreption 获取CallerName
     * @return
     * @throws ScmException
     */
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
     * @descreption check the file context by file,, use fileName
     * @param ws
     * @param fileName
     * @param version
     * @param filePath
     * @param localPath
     * @return
     * @throws Exception
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
     * @descreption check the file context by file, use fileId
     * @param ws
     * @param fileId
     * @param version
     * @param filePath
     * @param localPath
     * @return
     * @throws Exception
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
     * @descreption check scmfile's meta of sitelist
     * @param ws
     * @param fileId
     * @param major_version
     * @param expSites
     * @return
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

    /**
     * @descreption 校验调度任务相关的站点信息
     * @param ws
     * @param fileIds
     * @param major_version
     * @param expSites
     * @return
     * @throws ScmException
     */
    public static void checkScheTaskFileSites( ScmWorkspace ws,
            List< ScmId > fileIds, int major_version, SiteWrapper[] expSites )
            throws Exception {
        checkScheTaskFileSites( ws, fileIds, major_version, 0, fileIds.size(),
                expSites, defaultTimeOut );
    }

    /**
     * @descreption 校验调度任务相关的站点信息
     * @param ws
     * @param fileIds
     * @param major_version
     * @param expSites
     * @param timeOutSec
     * @return
     * @throws Exception
     */
    public static void checkScheTaskFileSites( ScmWorkspace ws,
            List< ScmId > fileIds, int major_version, SiteWrapper[] expSites,
            int timeOutSec ) throws Exception {
        checkScheTaskFileSites( ws, fileIds, major_version, 0, fileIds.size(),
                expSites, timeOutSec );
    }

    /**
     * @descreption 校验调度任务相关的站点信息
     * @param ws
     * @param fileIds
     * @param major_version
     * @param startNum
     * @param endNum
     * @param expSites
     * @return
     * @throws
     */
    public static void checkScheTaskFileSites( ScmWorkspace ws,
            List< ScmId > fileIds, int major_version, int startNum, int endNum,
            SiteWrapper[] expSites ) throws Exception {
        checkScheTaskFileSites( ws, fileIds, major_version, startNum, endNum,
                expSites, defaultTimeOut );
    }

    /**
     * @descreption 校验调度任务相关的站点信息
     * @param ws
     * @param fileIds
     * @param major_version
     * @param startNum
     * @param endNum
     * @param expSites
     * @param timeOutSec
     * @return
     * @throws
     */
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
                        TestSdbTools.Task.printlnTaskInfos();
                        throw new Exception(
                                "failed to wait task finished, " + "fileId = "
                                        + fileId + ", " + e.getMessage() );
                    }
                }
            }
        }
    }

    /**
     * @descreption check the scedule scmfile's meta of sitelist
     * @param ws
     * @param fileId
     * @param major_version
     * @param expSites
     * @return
     * @throws Exception
     */
    public static void checkScheduleFileSite( ScmWorkspace ws, ScmId fileId,
            int major_version, SiteWrapper[] expSites ) throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId, major_version,
                0 );
        int actSiteNum = file.getLocationList().size();
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
        // sort the actual siteId
        if ( actSiteNum != expSites.length ) {
            throw new Exception( "Failed to check siteNum, ws = " + ws.getName()
                    + ", fileId = " + fileId.get() + ", expSiteNum = "
                    + expSites.length + ", actSiteNum = " + actSiteNum
                    + ", actsiteId:" + actIdList.toString() + ";expsiteId:"
                    + expIdList.toString() );
        }
        if ( !actIdList.equals( expIdList ) ) {
            throw new Exception( "Failed to check siteId, ws = " + ws.getName()
                    + ", fileId = " + fileId.get() + ", actsiteId:"
                    + actIdList.toString() + ";expsiteId:"
                    + expIdList.toString() );
        }
    }

    /**
     * @descreption check scmfile's meta of major_version
     * @param ws
     * @param fileId
     * @param majorVersion
     * @return
     * @throws ScmException
     */
    public static void checkFileCurrentVersion( ScmWorkspace ws, ScmId fileId,
            int majorVersion ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        int fileVersion = file.getMajorVersion();
        Assert.assertEquals( fileVersion, majorVersion );
    }

    /**
     * @descreption check scmfile's meta of major_version
     * @param ws
     * @param fileId
     * @param majorVersion
     * @return
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
     * @descreption check scmfile's meta of size
     * @param ws
     * @param fileId
     * @param version
     * @param expSize
     * @throws ScmException
     */
    public static void checkFileSize( ScmWorkspace ws, ScmId fileId,
            int version, int expSize ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId, version, 0 );
        Assert.assertEquals( file.getSize(), expSize );
        Assert.assertEquals( file.getMajorVersion(), version );

    }

}
