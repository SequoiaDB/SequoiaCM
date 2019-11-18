package com.sequoiacm.rest;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.UUID;

/**
 * @FileName SCM-1329: 创建批次
 *           SCM-1330: 查询批次
 *           SCM-1331: 获取批次
 *           SCM-1332: 更新批次属性
 *           SCM-1333: 删除批次
 *           SCM-1334: 批次中添加文件
 *           SCM-1335: 批次中解除文件
 *           SCM-1336: 使用错误的请求参数创建批次
 * @Author linsuqiang
 * @Date 2018-04-20
 * @Version 1.00
 */

public class OprBatch1329 extends TestScmBase {
	private boolean runSuccess = false;
	private File localPath = null;
	private String filePath = null;
	private int fileSize = 0;
	private String testcaseName = "OprBatch1329";
	private String fileId = null;
	private WsWrapper ws = null;
	private RestWrapper rest = null;
	private SiteWrapper site = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() throws Exception {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		TestTools.LocalFile.removeFile(localPath);
		TestTools.LocalFile.createDir(localPath.toString());
		TestTools.LocalFile.createFile(filePath, fileSize);

		ws = ScmInfo.getWs();
		site = ScmInfo.getRootSite();
		rest = new RestWrapper();
		rest.connect(site.getSiteServiceName(), TestScmBase.scmUserName,TestScmBase.scmPassword);

        JSONObject desc = new JSONObject();
        desc.put("name", testcaseName +UUID.randomUUID());
		desc.put("author", testcaseName);
		desc.put("title", testcaseName);
		desc.put("mime_type", "text/plain");
        fileId = upload(filePath, ws, desc.toString());
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() throws Exception {
		// SCM-1329: create batch
		String response = rest.setRequestMethod(HttpMethod.POST)
				.setApi("batches?workspace_name=" + ws.getName() + "&description={uri}")
				.setUriVariables(new Object[]{ "{\"name\":\"" + testcaseName + "\", \"tags\":[\"old_value\"]}" })
				.setResponseType(String.class).exec().getBody().toString();
		String batchId = new JSONObject(response).getJSONObject("batch").getString("id");

		// SCM-1330: list batch
		response = rest.setRequestMethod(HttpMethod.GET)
				.setApi("batches?workspace_name=" + ws.getName() + "&filter={uri}")
                .setUriVariables(new Object[]{ "{\"name\":\"" + testcaseName + "\"}" })
				.setResponseType(String.class).exec().getBody().toString();
		JSONObject batchInfo = new JSONArray(response).getJSONObject(0);
		Assert.assertEquals(batchInfo.getString("name"), testcaseName);

		// SCM-1332: update batch
		rest.setRequestMethod(HttpMethod.PUT)
				.setApi("batches/" + batchId + "?workspace_name=" + ws.getName() + "&description={uri}")
				.setUriVariables(new Object[] { "{\"tags\":[\"new_value\"]}" })
				.setResponseType(String.class).exec();
		batchInfo = getBatchInfo(batchId);
		Assert.assertEquals(batchInfo.getString("name"), testcaseName);
		Assert.assertEquals(batchInfo.getString("tags").toString(),
				"[\"new_value\"]");

		// SCM-1334: attach file
		rest.setRequestMethod(HttpMethod.POST)
				.setApi("batches/" + batchId + "/attachfile")
				.setParameter("workspace_name", ws.getName())
				.setParameter("file_id", fileId)
				.setResponseType(String.class).exec();
		batchInfo = getBatchInfo(batchId);
		Assert.assertEquals(batchInfo.getJSONArray("files").length(), 1);

		// SCM-1335: detach file
		rest.setRequestMethod(HttpMethod.POST)
				.setApi("batches/" + batchId + "/detachfile")
				.setParameter("workspace_name", ws.getName())
				.setParameter("file_id", fileId)
				.setResponseType(String.class).exec();
		batchInfo = getBatchInfo(batchId);
		Assert.assertEquals(batchInfo.getJSONArray("files").length(), 0);

		// SCM-1335: delete batch
		rest.setRequestMethod(HttpMethod.DELETE)
				.setApi("batches/" + batchId + "?workspace_name=" + ws.getName())
				.setResponseType(String.class).exec();
		try {
			getBatchInfo(batchId);
			Assert.fail("batch should not exist");
		} catch (HttpServerErrorException e) {
		    Assert.assertEquals(e.getStatusCode().value(), ScmError.HTTP_INTERNAL_SERVER_ERROR.getErrorCode(), e.getMessage());
		}

		// SCM-1336: wrong request
        try {
			rest.setRequestMethod(HttpMethod.POST)
					.setApi("batches?workspace_name=" + ws.getName() + "&desc={uri}")
					.setUriVariables(new Object[]{"{\"name\":\"" + testcaseName + "\"}"})
					.setResponseType(String.class).exec();
			Assert.fail("creating batch with wrong parameter should not succeed");
		} catch (HttpClientErrorException|HttpServerErrorException e) {
			Assert.assertEquals(e.getStatusCode().value(), ScmError.HTTP_BAD_REQUEST.getErrorCode(), e.getMessage());
		}
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		try {
			if (runSuccess || TestScmBase.forceClear) {
                rest.setRequestMethod(HttpMethod.DELETE)
						.setApi("files/" + fileId + "?workspace_name=" + ws.getName()+"&is_physical=true")
                        .setResponseType(String.class).exec();
				TestTools.LocalFile.removeFile(localPath);
			}
		} finally {
			if (rest != null) {
				rest.disconnect();
			}
		}
	}

	private String upload(String filePath, WsWrapper ws, String desc)
			throws HttpClientErrorException, JSONException, FileNotFoundException {
		File file = new File(filePath);
		//FileSystemResource resource = new FileSystemResource(file);
		String wResponse = rest.setApi("files?workspace_name=" + ws.getName())
				.setRequestMethod(HttpMethod.POST)
				//.setParameter("file", resource)
				//.setParameter("description", desc)
				.setRequestHeaders("description", desc.toString())
	            .setInputStream(new FileInputStream(file))
				.setResponseType(String.class).exec().getBody().toString();
		String fileId = new JSONObject(wResponse).getJSONObject("file").getString("id");
		return fileId;
	}

	private JSONObject getBatchInfo(String batchId) throws JSONException, UnsupportedEncodingException {
		// SCM-1331: get batch
		String response = rest.setRequestMethod(HttpMethod.GET)
				.setApi("batches/" + batchId + "?workspace_name=" + ws.getName())
				.setResponseType(String.class).exec().getBody().toString();
		response = URLDecoder.decode(response, "UTF-8");
		JSONObject batchInfo = new JSONObject(response).getJSONObject("batch");
		return batchInfo;
	}
}
