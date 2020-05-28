package com.sequoiacm.definemeta;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmClassProperties;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-1593:文件添加的模型id在当前ws不存在 SCM-1837:文件未定义模型直接添加自定义属性
 * @author huangxiaoni init
 * @date 2017.6.18
 */

public class DefineAttr_class_abnomal1593_1837 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( DefineAttr_class_abnomal1593_1837.class );
    private static final String NAME = "definemeta1593";
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private int failTimes = 0;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( NAME ).get();
        ScmFileUtils.cleanFile( wsp, cond );

        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( NAME );
        fileId = file.save();
    }

    @BeforeMethod
    private void initMethod() {
        if ( !runSuccess ) {
            failTimes++;
        }
        runSuccess = false;
    }

    @AfterMethod
    private void afterMethod() {
        if ( failTimes > 1 ) {
            runSuccess = false;
        }
    }

    @Test // SCM-1593
    private void test_classNotExist() throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties(
                "notExistClassId" );
        try {
            file.setClassProperties( properties );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info(
                    "classId not exist, errorMsg = [" + e.getError() + "]" );
        }
        runSuccess = true;
    }

    @Test // SCM-1837
    private void test_unsetClass() throws Exception {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( "test" );
        try {
            file.setClassProperty( "test", "test" );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "classId unSet, errorMsg = [" + e.getError() + "]" );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
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
}