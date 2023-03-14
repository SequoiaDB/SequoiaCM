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
import com.sequoiacm.client.element.bizconf.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.crypto.ScmPasswordMgr;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.dsutils.CephS3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @descreption SCM-5604:创建工作区配置主库信息
 * @author ZhangYanan
 * @date 2023/01/09
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5604 extends TestScmBase {
    private String wsName = "ws5604";
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private final String access_key = "abcdefghijklmnopqrstuvwxyzabcdefghij5604";
    private static final String secret_key = "abcdefghijklmnopqrstuvwxyzabcdefghijklmn";
    private File localPath = null;
    private ScmSession session = null;
    private final String uid = "uid5604";
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

        ScmWorkspaceUtil.createWS( session,
                prepareWsConf( access_key, passwdFilePath, wsName ) );

        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
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
        ScmWorkspaceUtil.createWS( session,
                prepareWsConf( access_key, passwdErrorFilePath, wsName ) );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
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
        ScmWorkspaceUtil.createWS( session,
                prepareWsConf( access_key, passwdFilePath, wsName ) );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
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

    public ScmWorkspaceConf prepareWsConf( String access_key, String passwdPath,
            String wsName ) throws ScmInvalidArgumentException {
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setDataLocations( getDataLocationList( ScmInfo.getSiteNum(),
                access_key, passwdPath ) );
        conf.setMetaLocation(
                ScmWorkspaceUtil.getMetaLocation( ScmShardingType.YEAR ) );
        conf.setName( wsName );
        return conf;
    }

    public static List< ScmDataLocation > getDataLocationList( int siteNum,
            String access_key, String passwdFilePath )
            throws ScmInvalidArgumentException {
        SiteWrapper rootSite = ScmInfo.getRootSite();
        List< SiteWrapper > siteList = new ArrayList<>();
        List< ScmDataLocation > scmDataLocationList = new ArrayList<>();
        if ( siteNum > 1 ) {
            siteList = ScmInfo.getBranchSites( siteNum - 1 );
        } else if ( siteNum < 1 ) {
            throw new IllegalArgumentException(
                    "error, create ws siteNum can't equal " + siteNum );
        }
        siteList.add( rootSite );
        for ( int i = 0; i < siteList.size(); i++ ) {
            String siteName = siteList.get( i ).getSiteName();
            String dataType = siteList.get( i ).getDataType().toString();
            switch ( dataType ) {
            case "sequoiadb":
                String domainName = TestSdbTools
                        .getDomainNames( siteList.get( i ).getDataDsUrl() )
                        .get( 0 );
                scmDataLocationList
                        .add( new ScmSdbDataLocation( siteName, domainName ) );
                break;
            case "hbase":
                scmDataLocationList.add( new ScmHbaseDataLocation( siteName ) );
                break;
            case "hdfs":
                scmDataLocationList.add( new ScmHdfsDataLocation( siteName ) );
                break;
            case "ceph_s3":
                ScmCephS3UserConfig primaryConfig = new ScmCephS3UserConfig(
                        access_key, passwdFilePath );
                ScmCephS3DataLocation scmCephS3DataLocation = new ScmCephS3DataLocation(
                        siteName, primaryConfig );
                scmDataLocationList.add( scmCephS3DataLocation );
                break;
            case "ceph_swift":
                scmDataLocationList
                        .add( new ScmCephSwiftDataLocation( siteName ) );
                break;
            case "sftp":
                scmDataLocationList.add( new ScmSftpDataLocation( siteName ) );
                break;
            default:
                Assert.fail( "dataSourceType not match: " + dataType );
            }
        }
        return scmDataLocationList;
    }
}