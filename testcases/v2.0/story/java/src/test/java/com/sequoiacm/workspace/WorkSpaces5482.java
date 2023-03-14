package com.sequoiacm.workspace;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @descreption SCM-5482:修改数据源分区规则与文件指定id下载验证
 * @author ZhangYanan
 * @date 2022/11/29
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5482 extends TestScmBase {
    private ScmSession session = null;
    private SiteWrapper site = null;
    private String wsName = "ws5482";
    private String fileName1 = "file5482_1";
    private String fileName2 = "file5482_2";
    private ArrayList< SiteWrapper > siteList = new ArrayList<>();
    private int fileSize = 1024 * 1024;
    private String filePath1 = null;
    private String filePath2 = null;
    private File localPath = null;
    private ScmId scmId = null;
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

        site = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( site );
        siteList.add( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        scmId = uploadFileByScmId( fileName1, filePath1 );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite", GroupTags.base })
    public void test() throws Exception {

        List< ScmDataLocation > dataLocation = ScmWorkspaceUtil
                .prepareWsDataLocation( siteList, ScmShardingType.YEAR );
        ws.updateDataLocation( dataLocation );
        ScmWorkspaceUtil.checkWsUpdate( session, wsName, dataLocation );

        SiteWrapper[] expSites = { site };
        ScmFileUtils.checkMetaAndData( wsName, scmId, expSites, localPath,
                filePath1 );

        scmId = uploadFileByScmId( fileName2, filePath2 );
        ScmFileUtils.checkMetaAndData( wsName, scmId, expSites, localPath,
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
            ScmWorkspaceUtil.deleteWs( wsName, session );
            if ( session != null ) {
                session.close();
            }
        }
    }

    public ScmId uploadFileByScmId( String fileName, String filePath )
            throws ScmException {
        ScmId scmId = new ScmId( ScmFileUtils.getFileIdByDate( new Date() ) );

        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileId( scmId );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        file.save();
        return scmId;
    }
}