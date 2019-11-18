
package com.sequoiacm.rest;

import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;

/**
 * @Description:SCM-1098 :: 获取站点列表/获取站点信息
 * @author fanyu
 * @Date:2018年3月21日
 * @version:1.0
 */
public class GetSite1098 extends TestScmBase{
    private RestWrapper rest = null;
    private SiteWrapper site = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			site = ScmInfo.getRootSite();
			rest = new RestWrapper();
			rest.connect(site.getSiteServiceName(), TestScmBase.scmUserName,TestScmBase.scmPassword);
		} catch (HttpClientErrorException | JSONException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite","twoSite", "fourSite" })
	private void test() throws Exception {
		String response = rest.setRequestMethod(HttpMethod.GET)
				.setApi("sites?filter={uri}")
				.setUriVariables(new Object[]{ "{\"id\":{\"$exists\":1}}"})
				.setResponseType(String.class).exec().getBody().toString();
        JSONArray siteListInfo = new JSONArray(response);
		List<SiteWrapper> siteList = ScmInfo.getAllSites();
		Assert.assertEquals(siteList.size(), siteListInfo.length(),"wsListByRest = " + siteListInfo.toString()+
				",wsListByDb = " + siteList.toString());

		response = rest.setRequestMethod(HttpMethod.GET)
				.setApi("sites?filter={uri}")
				.setUriVariables(new Object[]{ "{\"id\":{\"$exists\":0}}"})
				.setResponseType(String.class).exec().getBody().toString();
		siteListInfo = new JSONArray(response);
		Assert.assertEquals(0, siteListInfo.length(), "no site should be returned");

		SiteWrapper site = ScmInfo.getRootSite();
		String response2 = rest.setApi("sites/" + site.getSiteName())
                .setRequestMethod(HttpMethod.GET)
                .setResponseType(String.class).exec().getBody().toString();
		JSONObject siteInfo = new JSONObject(response2).getJSONObject("site");
        //check 
		Assert.assertEquals(siteInfo.getString("name"), site.getSiteName());
		Assert.assertEquals(siteInfo.getInt("id"),site.getSiteId());
		JSONObject data = siteInfo.getJSONObject("data");
		Assert.assertEquals(data.getString("type"), site.getDataType().toString());
		Assert.assertEquals(data.getString("user"), site.getDataUser());
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		if(rest != null){
			rest.disconnect();
		}
	}
}

