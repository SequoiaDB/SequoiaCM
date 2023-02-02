package com.sequoiacm.workspace.serial;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.core.*;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmCephS3DataLocation;
import com.sequoiacm.client.element.bizconf.ScmCephS3UserConfig;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.crypto.ScmPasswordMgr;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.dsutils.CephS3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @descreption SCM-5635:修改数据源用户配置信息和文件操作并发
 * @author ZhangYanan
 * @date 2023/01/09
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5635 extends TestScmBase {
    private String wsName = "ws5635";
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private SiteWrapper rootSite = null;
    private final String access_key = "abcdefghijklmnopqrstuvwxyzabcdefghij5635";
    private static final String secret_key = "abcdefghijklmnopqrstuvwxyzabcdefghijklmn";
    private File localPath = null;
    private ScmSession session = null;
    private final String uid = "uid5635";
    private final String passwordFileName = uid + "testPwd.txt";
    private static String passwdFilePath = null;
    private int fileSize = 1024 * 1024;
    private String fileName = "file5635_";
    private ScmWorkspace ws;
    private ScmId uploadFileId = null;
    private ScmId updateFileId = null;
    private ScmId transferFileId = null;
    private ScmId cleanFileId = null;
    private ScmId deleteFileId = null;
    private String filePath = null;
    private String fileUpdatePath = null;
    private String passwdLocalPath = null;
    private ArrayList< SiteWrapper > siteList = new ArrayList<>();

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        fileUpdatePath = localPath + File.separator + "updateFile_" + fileSize
                + ".txt";
        passwdLocalPath = localPath + File.separator + "passwdFile1_" + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        rootSite = ScmInfo.getRootSite();
        site = ScmInfo.getSiteByType( ScmType.DatasourceType.CEPH_S3 );
        siteList.add( site );

        // 加密密码,后续写入文件中
        String cryptPassword = ScmPasswordMgr.getInstance()
                .encrypt( ScmPasswordMgr.SCM_CRYPT_TYPE_DES, secret_key );
        TestTools.LocalFile.createFileSpecifiedContent( passwdLocalPath,
                access_key + ":" + cryptPassword );

        CephS3Utils.deleteCephS3User( site, uid, true );
        CephS3Utils.createCephS3UserAndKey( site, uid, access_key, secret_key,
                true );

        passwdFilePath = CephS3Utils.preparePasswdFile( site, passwdLocalPath,
                passwordFileName );

        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        prepareFile();
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( new UpdateWsUserThread( site, false, access_key,
                passwdFilePath ) );
        t.addWorker( new UploadFileThread( site ) );
        t.addWorker( new UpDateFileThread( site ) );
        t.addWorker( new TransferFileThread( site, fileName + "transfer" ) );
        t.addWorker( new CleanFileThread( site, fileName + "clean" ) );
        t.addWorker( new DeleteFileThread( site ) );
        t.run();

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmWorkspaceUtil.deleteWs( wsName, session );
                TestTools.LocalFile.removeFile( localPath );
                CephS3Utils.deleteCephS3User( site, uid, true );
                CephS3Utils.deletePasswdFile( site, passwdFilePath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    public static List< ScmDataLocation > prepareWsCephS3DataLocation(
            List< SiteWrapper > siteList, String access_key,
            String passwdFilePath, boolean isStandby )
            throws ScmInvalidArgumentException {
        if ( siteList.size() < 1 ) {
            throw new IllegalArgumentException(
                    "error, site num can't less than 1 ！" );
        }
        List< ScmDataLocation > scmDataLocationList = new ArrayList<>();
        for ( SiteWrapper site : siteList ) {
            ScmType.DatasourceType dataType = site.getDataType();
            String siteName = site.getSiteName();
            if ( dataType == ScmType.DatasourceType.CEPH_S3 ) {
                ScmCephS3UserConfig primaryConfig = new ScmCephS3UserConfig(
                        access_key, passwdFilePath );
                ScmCephS3UserConfig standbyConfig = new ScmCephS3UserConfig(
                        access_key, passwdFilePath );
                ScmCephS3DataLocation scmCephS3DataLocation = null;
                if ( isStandby ) {
                    scmCephS3DataLocation = new ScmCephS3DataLocation( siteName,
                            primaryConfig, standbyConfig );
                } else {
                    scmCephS3DataLocation = new ScmCephS3DataLocation( siteName,
                            primaryConfig );
                }
                scmDataLocationList.add( scmCephS3DataLocation );
            }
        }
        return scmDataLocationList;
    }

    private class UpdateWsUserThread {
        private boolean isStandby;
        private SiteWrapper site;
        private String access_key;
        private String passwdFilePath;

        public UpdateWsUserThread( SiteWrapper site, Boolean isStandby,
                String access_key, String passwdFilePath ) {
            this.isStandby = isStandby;
            this.site = site;
            this.access_key = access_key;
            this.passwdFilePath = passwdFilePath;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            ScmSession session = TestScmTools.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            try {
                List< ScmDataLocation > dataLocation = prepareWsCephS3DataLocation(
                        siteList, access_key, passwdFilePath, isStandby );
                ws.updateDataLocation( dataLocation );
                ScmWorkspaceUtil.checkWsUpdate( session, wsName, dataLocation );
            } finally {
                session.close();
            }
        }
    }

    private class UploadFileThread {
        private SiteWrapper site;

        public UploadFileThread( SiteWrapper site ) {
            this.site = site;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            ScmSession session = TestScmTools.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            try {
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setContent( filePath );
                file.setFileName( fileName + "upload" );
                file.setAuthor( fileName );
                uploadFileId = file.save();

                SiteWrapper[] expSites = { site };
                ScmFileUtils.checkMetaAndData( wsName, uploadFileId, expSites,
                        localPath, filePath );
            } finally {
                session.close();
            }
        }
    }

    private class UpDateFileThread {
        private SiteWrapper site;

        public UpDateFileThread( SiteWrapper site ) {
            this.site = site;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            ScmSession session = TestScmTools.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            try {
                ScmFile file = ScmFactory.File.getInstance( ws, updateFileId );
                file.updateContent( fileUpdatePath );

                SiteWrapper[] expSites = { site };
                ScmFileUtils.checkMetaAndData( wsName, updateFileId, expSites,
                        localPath, fileUpdatePath );
            } finally {
                session.close();
            }
        }
    }

    private class TransferFileThread {
        private SiteWrapper site;
        private String fileName;

        public TransferFileThread( SiteWrapper site, String fileName ) {
            this.site = site;
            this.fileName = fileName;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            ScmSession session = TestScmTools.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            try {
                BSONObject queryCond = ScmQueryBuilder
                        .start( ScmAttributeName.File.FILE_NAME ).is( fileName )
                        .get();
                ScmId taskId = ScmSystem.Task.startTransferTask( ws, queryCond,
                        ScmType.ScopeType.SCOPE_CURRENT,
                        rootSite.getSiteName() );
                ScmTaskUtils.waitTaskFinish( session, taskId );

                SiteWrapper[] expSites = { site, rootSite };
                ScmFileUtils.checkMetaAndData( wsName, transferFileId, expSites,
                        localPath, filePath );
            } finally {
                session.close();
            }
        }
    }

    private class CleanFileThread {
        private SiteWrapper site;
        private String fileName;

        public CleanFileThread( SiteWrapper site, String fileName ) {
            this.site = site;
            this.fileName = fileName;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            ScmSession session = TestScmTools.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            try {
                BSONObject queryCond = ScmQueryBuilder
                        .start( ScmAttributeName.File.FILE_NAME ).is( fileName )
                        .get();
                ScmId taskId = ScmSystem.Task.startCleanTask( ws, queryCond );
                ScmTaskUtils.waitTaskFinish( session, taskId );

                SiteWrapper[] expSites = { rootSite };
                ScmFileUtils.checkMetaAndData( wsName, cleanFileId, expSites,
                        localPath, filePath );
            } finally {
                session.close();
            }
        }
    }

    private class DeleteFileThread {
        private SiteWrapper site;

        public DeleteFileThread( SiteWrapper site ) {
            this.site = site;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            ScmSession session = TestScmTools.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            ScmFile file = ScmFactory.File.getInstance( ws, deleteFileId );
            file.delete( true );
            try {
                ScmFactory.File.getInstance( ws, deleteFileId );
                Assert.fail( "预期失败，实际成功" );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.FILE_NOT_FOUND ) {
                    throw e;
                }
            } finally {
                session.close();
            }
        }
    }

    public void prepareFile() throws Exception {
        // 准备文件修改线程文件
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( fileName + "update" );
        file.setAuthor( fileName );
        updateFileId = file.save();
        TestTools.LocalFile.createFile( fileUpdatePath, fileSize );

        // 准备文件迁移线程文件
        file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( fileName + "transfer" );
        file.setAuthor( fileName );
        transferFileId = file.save();

        // 准备文件清理线程文件
        file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( fileName + "clean" );
        file.setAuthor( fileName );
        cleanFileId = file.save();
        BSONObject queryCond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME )
                .is( fileName + "clean" ).get();
        ScmId taskId = ScmSystem.Task.startTransferTask( ws, queryCond,
                ScmType.ScopeType.SCOPE_CURRENT, rootSite.getSiteName() );
        ScmTaskUtils.waitTaskFinish( session, taskId );

        // 准备文件删除线程文件
        file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( fileName + "delete" );
        file.setAuthor( fileName );
        deleteFileId = file.save();
    }
}