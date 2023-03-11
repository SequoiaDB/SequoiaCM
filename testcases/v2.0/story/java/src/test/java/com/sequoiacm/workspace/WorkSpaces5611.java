package com.sequoiacm.workspace;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
import com.sequoiacm.infrastructure.crypto.ScmPasswordMgr;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.dsutils.CephS3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @descreption SCM-5611:指定修改方式修改工作区用户信息
 * @author ZhangYanan
 * @date 2023/01/09
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5611 extends TestScmBase {
    private String wsName = "ws5611";
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private final String access_key = "abcdefghijklmnopqrstuvwxyzabcdefghij5611";
    private static final String secret_key = "abcdefghijklmnopqrstuvwxyzabcdefghijklmn";
    private File localPath = null;
    private ScmSession session = null;
    private final String uid = "uid5611";
    private final String passwordFileName = uid + "testPwd.txt";
    private static String passwdFilePath = null;
    private int fileSize = 1024 * 1024;
    private String fileName = "file5611";
    private ScmWorkspace ws;
    private String filePath = null;
    private String passwdLocalPath = null;
    private ArrayList< SiteWrapper > siteList = new ArrayList<>();

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        passwdLocalPath = localPath + File.separator + "passwdFile1_" + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = ScmInfo.getSiteByType( ScmType.DatasourceType.CEPH_S3 );
        siteList.add( site );

        // 加密密码,后续写入文件中
        String cryptPassword = ScmPasswordMgr.getInstance()
                .encrypt( ScmPasswordMgr.SCM_CRYPT_TYPE_DES, secret_key );
        TestTools.LocalFile.createFileSpecifiedContent( passwdLocalPath,
                access_key + ":" + cryptPassword );

        CephS3Utils.deleteCephS3User( site, uid, false );
        CephS3Utils.deleteCephS3User( site, uid, true );
        CephS3Utils.createCephS3UserAndKey( site, uid, access_key, secret_key,
                false );
        CephS3Utils.createCephS3UserAndKey( site, uid, access_key, secret_key,
                true );
        passwdFilePath = CephS3Utils.preparePasswdFile( site, passwdLocalPath,
                passwordFileName );

        session = TestScmTools.createSession( site );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        // 修改前上传文件
        ScmId scmId = createFile( ws, filePath, fileName + "a", fileName );

        // 第一次修改，修改方式指定合并
        List< ScmDataLocation > dataLocation = prepareWsCephS3DataLocation(
                siteList, access_key, passwdFilePath, false );
        ws.updateDataLocation( dataLocation, true );
        ScmWorkspaceUtil.checkWsUpdate( session, wsName, dataLocation );

        SiteWrapper[] expSites = { site };
        ScmFileUtils.checkMetaAndData( wsName, scmId, expSites, localPath,
                filePath );

        scmId = createFile( ws, filePath, fileName + "b", fileName );
        ScmFileUtils.checkMetaAndData( wsName, scmId, expSites, localPath,
                filePath );

        scmId = createFile( ws, filePath, fileName + "c", fileName );
        // 第二次修改，修改方式指定覆盖
        dataLocation = prepareWsCephS3DataLocation( siteList, access_key,
                passwdFilePath, true );
        ws.updateDataLocation( dataLocation, false );
        ScmWorkspaceUtil.checkWsUpdate( session, wsName, dataLocation );

        ScmFileUtils.checkMetaAndData( wsName, scmId, expSites, localPath,
                filePath );

        scmId = createFile( ws, filePath, fileName + "d", fileName );
        ScmFileUtils.checkMetaAndData( wsName, scmId, expSites, localPath,
                filePath );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                CephS3Utils.deleteCephS3User( site, uid, false );
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

    public ScmId createFile( ScmWorkspace ws, String filePath, String fileName,
            String fileAuthor ) throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( fileName );
        file.setAuthor( fileAuthor );
        return file.save();
    }
}