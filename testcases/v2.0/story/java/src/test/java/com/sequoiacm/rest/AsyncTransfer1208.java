package com.sequoiacm.rest;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLDecoder;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName SCM-1208: 异步迁移文件
 * @Author linsuqiang
 * @Date 2018-04-11
 * @Version 1.00
 */

public class AsyncTransfer1208 extends TestScmBase {
	private RestWrapper rest = null;
	private WsWrapper ws = null;
	private File localPath = null;
	private String filePath = null;
	private final String authorName = "这是一个中文名1208";
	private String fileId = null;
	private SiteWrapper site = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() throws Exception {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile.txt";
		TestTools.LocalFile.removeFile(localPath);
		TestTools.LocalFile.createDir(localPath.toString());
		TestTools.LocalFile.createFile(filePath, 0);

		ws = ScmInfo.getWs();
		site = ScmInfo.getBranchSite();
        rest = new RestWrapper();
        rest.connect(site.getSiteServiceName(), TestScmBase.scmUserName,TestScmBase.scmPassword);
		fileId = createFile(ws, filePath);
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws Exception {
		String response = rest.setRequestMethod(HttpMethod.POST)
				.setApi("files/" + fileId + "/async-transfer?workspace_name=" + ws.getName())
				.setResponseType(String.class).exec().getBody().toString();
		Assert.assertEquals("\"\"", response);
		checkAsyncTransfer(rest, ws, fileId);

		try {
			String inexistentId = "ffffffffffffffff";
			response = rest.setRequestMethod(HttpMethod.POST)
					.setApi("files/" + inexistentId + "/async-transfer?workspace_name=" + ws.getName())
					.setResponseType(String.class).exec().getBody().toString();
			Assert.fail("async-transfer should not succeed");
		} catch (HttpClientErrorException e) {
			Assert.assertEquals(e.getStatusCode().value(), 400);
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
        deleteFile(ws, fileId);
		if (rest != null) {
			rest.disconnect();
		}
	}

	private String createFile(WsWrapper ws, String filePath) throws Exception {
		JSONObject desc = new JSONObject();
		desc.put("name", this.getClass().getSimpleName() + UUID.randomUUID());
		desc.put("author", authorName);
		desc.put("title", authorName);
		desc.put("mime_type", "text/plain");
		
		File file = new File(filePath);
		//FileSystemResource resource = new FileSystemResource(file);
		String wResponse = rest.setRequestMethod(HttpMethod.POST)
				.setApi("files?workspace_name=" + ws.getName())
				//.setParameter("file", resource)
				//.setParameter("description", desc.toString())
				.setRequestHeaders("description", desc.toString())
	            .setInputStream(new FileInputStream(file))
				.setResponseType(String.class).exec().getBody().toString();
		String fileId = JSON.parseObject(wResponse).getJSONObject("file").getString("id");
		return fileId;
	}

	private void deleteFile(WsWrapper ws, String fileId) {
		rest.setRequestMethod(HttpMethod.DELETE)
				.setApi("files/" + fileId + "?workspace_name=" + ws.getName() + "&is_physical=true")
				.setResponseType(Resource.class).exec();
	}

	private void checkAsyncTransfer(RestWrapper rest, WsWrapper ws, String fileId) throws Exception {
		int times = 120;
		int intervalSec = 1;
		boolean checkOk = false;
		for (int i = 0; i < times; ++i) {
            String response = rest.setRequestMethod(HttpMethod.HEAD)
                    .setApi("files/id/" + fileId + "?workspace_name=" + ws.getName())
                    .setResponseType(String.class).exec().getHeaders()
                    .get("file").toString();
            response = URLDecoder.decode(response, "UTF-8");
            JSONObject fileInfo = JSON.parseArray(response).getJSONObject(0);
            JSONArray siteList = (JSONArray) fileInfo.get("site_list");
            if (siteList.size() >= 2) {
                checkOk = true;
                break;
            } else {
				Thread.sleep(intervalSec * 1000);
			}
		}
		if (!checkOk) {
			throw new Exception("fail to asynctransfer");
		}
	}
}
