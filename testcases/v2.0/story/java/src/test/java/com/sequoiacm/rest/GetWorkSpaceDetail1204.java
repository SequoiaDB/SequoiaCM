package com.sequoiacm.rest;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpServerErrorException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName SCM-1204: 获取工作区详情
 * @Author linsuqiang
 * @Date 2018-04-11
 * @Version 1.00
 */

public class GetWorkSpaceDetail1204 extends TestScmBase {
	private RestWrapper rest = null;
    private SiteWrapper site = null;
	@BeforeClass(alwaysRun = true)
	private void setUp() throws Exception {
		site = ScmInfo.getRootSite();
        rest = new RestWrapper();
        rest.connect(site.getSiteServiceName(), TestScmBase.scmUserName,TestScmBase.scmPassword);
	}

	// TODO: function is not implemented yet.
	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() throws Exception {
		WsWrapper ws = ScmInfo.getWs();
		String response = rest.setRequestMethod(HttpMethod.GET)
				.setApi("workspaces/" + ws.getName())
				.setResponseType(String.class).exec().getHeaders().toString();
		// TODO: check result
		System.out.println(response);

		try {
			response = rest.setRequestMethod(HttpMethod.HEAD)
					.setApi("workspaces/inexistent_ws_name098345")
					.setResponseType(String.class).exec().getHeaders().toString();
		} catch (HttpServerErrorException e) {
			Assert.assertEquals(e.getStatusCode().value(), ScmError.HTTP_INTERNAL_SERVER_ERROR.getErrorCode(), e.getMessage());
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		if (rest != null) {
			rest.disconnect();
		}
	}
}
