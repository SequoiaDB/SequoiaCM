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
 * @Testcase:   SCM-1615:删除已存在的标签
 *				 SCM-1616:删除不存在的标签
 *				SCM-1617:获取不存在的标签
 * @author huangxiaoni init
 * @date 2017.6.22
 */

public class DefineAttr_tags_deleteOrGetTag_1615_to_1617 extends TestScmBase {
	private boolean runSuccess = false;
	private SiteWrapper site = null;
	private WsWrapper wsp = null;
	private ScmSession session = null;
	private ScmWorkspace ws = null;
	private String name = "defineTags1614";
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
		test_delTag01();
		test_delTag02();
		test_getTag();
		runSuccess = true;
	}

	//SCM-1615:删除已存在的标签
	private void test_delTag01() throws Exception {
		//test scm file
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		file.removeTag("k1");
		// check results
		file = ScmFactory.File.getInstance(ws, fileId);
		ScmTags tags = file.getTags();
		Assert.assertFalse(tags.contains("k1"), tags.toString());
		Assert.assertTrue(tags.contains("k2"), tags.toString());

		//test scm batch
		ScmBatch batch = ScmFactory.Batch.getInstance(ws, batchId);
		batch.removeTag("k1");
		// check results
		batch = ScmFactory.Batch.getInstance(ws, batchId);
		ScmTags tags1 = batch.getTags();
		Assert.assertFalse(tags1.contains("k1"), tags.toString());
		Assert.assertTrue(tags1.contains("k2"), tags.toString());

	}

	//SCM-1616:删除不存在的标签
	private void test_delTag02() throws Exception {
		//test scm file
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		file.removeTag("k5555");
		// check results
		file = ScmFactory.File.getInstance(ws, fileId);
		ScmTags tags = file.getTags();
		Assert.assertFalse(tags.contains("k1"), tags.toString());
		Assert.assertTrue(tags.contains("k2"), tags.toString());

		//test scm batch
		ScmBatch batch = ScmFactory.Batch.getInstance(ws, batchId);
		batch.removeTag("k5555");
		// check results
		batch = ScmFactory.Batch.getInstance(ws, batchId);
		ScmTags tags1 = batch.getTags();
		Assert.assertFalse(tags1.contains("k1"), tags.toString());
		Assert.assertTrue(tags1.contains("k2"), tags.toString());
	}

	//SCM-1617:获取不存在的标签
	private void test_getTag() throws Exception {
		//test scm file
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		ScmTags tags = file.getTags();
		Assert.assertFalse(tags.contains("k5555"));

		//test scm batch
		ScmBatch batch = ScmFactory.Batch.getInstance(ws, batchId);
		ScmTags tags1 = batch.getTags();
		Assert.assertFalse(tags1.contains("k5555"));
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws ScmException {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				ScmFactory.File.deleteInstance(ws, fileId, true);
				ScmFactory.Batch.deleteInstance(ws, batchId);
			}
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}

	private void prepareScmFile() throws ScmException {
		// define tags
		Set<String> tagSet = new HashSet<>();
		tagSet.add("k1");
		tagSet.add("k2");
		ScmTags tags = new ScmTags();
		tags.addTags(tagSet);
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