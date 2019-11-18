package com.sequoiacm.rest;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.util.json.JSONObject;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName SCM-1212: 停止任务
 * @Author linsuqiang
 * @Date 2018-04-11
 * @Version 1.00
 */

public class StopTasks1212 extends TestScmBase {
	private WsWrapper ws = null;
	private RestWrapper rest = null;
	private String taskId = null;
	private File localPath = null;
	private String filePath = null;
	private final int fileNum = 5;
	private final int fileSize = 0;
	private final String authorName = "这是一个中文名1212";
	private final List<String> fileIdList = new ArrayList<>(fileSize);
	private SiteWrapper site = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() throws Exception {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		TestTools.LocalFile.removeFile(localPath);
		TestTools.LocalFile.createDir(localPath.toString());
		TestTools.LocalFile.createFile(filePath, fileSize);
        site = ScmInfo.getBranchSite();
        ws = ScmInfo.getWs();
        rest = new RestWrapper();
        rest.connect(site.getSiteServiceName(), TestScmBase.scmUserName,TestScmBase.scmPassword);

		for (int i = 0; i < fileNum; i++) {
			String fileId = createFile(rest, ws, filePath);
			fileIdList.add(fileId);
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	// check stop task function is complicated, so I just check the return.
	// function has been checked in detail by scm driver testcases.
	private void test() throws Exception {
		taskId = transferFiles(rest, ws);
	    rest.setRequestMethod(HttpMethod.POST)
				.setApi("tasks/" + taskId + "/stop")
				.setResponseType(String.class).exec();

		try {
			String inexistentId = "ffffffffffffffff";
			String response = rest.setRequestMethod(HttpMethod.POST)
					.setApi("tasks/" + inexistentId + "/stop")
					.setResponseType(String.class).exec().getBody().toString();
			System.out.println(response);
			Assert.fail("stopping inexistent task should not succeed");
		} catch (HttpClientErrorException e) {
		    Assert.assertEquals(404, e.getStatusCode().value());
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		for (int i = 0; i < fileNum; i++) {
			deleteFile(rest, ws, fileIdList.get(i));
		}
		if (taskId != null) {
			TestSdbTools.Task.deleteMeta(new ScmId(taskId));
		}
        if (rest != null) {
            rest.disconnect();
        }
	}

	private String createFile(RestWrapper rest, WsWrapper ws, String filePath) throws Exception {
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
		String fileId = new JSONObject(wResponse).getJSONObject("file").getString("id");
		return fileId;
	}

	private void deleteFile(RestWrapper rest, WsWrapper ws, String fileId) {
		rest.setRequestMethod(HttpMethod.DELETE)
				.setApi("files/" + fileId + "?workspace_name=" + ws.getName() + "&is_physical=true")
				.setResponseType(Resource.class).exec();
	}

	private String transferFiles(RestWrapper rest, WsWrapper ws) throws Exception {
		JSONObject options = new JSONObject().put("filter", new JSONObject().put("author", authorName));
		String response = rest.setRequestMethod(HttpMethod.POST)
				.setApi("tasks")
				.setParameter("task_type", "2")
				.setParameter("workspace_name", ws.getName())
				.setParameter("options", options.toString())
				.setResponseType(String.class).exec().getBody().toString();
		String taskId = new JSONObject(response).getJSONObject("task").getString("id");
		return taskId;
	}
}
