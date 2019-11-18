package com.sequoiacm.rest;

import org.springframework.http.HttpMethod;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName SCM-2177:元数据增删改查接口测试
 * @Author fanyu
 * @Date 2018-04-11
 * @Version 1.00
 */

public class CrudDefindMeta2177 extends TestScmBase {
	private RestWrapper rest = null;
	private SiteWrapper site = null;
	private WsWrapper wsp = null;
	private String name = "CrudDefindMeta2177";
	private String classId = null;
	private String attrId = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() throws Exception {
		site = ScmInfo.getSite();
		wsp = ScmInfo.getWs();
		rest = new RestWrapper();
		rest.connect(site.getSiteServiceName(), TestScmBase.scmUserName, TestScmBase.scmPassword);
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() throws Exception {
		// create class
		JSONObject desc1 = new JSONObject();
		desc1.put("name", name);
		desc1.put("description", name);
		String response = rest.setRequestMethod(HttpMethod.POST).setApi("/metadatas/classes")
				.setParameter("workspace_name", wsp.getName()).setParameter("description", desc1.toString())
				.setResponseType(String.class).exec().getBody().toString();
		classId = new JSONObject(response).getString("id");

		// list class
		JSONObject desc2 = new JSONObject();
		desc2.put("name", name);
		String response1 = rest.setRequestMethod(HttpMethod.GET)
				.setApi("/metadatas/classes?workspace_name=" + wsp.getName() + "&filter={uri}")
				.setUriVariables(new Object[] { desc2.toString() }).setResponseType(String.class).exec().getBody()
				.toString();
		JSONArray infoArr = new JSONArray(response1);
		Assert.assertEquals(infoArr.length(), 1);

		// get class
		String response2 = rest.setRequestMethod(HttpMethod.GET)
				.setApi("/metadatas/classes/" + classId + "?workspace_name=" + wsp.getName())
				.setResponseType(String.class).exec().getBody().toString();
		JSONObject classInfo = new JSONObject(response2);
		Assert.assertEquals(classInfo.getString("name"), name);
		Assert.assertEquals(classInfo.getString("description"), name);

		// update class
		JSONObject desc3 = new JSONObject();
		// desc3.put("name", name);
		desc3.put("description", name + "_update");
		rest.setRequestMethod(HttpMethod.PUT)
				.setApi("/metadatas/classes/" + classId + "?workspace_name=" + wsp.getName() + "&description={uri}")
				.setUriVariables(new Object[] { desc3.toString() }).setResponseType(String.class).exec();
		// System.out.println("response3 = " + response3);
		String response4 = rest.setRequestMethod(HttpMethod.GET)
				.setApi("/metadatas/classes/" + classId + "?workspace_name=" + wsp.getName())
				.setResponseType(String.class).exec().getBody().toString();
		JSONObject classInfo1 = new JSONObject(response4);
		Assert.assertEquals(classInfo.getString("name"), name);
		Assert.assertEquals(classInfo1.getString("description"), name + "_update");

		// create attr
		JSONObject desc4 = new JSONObject();
		desc4.put("name", name);
		desc4.put("display_name", name);
		desc4.put("description", name);
		desc4.put("type", AttributeType.BOOLEAN);
		desc4.put("required", false);
		String response3 = rest.setRequestMethod(HttpMethod.POST).setApi("/metadatas/attrs")
				.setParameter("workspace_name", wsp.getName()).setParameter("description", desc4.toString())
				.setResponseType(String.class).exec().getBody().toString();
		attrId = new JSONObject(response3).getString("id");
		// System.out.println("classId = " + classId);

		// list attr
		JSONObject desc5 = new JSONObject();
		desc5.put("name", name);
		String response5 = rest.setRequestMethod(HttpMethod.GET)
				.setApi("/metadatas/attrs?workspace_name=" + wsp.getName() + "&filter={uri}")
				.setUriVariables(new Object[] { desc5.toString() }).setResponseType(String.class).exec().getBody()
				.toString();
		JSONArray infoArr1 = new JSONArray(response5);
		Assert.assertEquals(infoArr1.length(), 1);

		// get Attr
		String response6 = rest.setRequestMethod(HttpMethod.GET)
				.setApi("/metadatas/attrs/" + attrId + "?workspace_name=" + wsp.getName()).setResponseType(String.class)
				.exec().getBody().toString();
		JSONObject attrInfo = new JSONObject(response6);
		Assert.assertEquals(attrInfo.getString("name"), name);
		Assert.assertEquals(attrInfo.getString("description"), name);

		// update
		JSONObject desc6 = new JSONObject();
		// desc3.put("name", name);
		desc6.put("description", name + "_update");
		rest.setRequestMethod(HttpMethod.PUT)
				.setApi("/metadatas/attrs/" + attrId + "?workspace_name=" + wsp.getName() + "&description={uri}")
				.setUriVariables(new Object[] { desc6.toString() }).setResponseType(String.class).exec();
		JSONObject attrInfo1 = getAttrInfo();
		Assert.assertEquals(attrInfo1.getString("name"), name);
		Assert.assertEquals(attrInfo1.getString("description"), name + "_update");

		// class attach attr
	    rest.setRequestMethod(HttpMethod.PUT)
				.setApi("/metadatas/classes/" + classId + "/attachattr/" + attrId + "?workspace_name=" + wsp.getName())
				.setResponseType(String.class).exec();
		JSONObject classInfo2 = getClassInfo();
		Assert.assertEquals(new JSONArray(classInfo2.getString("attrs")).length(), 1);

		// class detach attr	
		rest.setRequestMethod(HttpMethod.PUT)
			.setApi("/metadatas/classes/" + classId + "/detachattr/" + attrId + "?workspace_name=" + wsp.getName())
			.setResponseType(String.class).exec();
	    JSONObject classInfo3 = getClassInfo();
	    Assert.assertEquals(new JSONArray(classInfo3.getString("attrs")).length(), 0);
	    
	    //delete class
		rest.setRequestMethod(HttpMethod.DELETE)
		.setApi("/metadatas/classes/" + classId + "?workspace_name=" + wsp.getName())
		.setResponseType(String.class).exec();
		
		//delete attr
		rest.setRequestMethod(HttpMethod.DELETE)
		.setApi("/metadatas/attrs/" + attrId + "?workspace_name=" + wsp.getName())
		.setResponseType(String.class).exec();
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		if (rest != null) {
			rest.disconnect();
		}
	}

	private JSONObject getClassInfo() throws JSONException {
		String response2 = rest.setRequestMethod(HttpMethod.GET)
				.setApi("/metadatas/classes/" + classId + "?workspace_name=" + wsp.getName())
				.setResponseType(String.class).exec().getBody().toString();
		JSONObject classInfo = new JSONObject(response2);
		return classInfo;
	}

	private JSONObject getAttrInfo() throws JSONException {
		String response6 = rest.setRequestMethod(HttpMethod.GET)
				.setApi("/metadatas/attrs/" + attrId + "?workspace_name=" + wsp.getName()).setResponseType(String.class)
				.exec().getBody().toString();
		JSONObject attrInfo = new JSONObject(response6);
		return attrInfo;
	}
}
