package com.sequoiacm.definemeta;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @Testcase:  SCM-1608:批量添加多个标签，不重复
 * 			   SCM-1609:批量添加多个标签，部分重复
 * @author huangxiaoni init
 * @date 2017.6.22
 */

public class DefineAttr_tags_setTags_1608_1609 extends TestScmBase {
	private boolean runSuccess = false;
	private String name = "defineTags1608";
	private SiteWrapper site = null;
	private WsWrapper wsp = null;
	private ScmSession session = null;
	private ScmWorkspace ws = null;
	private ScmId fileId = null;
	private ScmId batchId = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() throws IOException, ScmException {
		site = ScmInfo.getSite();
		wsp = ScmInfo.getWs();
		session = TestScmTools.createSession(site);
		ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);

		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(name).get();
		ScmFileUtils.cleanFile(wsp, cond);
		this.prepareScmFile();
		this.prepareBatch();
	}

	@Test
	private void test() throws Exception {
		test_setTags01();
		test_setTags02();
		runSuccess = true;
	}

	private void test_setTags01() throws Exception {
		// define tags
		Set<String> tagSet = new HashSet<>();
		tagSet.add("test");
		tagSet.add("123");
		tagSet.add("null");
		ScmTags scmTags = new ScmTags();
		scmTags.addTags(tagSet);
		// test scm file set tags
		ScmFile file = ScmFactory.File.getInstance(ws,fileId);
		file.setTags(scmTags);
		// check results
		file = ScmFactory.File.getInstance(ws, fileId);
		ScmTags fileTags = file.getTags();
		Assert.assertEquals(fileTags.toSet().size(),tagSet.size());
		Assert.assertTrue(fileTags.toSet().containsAll(tagSet),
				"fileTags = " + fileTags.toString() + ",set = "+ tagSet.toString());

		//test scm batch
		ScmBatch batch = ScmFactory.Batch.getInstance(ws,batchId);
		batch.setTags(scmTags);
		//check result
		batch = ScmFactory.Batch.getInstance(ws,batchId);
		ScmTags batchTags = batch.getTags();
		Assert.assertEquals(batchTags.toSet().size(),tagSet.size());
		Assert.assertTrue(batchTags.toSet().containsAll(tagSet),
				"fileTags = " + fileTags.toString() + ",set = "+ tagSet.toString());
	}

	private void test_setTags02() throws Exception {
		// define tags 客户端标签重复
		ScmTags scmTags = new ScmTags();
		scmTags.addTag("test2");
		scmTags.addTag("test3");
		scmTags.addTag("test3");
		scmTags.addTag("111");
		scmTags.addTag("true");
		scmTags.addTag("true");

		// test scm file set tags
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		file.setTags(scmTags);
		//服务端标签重复
		file.addTag("test2");
		// check results
		file = ScmFactory.File.getInstance(ws, fileId);
		ScmTags fileTags = file.getTags();
		Assert.assertEquals(fileTags.toSet(),scmTags.toSet(),
				"fileTags = " + fileTags.toString() + ",set = "+ scmTags.toString().toString());

		//test scm batch
		ScmBatch batch = ScmFactory.Batch.getInstance(ws,batchId);
		batch.setTags(scmTags);
		// check results
		batch = ScmFactory.Batch.getInstance(ws,batchId);
		ScmTags batchTags = batch.getTags();
		Assert.assertEquals(batchTags.toSet(),scmTags.toSet(),
				"fileTags = " + fileTags.toString() + ",set = "+ scmTags.toString().toString());

	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws ScmException {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				ScmFactory.File.deleteInstance(ws, fileId, true);
				ScmFactory.Batch.deleteInstance(ws,batchId);
			}
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}

	private void prepareScmFile() throws ScmException {
		// define tags
		ScmTags tags = new ScmTags();
		tags.addTag("k1");
		tags.addTag("k2");

		// upload file and set tags
		ScmFile file = ScmFactory.File.createInstance(ws);
		file.setFileName(name);
		file.setTags(tags);
		fileId = file.save();
	}

	private void prepareBatch() throws ScmException {
		// define tags
		Set<String> tagSet = new HashSet<>();
		tagSet.add("k1");
		tagSet.add("k2");
		ScmTags tags = new ScmTags();
		tags.addTags(tagSet);
		// upload file and set tags
		ScmBatch scmBatch = ScmFactory.Batch.createInstance(ws);
		scmBatch.setTags(tags);
		scmBatch.setName(name);
		batchId = scmBatch.save();
	}
}