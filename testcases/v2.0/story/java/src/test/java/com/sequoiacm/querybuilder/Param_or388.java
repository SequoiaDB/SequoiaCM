package com.sequoiacm.querybuilder;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.TestScmBase;

/**
 * @FileName SCM-388: or无效参数校验
 * @Author linsuqiang
 * @Date 2017-05-25
 * @Version 1.00
 */

/*
 * 1、or无效参数校验：Obj/key为null； 2、检查执行结果；
 */

public class Param_or388 extends TestScmBase {

    @BeforeClass(alwaysRun = true)
    private void setUp() {
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testObjIsNull() throws Exception {
        try {
            BSONObject obj = null;
            BSONObject cond = ScmQueryBuilder.start().or( obj ).get();
            Assert.assertEquals( cond.toString().replaceAll( "\\s*", "" ),
                    ( "{ \"$or\" : [  null ]}" ).replaceAll( "\\s*", "" ) );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testKeyIsNull() throws Exception {
        try {
            String key = null;
            BSONObject cond = ScmQueryBuilder.start( key ).or().get();
            Assert.fail(
                    "build condition when object is null shouldn't succeed. "
                            + "cond: " + cond );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
    }

}