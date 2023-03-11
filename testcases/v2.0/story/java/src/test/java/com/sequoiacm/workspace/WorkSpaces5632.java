package com.sequoiacm.workspace;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
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
 * @descreption SCM-5632:并发修改数据源用户配置信息
 * @author ZhangYanan
 * @date 2023/01/09
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5632 extends TestScmBase {
    private String wsName = "ws5632";
    private SiteWrapper site = null;
    private final String access_keyA = "abcdefghijklmnopqrstuvwxyzabcdefghij5632a";
    private final String access_keyB = "abcdefghijklmnopqrstuvwxyzabcdefghij5632b";
    private static final String secret_key = "abcdefghijklmnopqrstuvwxyzabcdefghijklmn";
    private File localPath = null;
    private ScmSession session = null;
    private final String uid = "uid5632";
    private final String passwordFileNameA = uid + "testPwdA.txt";
    private final String passwordFileNameB = uid + "testPwdB.txt";
    private static String passwdFilePathA = null;
    private static String passwdFilePathB = null;
    private int fileSize = 1024 * 1024;
    private String fileName = "file5632";
    private ScmWorkspace ws;
    private String filePath = null;
    private String passwdLocalPathA = null;
    private String passwdLocalPathB = null;
    private ArrayList< SiteWrapper > siteList = new ArrayList<>();
    private AtomicInteger successTestCount = new AtomicInteger( 0 );

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        passwdLocalPathA = localPath + File.separator + "passwdFile1_" + ".txt";
        passwdLocalPathB = localPath + File.separator + "passwdFile2_" + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = ScmInfo.getSiteByType( ScmType.DatasourceType.CEPH_S3 );
        siteList.add( site );

        // 加密密码,后续写入文件中
        String cryptPassword = ScmPasswordMgr.getInstance()
                .encrypt( ScmPasswordMgr.SCM_CRYPT_TYPE_DES, secret_key );
        TestTools.LocalFile.createFileSpecifiedContent( passwdLocalPathA,
                access_keyA + ":" + cryptPassword );
        TestTools.LocalFile.createFileSpecifiedContent( passwdLocalPathB,
                access_keyB + ":" + cryptPassword );

        CephS3Utils.deleteCephS3User( site, uid, false );
        CephS3Utils.deleteCephS3User( site, uid, true );
        CephS3Utils.createCephS3UserAndKey( site, uid, access_keyA, secret_key,
                false );
        CephS3Utils.createCephS3UserAndKey( site, uid, access_keyB, secret_key,
                false );

        CephS3Utils.createCephS3UserAndKey( site, uid, access_keyA, secret_key,
                true );
        CephS3Utils.createCephS3UserAndKey( site, uid, access_keyB, secret_key,
                true );

        passwdFilePathA = CephS3Utils.preparePasswdFile( site, passwdLocalPathA,
                passwordFileNameA );
        passwdFilePathB = CephS3Utils.preparePasswdFile( site, passwdLocalPathB,
                passwordFileNameB );

        session = TestScmTools.createSession( site );
    }

    @DataProvider(name = "data")
    private Object[] users() {
        return new Object[][] {
                // 测试点a:多个线程修改主库用户
                { false, false },
                // 测试点b:多个线程修改备库用户
                { true, true },
                // 测试点c:一个线程修改主库用户，一个线程修改备库用户
                { false, true } };
    }

    @Test(groups = { "twoSite", "fourSite" }, dataProvider = "data")
    public void test( boolean thread1User, boolean thread2User )
            throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( new UpdateWsUserThread( site, thread1User, access_keyA,
                passwdFilePathA ) );
        t.addWorker( new UpdateWsUserThread( site, thread2User, access_keyB,
                passwdFilePathB ) );
        t.run();

        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        ScmId scmId = file.save();
        SiteWrapper[] expSites = { site };
        ScmFileUtils.checkMetaAndData( wsName, scmId, expSites, localPath,
                filePath );
        file.delete( true );

        successTestCount.getAndIncrement();
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( successTestCount.get() == users().length
                    || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
                CephS3Utils.deleteCephS3User( site, uid, false );
                CephS3Utils.deleteCephS3User( site, uid, true );
                CephS3Utils.deletePasswdFile( site, passwdFilePathA );
                CephS3Utils.deletePasswdFile( site, passwdFilePathB );
            }
        } finally {
            ScmWorkspaceUtil.deleteWs( wsName, session );
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
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.WORKSPACE_CACHE_EXPIRE ) {
                    throw e;
                }
            } finally {
                session.close();
            }
        }
    }
}