package com.sequoiacm.datasource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.bson.BSONObject;
import org.testng.Assert;
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
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content: create file and read file from different site ,and the sites
 * connected to different datasource testlink-case:SCM-2097
 *
 * @author wuyan
 * @Date 2018.07.11
 * @version 1.00
 */

public class WriteAndReadFromDiffDatasource2097 extends TestScmBase {
    private static WsWrapper wsp = null;
    private static ScmSession sessionA = null;
    private static ScmSession sessionB = null;
    private static List< SiteWrapper > sites = new ArrayList<>();
    private boolean runSuccess = false;
    private ScmWorkspace wsA = null;
    private ScmWorkspace wsB = null;
    private ScmId fileId = null;
    private String fileName = "file2097";
    private String authorName = "file2097";
    private byte[] writeData = new byte[ 1024 * 1024 * 2 ];

    @BeforeClass
    private void setUp() throws ScmException {
        sites = ScmInfo.getAllSites();
        wsp = ScmInfo.getWs();
        sessionA = TestScmTools.createSession( sites.get( 0 ) );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionB = TestScmTools.createSession( sites.get( 1 ) );
        wsB = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionB );

        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
    }

    @Test(groups = { GroupTags.twoSite, GroupTags.fourSite })
    private void test() throws Exception {
        fileId = VersionUtils.createFileByStream( wsB, fileName, writeData,
                authorName );
        int majorVersion = 1;
        VersionUtils.CheckFileContentByStream( wsA, fileName, majorVersion,
                writeData );
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
            if ( sessionB != null ) {
                sessionB.close();
            }
        }
    }

    private int[] randomCommon( int max, int min, int n ) {
        if ( n > ( max - min + 1 ) || max < min ) {
            return null;
        }
        int[] result = new int[ n ];
        int count = 0;
        while ( count < n ) {
            int num = ( int ) ( Math.random() * ( max - min ) ) + min;
            boolean flag = true;
            for ( int j = 0; j < n; j++ ) {
                if ( num == result[ j ] ) {
                    flag = false;
                    break;
                }
            }
            if ( flag ) {
                result[ count ] = num;
                count++;
            }
        }
        return result;
    }
}