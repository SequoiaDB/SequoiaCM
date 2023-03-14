package com.sequoiacm.version;

import java.io.File;
import java.io.IOException;

import com.sequoiacm.client.core.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @description SCM-1650:跨站点读取更新文件内容
 * @author wuyan
 * @createDate 2018.06.04
 * @updateUser ZhangYanan
 * @updateDate 2021.12.06
 * @updateRemark
 * @version v1.0
 */
public class UpdateContent1650 extends TestScmBase {
    private static WsWrapper wsp = null;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionB = null;
    private ScmWorkspace wsB = null;
    private ScmId fileId = null;
    private boolean runSuccess = false;
    private String fileName = "file1650";
    private int writeSize = 1024 * 500;
    private int updateSize = 1024 * 1024 * 2;
    private byte[] filedata = new byte[ writeSize ];
    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + updateSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, updateSize );

        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();

        sessionA = ScmSessionUtils.createSession( rootSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionB = ScmSessionUtils.createSession( branSite );
        wsB = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionB );
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        // write and updateContent from siteA
        fileId = ScmFileUtils.createFileByStream( wsA, fileName, filedata );
        VersionUtils.updateContentByFile( wsA, fileName, fileId, filePath );

        // download file from siteB and check result
        int currentVersion = 2;
        int historyVersion = 1;
        // check fileContent
        VersionUtils.CheckFileContentByStream( wsA, fileName, historyVersion,
                filedata );
        VersionUtils.CheckFileContentByFile( wsB, fileName, currentVersion,
                filePath, localPath );

        // check the sitelist/currentversion/size
        SiteWrapper[] expSiteList = { rootSite, branSite,};
        SiteWrapper[] expSiteListA = { rootSite };
        VersionUtils.checkSite( wsA, fileId, currentVersion, expSiteList );
        VersionUtils.checkSite( wsA, fileId, historyVersion, expSiteListA );
        VersionUtils.checkFileCurrentVersion( wsA, fileId, currentVersion );
        VersionUtils.checkFileSize( wsA, fileId, currentVersion, updateSize );
        VersionUtils.checkFileSize( wsA, fileId, historyVersion, writeSize );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( wsB, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionB != null ) {
                sessionB.close();
            }
        }
    }
}