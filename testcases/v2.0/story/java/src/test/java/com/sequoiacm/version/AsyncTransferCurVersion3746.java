package com.sequoiacm.version;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.io.File;
import java.io.IOException;

/**
 * @description SCM-3746:异步迁移当前版本文件，源站点为主站点，目标站点为分站点
 * @author YiPan
 * @createDate 2021.09.10
 * @updateUser ZhangYanan
 * @updateDate 2021.12.06
 * @updateRemark
 * @version v1.0
 */
public class AsyncTransferCurVersion3746 extends TestScmBase {
    private static final String fileName = "file3746";
    private int fileSize = 1024;
    private int maxVersion = 3;
    private File localPath = null;
    private String filePath = null;
    private String updatefilePath = null;
    private static WsWrapper wsp = null;
    private SiteWrapper branchSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession rootSiteSession = null;
    private ScmWorkspace rootSiteWs = null;
    private ScmId fileId = null;
    private BSONObject queryCond;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws ScmException, IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        updatefilePath = localPath + File.separator + "updateFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( updatefilePath, fileSize * 2 );

        branchSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();
        rootSiteSession = TestScmTools.createSession( rootSite );
        rootSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootSiteSession );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        fileId = VersionUtils.createFileByFile( rootSiteWs, fileName,
                filePath );
        for ( int i = 0; i < maxVersion - 1; i++ ) {
            VersionUtils.updateContentByFile( rootSiteWs, fileName, fileId,
                    updatefilePath );
        }

        ScmFactory.File.asyncTransfer( rootSiteWs, fileId, maxVersion, 0,
                branchSite.getSiteName() );
        VersionUtils.waitAsyncTaskFinished( rootSiteWs, fileId, maxVersion, 2 );
        SiteWrapper[] expCurrentFileSiteList = { rootSite, branchSite };
        VersionUtils.checkSite( rootSiteWs, fileId, maxVersion,
                expCurrentFileSiteList );
        SiteWrapper[] expHistoryFileSiteList = { rootSite };
        VersionUtils.checkSite( rootSiteWs, fileId, maxVersion - 1,
                expHistoryFileSiteList );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( rootSiteSession != null ) {
                rootSiteSession.close();
            }
        }
    }

}