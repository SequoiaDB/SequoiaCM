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
 * @FileName SCM-387: and无效参数校验
 * @Author linsuqiang
 * @Date 2017-05-25
 * @Version 1.00
 */

/*
 * 1、and接口无效参数校验：obj/key为null 2、检查查询结果
 */

public class Param_and387 extends TestScmBase {

    @BeforeClass(alwaysRun = true)
    private void setUp() {
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testObjIsNull() throws Exception {
        try {
            BSONObject obj = null;
            BSONObject cond = ScmQueryBuilder.start().and( obj ).get();
            Assert.assertEquals( cond.toString().replaceAll( "\\s*", "" ),
                    ( "{ \"$and\" : [  null ]}" ).replaceAll( "\\s*", "" ) );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testKeyIsNull() throws Exception {
        try {
            String key = null;
            BSONObject cond = ScmQueryBuilder.start().and( key ).get();
            Assert.fail( "build condition when key(String) is null shouldn't "
                    + "succeed. cond: " + cond );
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