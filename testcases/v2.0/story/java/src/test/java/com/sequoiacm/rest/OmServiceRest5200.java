/**
 *
 */
package com.sequoiacm.rest;

import org.springframework.http.HttpMethod;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.alibaba.fastjson.JSON;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;

/**
 * @description SCM-5200:rest请求验证om服务状态
 * @author ZhangYanan
 * @createDate 2022.09.06
 * @updateUser ZhangYanan
 * @updateDate 2022.09.06
 * @updateRemark
 * @version v1.0
 */
public class OmServiceRest5200 extends TestScmBase {
    private RestWrapper rest = null;

    @BeforeClass()
    private void setUp() {
        rest = new RestWrapper();
    }

    @Test(groups = { GroupTags.oneSite, GroupTags.twoSite, GroupTags.fourSite })
    private void test() {
        String wResponse = rest.setRequestMethod( HttpMethod.GET )
                .setResponseType( String.class ).setServerType( "om-server" )
                .exec().getBody().toString();
        Assert.assertEquals( JSON.parseObject( wResponse ).get( "status" ),
                "UP" );

    }

    @AfterClass()
    private void tearDown() throws Exception {
        if ( rest != null ) {
            rest.disconnect();
        }
    }
}