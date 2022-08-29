package com.sequoiacm.datasource;

import java.io.IOException;
import java.util.List;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:ayncTransfer file,target site and source site data sources are
 * different testlink-case:SCM-2098
 * 
 * @author wuyan
 * @Date 2018.07.17
 * @version 1.00
 */

public class AsyncTransferFile2098 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmId fileId = null;

    private String fileName = "file2098";
    private byte[] filedata = new byte[ 1024 * 100 ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        int getSiteNums = 2;
        List< SiteWrapper > branSitelist = ScmInfo
                .getBranchSites( getSiteNums );
        rootSite = ScmInfo.getRootSite();
        DatasourceType rootSiteDataType = rootSite.getDataType();

        int dbDataSoureCount = 0;
        for ( int i = 0; i < branSitelist.size(); i++ ) {
            DatasourceType dataType = branSitelist.get( i ).getDataType();
            if ( !dataType.equals( rootSiteDataType ) ) {
                branSite = branSitelist.get( i );
                break;
            }
            dbDataSoureCount++;
        }
        if ( dbDataSoureCount == branSitelist.size() ) {
            throw new SkipException(
                    "all bransite are connected to sequoiadb datasourse, "
                            + "skip!" );
        }
        wsp = ScmInfo.getWs();
        sessionA = TestScmTools.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );

        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        fileId = VersionUtils.createFileByStream( wsA, fileName, filedata );
    }

    @Test(groups = { GroupTags.twoSite, GroupTags.fourSite })
    private void test() throws Exception {
        asyncTransferFile( wsA );

        // check the file data and siteinfo
        SiteWrapper[] expCurSiteList = { rootSite, branSite };
        int currentVersion = 1;
        VersionUtils.checkSite( wsA, fileId, currentVersion, expCurSiteList );
        VersionUtils.CheckFileContentByStream( wsM, fileName, currentVersion,
                filedata );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionM != null ) {
                sessionM.close();
            }
        }
    }

    private void asyncTransferFile( ScmWorkspace ws ) throws Exception {
        ScmFactory.File.asyncTransfer( ws, fileId, rootSite.getSiteName() );

        int sitenums = 2;
        ScmTaskUtils.waitAsyncTaskFinished( wsM, fileId, sitenums );
    }

}