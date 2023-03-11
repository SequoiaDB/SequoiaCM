package com.sequoiacm.workspace;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
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
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @descreption SCM-5926:工作区配置cephS3数据源主库用户未拥有原桶权限
 * @author ZhangYanan
 * @date 2023/01/09
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5926 extends TestScmBase {
    private String wsName = "ws5926";
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
    private ScmId updateFileId = null;
    private String fileName = "file5635_";
    private ScmWorkspace ws;
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

        // 准备初始用户和修改的用户
        prepareCephS3User();

        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {

        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( fileName + "update" );
        file.setAuthor( fileName );
        updateFileId = file.save();
        TestTools.LocalFile.createFile( fileUpdatePath, fileSize );

        List< ScmDataLocation > dataLocation = prepareWsCephS3DataLocation(
                siteList, access_key, passwdFilePath, false );
        ws.updateDataLocation( dataLocation );
        ScmWorkspaceUtil.checkWsUpdate( session, wsName, dataLocation );

        updateFile();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
                CephS3Utils.deleteCephS3User( site, uid, true );
                CephS3Utils.deletePasswdFile( site, passwdFilePath );
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

    public void updateFile() throws Exception {
        ScmSession session = TestScmTools.createSession( site );
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        try {
            ScmFile file = ScmFactory.File.getInstance( ws, updateFileId );
            file.updateContent( fileUpdatePath );
            Assert.fail( "预期失败，实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.DATA_WRITE_ERROR
                    .getErrorCode() ) {
                throw e;
            }
        } finally {
            session.close();
        }
    }

    public void prepareCephS3User() throws Exception {
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
    }
}