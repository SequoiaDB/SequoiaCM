package com.sequoiacm.workspace;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @descreption SCM-5590:多次修改数据源分区规则，上传下载文件
 * @author ZhangYanan
 * @date 2023/06/08
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5590 extends TestScmBase {
    private ScmSession rootSession = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branSite = null;
    private String wsName = "ws5590";
    private String fileName1 = "file5590_1";
    private String fileName2 = "file5590_2";
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

        rootSession = ScmSessionUtils.createSession( rootSite );
        siteList.add( rootSite );

        ScmWorkspaceUtil.deleteWs( wsName, rootSession );
        ScmWorkspaceUtil.createWS( rootSession, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( rootSession, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, rootSession );
    }

    @Test
    public void test() throws Exception {
        SiteWrapper[] expSites = { rootSite };
        ScmId fileId = ScmFileUtils.create( ws, fileName1, filePath1 );

        List< ScmDataLocation > dataLocation = ScmWorkspaceUtil
                .prepareWsDataLocation( siteList, ScmShardingType.QUARTER );
        ws.updateDataLocation( dataLocation );
        ScmWorkspaceUtil.checkWsUpdate( rootSession, wsName, dataLocation );

        ScmFileUtils.checkMetaAndData( wsName, fileId, expSites, localPath,
                filePath1 );

        dataLocation = ScmWorkspaceUtil.prepareWsDataLocation( siteList,
                ScmShardingType.MONTH );
        ws.updateDataLocation( dataLocation );
        ScmWorkspaceUtil.checkWsUpdate( rootSession, wsName, dataLocation );

        ScmFileUtils.checkMetaAndData( wsName, fileId, expSites, localPath,
                filePath1 );

        fileId = ScmFileUtils.create( ws, fileName2, filePath2 );
        ScmFileUtils.checkMetaAndData( wsName, fileId, expSites, localPath,
                filePath2 );

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
        }
    }
}