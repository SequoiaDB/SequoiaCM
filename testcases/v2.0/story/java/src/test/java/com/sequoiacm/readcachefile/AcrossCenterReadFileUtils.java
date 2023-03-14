package com.sequoiacm.readcachefile;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @Descreption
 * @Author
 * @Date
 */
public class AcrossCenterReadFileUtils {
    /**
     * 指定站点读取文件并缓存至本地
     *
     * @param workspace
     * @param fileId
     * @param localpath
     * @return
     * @throws Exception
     */
    public static String readFile( ScmWorkspace workspace, ScmId fileId,
            File localpath ) throws Exception {
        OutputStream os = null;
        String downloadPath = TestTools.LocalFile.initDownloadPath( localpath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        try {
            ScmFile instance = ScmFactory.File.getInstance( workspace, fileId );
            os = new FileOutputStream( new File( downloadPath ) );
            instance.getContent( os );
        } finally {
            if ( os != null ) {
                os.close();
            }
        }
        return downloadPath;
    }

    /**
     * getContent接口指定readFlag读取文件
     *
     * @param workspace
     * @param fileId
     * @param localpath
     * @param readFlag
     * @return
     * @throws Exception
     */
    public static String getContentReadFile( ScmWorkspace workspace,
            ScmId fileId, File localpath, int readFlag ) throws Exception {
        OutputStream os = null;
        String downloadPath = TestTools.LocalFile.initDownloadPath( localpath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        try {
            ScmFile instance = ScmFactory.File.getInstance( workspace, fileId );
            os = new FileOutputStream( new File( downloadPath ) );
            instance.getContent( os, readFlag );
        } finally {
            if ( os != null ) {
                os.close();
            }
        }
        return downloadPath;
    }

    /**
     * getInputStream接口指定readFlag读取
     *
     * @param workspace
     * @param fileId
     * @param localpath
     * @param readFlag
     * @return
     * @throws Exception
     */
    public static String getInputStreamReadFile( ScmWorkspace workspace,
            ScmId fileId, File localpath, int readFlag ) throws Exception {
        OutputStream os = null;
        ScmInputStream is = null;
        String downloadPath = TestTools.LocalFile.initDownloadPath( localpath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        try {
            ScmFile instance = ScmFactory.File.getInstance( workspace, fileId );
            is = ScmFactory.File.createInputStream( instance, readFlag );
            os = new FileOutputStream( downloadPath );
            is.read( os );
        } finally {
            if ( is != null ) {
                is.close();
            }
            if ( os != null ) {
                os.close();
            }
        }
        return downloadPath;
    }

    /**
     * getInputStream偏移读取
     *
     * @param workspace
     * @param fileId
     * @param localpath
     * @param readFlag
     * @param length
     * @return
     * @throws Exception
     */
    public static String getInputStreamSeekFile( ScmWorkspace workspace,
            ScmId fileId, File localpath, int readFlag, int length )
            throws Exception {
        OutputStream os = null;
        ScmInputStream is = null;
        String downloadPath = TestTools.LocalFile.initDownloadPath( localpath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        try {
            ScmFile instance = ScmFactory.File.getInstance( workspace, fileId );
            is = ScmFactory.File.createInputStream( instance, readFlag );
            is.seek( CommonDefine.SeekType.SCM_FILE_SEEK_SET, length );
            os = new FileOutputStream( downloadPath );
            is.read( os );
        } finally {
            if ( is != null ) {
                is.close();
            }
            if ( os != null ) {
                os.close();
            }
        }
        return downloadPath;
    }

    /**
     * 获取所有存在指定文件缓存的站点id
     *
     * @param wsName
     * @param fileId
     * @param localPath
     * @param filePath
     * @return
     * @throws Exception
     */
    public static Object[] getCacheDataSites( String wsName, ScmId fileId,
            File localPath, String filePath ) throws Exception {
        List actSitesIds = new ArrayList<>();
        List< SiteWrapper > allSites = ScmInfo.getAllSites();
        String expMd5 = TestTools.getMD5( filePath );
        for ( SiteWrapper site : allSites ) {
            ScmSession session = ScmSessionUtils.createSession( site );
            ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            ScmFile file = ScmFactory.File.getInstance( workspace, fileId );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            try {
                TestTools.LocalFile.removeFile( downloadPath );
                file.getContentFromLocalSite( downloadPath );
                String actMd5 = TestTools.getMD5( downloadPath );
                if ( expMd5.equals( actMd5 ) ) {
                    actSitesIds.add( site.getSiteId() );
                }
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DATA_NOT_EXIST ) {
                    throw e;
                }
            } finally {
                session.close();
                TestTools.LocalFile.removeFile( downloadPath );
            }
        }
        return actSitesIds.toArray();
    }
}
