package com.sequoiacm.net.datasource;

import java.io.IOException;
import java.util.List;

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
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:ayncTransfer file,target site and source site data sources
 * are different
 * testlink-case:SCM-2098 
 * @author wuyan
 * @Date 2018.07.17
 * @modify Date 2018.07.30
 * @version 1.10
 */

public class AsyncTransferFile2098 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper aysncTransferSite = null;
    private SiteWrapper targetSite = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionT = null;
    private ScmWorkspace wsT = null;
    private ScmId fileId = null;

    private String fileName = "file2098";
    private byte[] filedata = new byte[ 1024 * 100 ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        wsp = ScmInfo.getWs();
        List< SiteWrapper > siteList = ScmNetUtils.getSortSites( wsp );
        int dbDataSoureCount = 0;
        for ( int i = 0; i < siteList.size() - 1; i++ ) {
            aysncTransferSite = siteList.get( i );
            targetSite = siteList.get( i + 1 );
            DatasourceType sourceDataType = aysncTransferSite.getDataType();
            DatasourceType targetDataType = targetSite.getDataType();

            if ( !sourceDataType.equals( targetDataType ) ) {
                break;
            }
            dbDataSoureCount++;
        }

        if ( dbDataSoureCount == siteList.size() - 1 ) {
            throw new SkipException(
                    "target and source site are connected to same datasourse," +
                            " skip!" );
        }

        sessionA = TestScmTools.createSession( aysncTransferSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionT = TestScmTools.createSession( targetSite );
        wsT = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionT );

        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        fileId = VersionUtils.createFileByStream( wsA, fileName, filedata );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        asyncTransferFile( wsA );

        // check the file data and siteinfo
        SiteWrapper[] expCurSiteList = { targetSite, aysncTransferSite };
        int currentVersion = 1;
        VersionUtils.checkSite( wsA, fileId, currentVersion, expCurSiteList );
        VersionUtils.CheckFileContentByStream( wsT, fileName, currentVersion,
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
            if ( sessionT != null ) {
                sessionT.close();
            }
        }
    }

    private void asyncTransferFile( ScmWorkspace ws ) throws Exception {
        ScmFactory.File.asyncTransfer( ws, fileId );

        int sitenums = 2;
        ScmTaskUtils.waitAsyncTaskFinished( wsT, fileId, sitenums );
    }

}