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
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @descreption SCM-5609:修改工作区主库用户信息
 * @author ZhangYanan
 * @date 2023/01/09
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5609 extends TestScmBase {
    private String wsName = "ws5609";
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private final String access_key = "abcdefghijklmnopqrstuvwxyzabcdefghij5609";
    private static final String secret_key = "abcdefghijklmnopqrstuvwxyzabcdefghijklmn";
    private File localPath = null;
    private ScmSession session = null;
    private final String uid = "uid5609";
    private final String passwordFileName = uid + "testPwd.txt";
    private final String passwordErrorFileName = uid + "testErrorPwd.txt";
    private static String passwdFilePath = null;
    private static String passwdErrorFilePath = null;
    private int fileSize = 1024 * 1024;
    private String fileName = "file5604";
    private ScmWorkspace ws;
    private String filePath = null;
    private String passwdLocalPath = null;
    private String passwdErrorLocalPath = null;
    private ArrayList< SiteWrapper > siteList = new ArrayList<>();

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        passwdLocalPath = localPath + File.separator + "passwdFile1_" + ".txt";
        passwdErrorLocalPath = localPath + File.separator + "passwdFile2_"
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = ScmInfo.getSiteByType( ScmType.DatasourceType.CEPH_S3 );
        siteList.add( site );

        // 加密密码,后续写入文件中
        String cryptPassword = ScmPasswordMgr.getInstance()
                .encrypt( ScmPasswordMgr.SCM_CRYPT_TYPE_DES, secret_key );
        String cryptPasswordError = ScmPasswordMgr.getInstance()
                .encrypt( ScmPasswordMgr.SCM_CRYPT_TYPE_DES, "error" );
        TestTools.LocalFile.createFileSpecifiedContent( passwdLocalPath,
                access_key + ":" + cryptPassword );
        TestTools.LocalFile.createFileSpecifiedContent( passwdErrorLocalPath,
                access_key + ":" + cryptPasswordError );

        CephS3Utils.deleteCephS3User( site, uid, true );
        CephS3Utils.createCephS3UserAndKey( site, uid, access_key, secret_key,
                true );
        passwdFilePath = CephS3Utils.preparePasswdFile( site, passwdLocalPath,
                passwordFileName );
        passwdErrorFilePath = CephS3Utils.preparePasswdFile( site,
                passwdErrorLocalPath, passwordErrorFileName );

        session = ScmSessionUtils.createSession( site );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // test a : 主库用户密码正确
        testA();
        // test b : 主库用户密码错误
        testB();
        // test c : 主库用户不存在
        testC();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                CephS3Utils.deleteCephS3User( site, uid, true );
                CephS3Utils.deletePasswdFile( site, passwdFilePath );
                CephS3Utils.deletePasswdFile( site, passwdErrorFilePath );
            }
        } finally {
            ScmWorkspaceUtil.deleteWs( wsName, session );
            if ( session != null ) {
                session.close();
            }
        }
    }

    public void testA() throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        List< ScmDataLocation > dataLocation = prepareWsCephS3DataLocation(
                siteList, access_key, passwdFilePath );
        ws.updateDataLocation( dataLocation );
        ScmWorkspaceUtil.checkWsUpdate( session, wsName, dataLocation );

        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        ScmId scmId = file.save();

        SiteWrapper[] expSites = { site };
        ScmFileUtils.checkMetaAndData( wsName, scmId, expSites, localPath,
                filePath );
    }

    public void testB() throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        List< ScmDataLocation > dataLocation = prepareWsCephS3DataLocation(
                siteList, access_key, passwdErrorFilePath );
        ws.updateDataLocation( dataLocation );
        ScmWorkspaceUtil.checkWsUpdate( session, wsName, dataLocation );

        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        try {
            file.save();
            Assert.fail( "预期失败，实际成功" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.DATA_WRITE_ERROR
                    .getErrorCode() ) {
                throw e;
            }
        }
    }

    public void testC() throws Exception {
        CephS3Utils.deleteCephS3User( site, uid, true );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        List< ScmDataLocation > dataLocation = prepareWsCephS3DataLocation(
                siteList, access_key, passwdFilePath );
        ws.updateDataLocation( dataLocation );
        ScmWorkspaceUtil.checkWsUpdate( session, wsName, dataLocation );

        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        try {
            file.save();
            Assert.fail( "预期失败，实际成功" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.DATA_WRITE_ERROR
                    .getErrorCode() ) {
                throw e;
            }
        }
    }

    public static List< ScmDataLocation > prepareWsCephS3DataLocation(
            List< SiteWrapper > siteList, String access_key,
            String passwdFilePath ) throws ScmInvalidArgumentException {
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
                ScmCephS3DataLocation scmCephS3DataLocation = new ScmCephS3DataLocation(
                        siteName, primaryConfig );
                scmDataLocationList.add( scmCephS3DataLocation );
            }
        }
        return scmDataLocationList;
    }
}