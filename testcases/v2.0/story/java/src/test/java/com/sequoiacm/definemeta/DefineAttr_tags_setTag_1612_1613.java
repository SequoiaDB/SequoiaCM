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
 * @Testcase:   SCM-1612:更新已存在标签
 *				SCM-1613:更新添加新的标签
 * @author huangxiaoni init
 * @date 2017.6.22
 */

public class DefineAttr_tags_setTag_1612_1613 extends TestScmBase {
	private boolean runSuccess = false;
	private SiteWrapper site = null;
	private WsWrapper wsp = null;
	private ScmSession session = null;
	private ScmWorkspace ws = null;

	private String name = "defineTags1612";
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
		test_setTag01();
		test_setTag02();
		runSuccess = true;
	}

	// SCM-1612:更新已存在标签
	private void test_setTag01() throws Exception {
		//test scm file
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		file.addTag("k1");
		file.addTag("k2");
		// check results
		file = ScmFactory.File.getInstance(ws, fileId);
		ScmTags fileTags = file.getTags();
		Assert.assertTrue(fileTags.toSet().contains("k1"),fileTags.toString());
		Assert.assertTrue(fileTags.toSet().contains("k2"), fileTags.toString());

		//test scm batch
		ScmBatch batch = ScmFactory.Batch.getInstance(ws,batchId);
		batch.addTag("k1");
		batch.addTag("k2");
		// check results
		batch = ScmFactory.Batch.getInstance(ws,batchId);
		ScmTags batchTags = batch.getTags();
		Assert.assertTrue(batchTags.toSet().contains("k1"),fileTags.toString());
		Assert.assertTrue(batchTags.toSet().contains("k2"), fileTags.toString());
	}

	// SCM-1613:更新添加新的标签
	private void test_setTag02() throws Exception {
		//test scm batch
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		file.addTag("k3");
		file.addTag("k4");
		file.addTag("k5");
		// check results
		file = ScmFactory.File.getInstance(ws, fileId);
		ScmTags fileTags = file.getTags();

		//test scm batch
		ScmBatch batch = ScmFactory.Batch.getInstance(ws,batchId);
		batch.addTag("k3");
		batch.addTag("k4");
		batch.addTag("k5");
		// check results
		batch = ScmFactory.Batch.getInstance(ws,batchId);
		ScmTags batchTags = batch.getTags();

		//exp tags;
		Set<String> expTagsSet = new HashSet<>();
		expTagsSet.add("k1");
		expTagsSet.add("k2");
		expTagsSet.add("k3");
		expTagsSet.add("k4");
		expTagsSet.add("k5");
		//check result
		Assert.assertEquals(fileTags.toSet(), expTagsSet,"fileTags = "
		+ fileTags.toString() + ",expTagsSet = "+ expTagsSet.toString());
		Assert.assertEquals(batchTags.toSet(), expTagsSet,"fileTags = "
				+ batchTags.toString() + ",expTagsSet = "+ expTagsSet.toString());
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