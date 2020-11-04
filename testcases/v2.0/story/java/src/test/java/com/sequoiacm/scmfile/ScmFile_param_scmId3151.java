package com.sequoiacm.scmfile;

import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.TestScmBase;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Description: SCM-3151:ScmId参数校验
 * @author fanyu
 * @Date:2020年11月04日
 * @version:1.0
 */

public class ScmFile_param_scmId3151 extends TestScmBase {

    @BeforeClass(alwaysRun = true)
    private void setUp() {
    }

    @Test
    private void test1() throws ScmException {
        // 大于24位
        try {
            new ScmId("5fa0fd5a40000400542f157d1");
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if(e.getError() != ScmError.INVALID_ID){
                throw e;
            }
        }

        // 小于24位
        try {
            new ScmId("5fa0fd5a40000400542f157");
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if(e.getError() != ScmError.INVALID_ID){
                throw e;
            }
        }

        //  非十六进制
        try {
            new ScmId("5fa0fd5a40000400542f157k");
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if(e.getError() != ScmError.INVALID_ID){
                throw e;
            }
        }
    }

    @Test
    private void test2() throws ScmException {
        //  含特殊字符、中文、数字、英文
        String strId1 = "测试！@#￥……&*（）——=123456789";
        ScmId id1 = new ScmId( strId1,false);
        Assert.assertEquals( id1.get(), strId1);

        String strId2 = "";
        id1.setId( strId2 );
        Assert.assertEquals( id1.get(), strId2);

        // id为null
        try {
            new ScmId( null, false );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if(e.getError() != ScmError.INVALID_ID){
                throw e;
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
    }

}