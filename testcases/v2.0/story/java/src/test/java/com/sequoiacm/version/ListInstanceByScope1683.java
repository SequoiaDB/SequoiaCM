package com.sequoiacm.version;

import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @description SCM-1683:查询条件存在历史表以外的字段
 * @author wuyan
 * @createDate 2018.06.12
 * @updateUser ZhangYanan
 * @updateDate 2021.12.06
 * @updateRemark
 * @version v1.0
 */
public class ListInstanceByScope1683 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String fileName = "file1683";
    private String authorName = "author1683";
    private byte[] writedata = new byte[ 1024 * 1 ];
    private byte[] updatedata = new byte[ 1024 * 2 ];
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        fileId = ScmFileUtils.createFileByStream( ws, fileName, writedata,
                authorName );
        VersionUtils.updateContentByStream( ws, fileId, updatedata );
    }

    @Test
    private void test() throws Exception {
        listInstanceByHistoryVersion();
        listInstanceByAllVersion();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void listInstanceByHistoryVersion() throws ScmException {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName ).get();
        ScmCursor< ScmFileBasicInfo > cursor = null;
        try {
            cursor = ScmFactory.File.listInstance( ws, ScopeType.SCOPE_HISTORY,
                    condition );
            Assert.fail( "list history version file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.INVALID_ARGUMENT != e.getError() ) {
                Assert.fail( "expErrorCode:-101  actError:" + e.getError()
                        + e.getMessage() );
            }
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }

    private void listInstanceByAllVersion() throws ScmException {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmCursor< ScmFileBasicInfo > cursor = null;
        try {
            cursor = ScmFactory.File.listInstance( ws, ScopeType.SCOPE_ALL,
                    condition );
            Assert.fail( "list all version file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.INVALID_ARGUMENT != e.getError() ) {
                Assert.fail( "expErrorCode:-101  actError:" + e.getError()
                        + e.getMessage() );
            }
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }
}