package com.sequoiacm.scmfile.serial;

import java.util.ArrayList;
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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @FileName SCM-541 : 游标未关闭登出
 * @Author linsuqiang
 * @Date 2017-07-05
 * @Version 1.00
 */

/*
 * 1、写入多个文件 2、查询文件列表（当前返回游标） 3、游标不关闭，执行session.close登出，检查登出结果
 * 4、执行cursor.getNext继续获取文件信息，检查执行结果 5、在当前session继续操作，如getWorkspace，检查执行结果
 */

public class LoginOutWhenCursorOpen541 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private final int fileNum = 200;
    private final String fileName = "LoginOutWhenCursorOpen541";
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private String author = fileName;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = ScmSessionUtils.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( wsp, cond );

            prepareScmFile();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" }, enabled = false)
    private void test() throws Exception {
        try {
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( fileName ).get();
            ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File
                    .listInstance( ws, ScopeType.SCOPE_CURRENT, cond );
            cursor.getNext();
            session.close();
            try {
                while ( cursor.hasNext() ) {
                    cursor.getNext();
                }
                cursor.close();
                Assert.fail(
                        "cursor got next successfully when session is closed"
                                + "." );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.SESSION_CLOSED ) { //
                    // EN_SCM_SESSION_SESSION_CLOSED(-202)
                    throw e;
                }
            }

            try {
                ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
                Assert.fail(
                        "get workspace successfully when session is closed." );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.SESSION_CLOSED ) { //
                    // EN_SCM_SESSION_SESSION_CLOSED(-202)
                    throw e;
                }
            }

            runSuccess = true;
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void prepareScmFile() throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName + "_" + UUID.randomUUID() );
            file.setAuthor( author );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        }
    }

}