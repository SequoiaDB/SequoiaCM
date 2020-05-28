package com.sequoiacm.scmfile.serial;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @FileName SCM-130:跨子表查询文件列表
 * @Author huangxiaoni
 * @Date 2017-05-24
 */

public class ListInstance130_MultiSubCL extends TestScmBase {
    private final int fileNum = 9;
    private final String fileName = "ListInstance130";
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = "test_130";
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private String author = fileName;
    private Calendar cal = Calendar.getInstance();

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            session = TestScmTools.createSession( site );
            ScmWorkspaceUtil.deleteWs( wsName, session );
            ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
            ScmWorkspaceUtil.wsSetPriority( session, wsName );
            prepareScmFile();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" }, enabled = false)
    private void test() {
        ScmSession session = null;
        ScmCursor< ScmFileBasicInfo > cursor = null;
        try {
            session = TestScmTools.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                    session );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( fileName ).get();
            cursor = ScmFactory.File.listInstance( ws, ScopeType.SCOPE_CURRENT,
                    cond );

            int size = 0;
            while ( cursor.hasNext() ) {
                cursor.getNext();
                size++;
            }
            Assert.assertEquals( size, fileNum );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( null != cursor ) {
                cursor.close();
            }
            if ( null != session ) {
                session.close();
            }
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            if ( runSuccess || forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
            }
            ScmWorkspaceUtil.deleteWs( wsName, session );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
    }

    private void prepareScmFile() throws ScmException {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        // create files from (current_year-fileNum) to current_year
        for ( int i = 0; i < fileNum; i++ ) {
            cal.set( Calendar.YEAR, cal.get( Calendar.YEAR ) - 1 );
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName + "_" + UUID.randomUUID() );
            file.setAuthor( author );
            file.setCreateTime( cal.getTime() );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        }
    }
}
