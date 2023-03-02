package com.sequoiacm.workspace.serial;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.exception.ScmException;
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
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.infrastructure.crypto.ScmPasswordMgr;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.dsutils.CephS3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @descreption SCM-5614:修改工作区配置用户信息
 * @author ZhangYanan
 * @date 2023/01/09
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5614 extends TestScmBase {
    private String wsName = "ws5614";
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private String access_key = null;
    private static String passwdFilePath = null;
    private File localPath = null;
    private ScmSession session = null;
    private final String uid = "uid5614";
    private int fileSize = 1024 * 1024;
    private String fileName = "file5614";
    private ScmWorkspace ws;
    private String filePath = null;
    private ArrayList< SiteWrapper > siteList = new ArrayList<>();

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = ScmInfo.getSiteByType( ScmType.DatasourceType.CEPH_S3 );
        siteList.add( site );

        passwdFilePath = site.getDataPasswd();
        access_key = site.getDataUser();

        session = TestScmTools.createSession( site );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        ScmId scmId = createFile( ws, filePath, fileName + "a", fileName );

        List< ScmDataLocation > dataLocation = prepareWsCephS3DataLocation(
                siteList, access_key, passwdFilePath, true );
        ws.updateDataLocation( dataLocation, true );
        ScmWorkspaceUtil.checkWsUpdate( session, wsName, dataLocation );

        SiteWrapper[] expSites = { site };
        ScmFileUtils.checkMetaAndData( wsName, scmId, expSites, localPath,
                filePath );

        scmId = createFile( ws, filePath, fileName + "b", fileName );
        ScmFileUtils.checkMetaAndData( wsName, scmId, expSites, localPath,
                filePath );

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
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