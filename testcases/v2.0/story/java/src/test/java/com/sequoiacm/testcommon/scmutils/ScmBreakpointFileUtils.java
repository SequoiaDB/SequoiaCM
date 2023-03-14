package com.sequoiacm.testcommon.scmutils;

import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import org.testng.SkipException;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testresource.SkipTestException;

/**
 * @Description ScmBreakpointFileUtils.java
 * @author zhangYanan
 * @date 2022.08.08
 */
public class ScmBreakpointFileUtils extends TestScmBase {

    /**
     * @descreption checkDBDataSource ,if dataSource type not have SEQUOIADB or CEPH_S3 ,skip!
     * @throws Exception
     * @return SEQUOIADB and CEPH_S3 dataSource sites
     */
    public static List< SiteWrapper > checkDBAndCephS3DataSource() {
        List< SiteWrapper > sites = ScmInfo.getAllSites();
        List< SiteWrapper > DBSites = new ArrayList<>();
        for ( SiteWrapper site : sites ) {
            ScmType.DatasourceType dsType = site.getDataType();
            if ( dsType.equals( ScmType.DatasourceType.SEQUOIADB )
                    || dsType.equals( ScmType.DatasourceType.CEPH_S3 ) ) {
                DBSites.add( site );
            }
        }
        if ( DBSites.size() == 0 ) {
            throw new SkipTestException(
                    "breakpoint file only support sequoiadb datasourse and ceph S3 datasourse, "
                            + "skip!" );
        } else {
            return DBSites;
        }
    }

    /**
     * @descreption 通过字节流创建断点文件
     * @param ws
     * @param fileName
     * @param data
     * @return ScmBreakpointFile
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
     * @descreption 断点文件转为文件
     * @param breakpointFileName
     * @param fileName
     * @param fileIdList
     * @param siteWorkspace
     * @return
     * @throws Exception
     */
    public static long breakpointFileToFile( String breakpointFileName,
            ScmWorkspace siteWorkspace, String fileName,
            List<ScmId> fileIdList ) throws Exception {
        long uploadBeginTime = System.nanoTime();
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( siteWorkspace, breakpointFileName );
        ScmFile file = ScmFactory.File.createInstance( siteWorkspace );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        file.setContent( breakpointFile );
        fileIdList.add( file.save() );
        long uploadTime = TimeUnit.MILLISECONDS.convert(
                System.nanoTime() - uploadBeginTime, TimeUnit.NANOSECONDS );
        return uploadTime;
    }

    /**
     * @descreption 创建断点文件
     * @param breakpointFileName
     * @param siteWorkspace
     * @return
     * @throws Exception
     */
    public static long createAndUploadBreakpointFile( String breakpointFileName,
            ScmWorkspace siteWorkspace, String filePath ) throws Exception {
        long uploadBeginTime = System.nanoTime();
        ScmChecksumType checksumType = ScmChecksumType.NONE;
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( siteWorkspace, breakpointFileName,
                        checksumType );
        FileInputStream fStream = new FileInputStream( filePath );
        breakpointFile.upload( fStream );
        long uploadTime = TimeUnit.MILLISECONDS.convert(
                System.nanoTime() - uploadBeginTime, TimeUnit.NANOSECONDS );
        return uploadTime;
    }
}
