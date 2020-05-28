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
 * @FileName SCM-385: start参数校验
 * @Author linsuqiang
 * @Date 2017-05-25
 * @Version 1.00
 */

/*
 * 1、start接口参数校验： 有效参数校验：空串； 无效参数校验：null； 2、检查查询结果
 */

public class Param_start385 extends TestScmBase {

    @BeforeClass(alwaysRun = true)
    private void setUp() {
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testQuery() throws Exception {
        try {
            BSONObject cond = ScmQueryBuilder.start( "" ).is( "value" ).get();
            Assert.assertEquals( cond.toString().replaceAll( "\\s*", "" ),
                    ( "{ \"\" : \"value\"}" ).replaceAll( "\\s*", "" ) );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        try {
            BSONObject cond = ScmQueryBuilder.start( null ).is( "value" ).get();
            Assert.fail( "build condition when key is null shouldn't succeed. "
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