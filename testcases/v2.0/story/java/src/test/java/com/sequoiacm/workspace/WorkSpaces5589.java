package com.sequoiacm.workspace;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @descreption SCM-5589:修改数据源分区规则与跨站点读文件验证
 * @author ZhangYanan
 * @date 2023/06/08
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5589 extends TestScmBase {
    private ScmSession rootSession = null;
    private ScmSession branSession = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branSite = null;
    private String wsName = "ws5589";
    private String fileName1 = "file5589_1";
    private String fileName2 = "file5589_2";
    private ArrayList< SiteWrapper > siteList = new ArrayList<>();
    private int fileSize = 1024 * 1024;
    private String filePath1 = null;
    private String filePath2 = null;
    private File localPath = null;
    private boolean runSuccess = false;
    private ScmWorkspace ws;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath1 = localPath + File.separator + "localFile1_" + fileSize
                + ".txt";
        filePath2 = localPath + File.separator + "localFile2_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath1, fileSize );
        TestTools.LocalFile.createFile( filePath2, fileSize );

        rootSite = ScmInfo.getRootSite();
        branSite = ScmInfo.getBranchSite();

        rootSession = ScmSessionUtils.createSession( rootSite );
        branSession = ScmSessionUtils.createSession( branSite );
        siteList.add( rootSite );

        ScmWorkspaceUtil.deleteWs( wsName, rootSession );
        ScmWorkspaceUtil.createWS( rootSession, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( rootSession, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, rootSession );
    }

    @Test(groups = { GroupTags.twoSite, GroupTags.fourSite })
    public void test() throws Exception {
        ScmId scmId = ScmFileUtils.create( ws, fileName1, filePath1 );
        List< ScmDataLocation > dataLocation = ScmWorkspaceUtil
                .prepareWsDataLocation( siteList, ScmShardingType.QUARTER );
        ws.updateDataLocation( dataLocation );
        ScmWorkspaceUtil.checkWsUpdate( rootSession, wsName, dataLocation );
        readFileFrom( branSite, filePath1, scmId );

        scmId = ScmFileUtils.create( ws, fileName2, filePath2 );
        readFileFrom( branSite, filePath2, scmId );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            ScmWorkspaceUtil.deleteWs( wsName, rootSession );
            if ( rootSession != null ) {
                rootSession.close();
            }
            if ( branSession != null ) {
                branSession.close();
            }
        }
    }

    public void readFileFrom( SiteWrapper site, String filePath, ScmId fileId )
            throws Exception {
        ScmSession session = null;
        try {
            session = ScmSessionUtils.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            file.getContent( downloadPath );
            Assert.assertEquals( TestTools.getMD5( filePath ),
                    TestTools.getMD5( downloadPath ) );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

}