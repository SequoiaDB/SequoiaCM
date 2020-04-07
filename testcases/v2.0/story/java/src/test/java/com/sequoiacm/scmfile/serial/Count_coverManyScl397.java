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
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @FileName SCM-397: 统计多个子表下的文件
 * @Author linsuqiang
 * @Date 2017-05-24
 * @Version 1.00
 */

/*
 * 1、带条件统计多个子表下的文件； 2、检查统计结果正确性；
 * 
 * 备注： 1.这个用例不兼容windows系统。因为设置系统时间的操作在windows测试机是没有权限的。
 */

public class Count_coverManyScl397 extends TestScmBase {
    private static SiteWrapper site = null;
    private final int fileNum = 4;
    private final String fileName = "file397";
    private boolean runSuccess = false;
    private ScmSession session = null;
    private String wsName = "test_397";
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
            Thread.sleep( 10000 );
            prepareScmFile();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( wsName, session );

            BSONObject fileCond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            long actCount = ScmFactory.File
                    .countInstance( ws, ScopeType.SCOPE_CURRENT, fileCond );
            Assert.assertEquals( actCount, fileNum, "wrong file count" );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( wsName, session );
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
        //create files from (current_year-fileNum) to current_year
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
