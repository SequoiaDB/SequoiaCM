package com.sequoiacm.querybuilder;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @Testcase: SCM-332:所有匹配符(同级)+不同字段组合查询 SCM-539:eleMatch和其他匹配符组合查询（同级）
 * @author huangxiaoni init
 * @date 2017.6.6
 */

public class MthCombin332 extends TestScmBase {
	private boolean runSuccess = false;
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;

	private List<ScmFile> fileList = new ArrayList<>();
	private int fileNum = 5;
	private String fileName = "MthCombin332_";

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			site = ScmInfo.getSite();
			wsp = ScmInfo.getWs();
			session = TestScmTools.createSession(site);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);

			for (int i = 0; i < fileNum; i++) {
				BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName + i).get();
				ScmFileUtils.cleanFile(wsp, cond);
			}

			readyScmFile();
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testQuery() throws Exception {
		ScmQueryBuilder builder = null;
		try {
			builder = ScmBuilder();
			long count = ScmFactory.File.countInstance(ws, ScopeType.SCOPE_CURRENT, builder.get());
			Assert.assertEquals(count, 1);

			runSuccess = true;
		} catch (ScmException e) {
			System.out.println(builder.get().toString());
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws ScmException {
		try {
			if (runSuccess || forceClear) {
				for (ScmFile file : fileList) {
					ScmId fileId = file.getFileId();
					ScmFactory.File.getInstance(ws, fileId).delete(true);
				}
			}
		} catch (BaseException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}

		}
	}

	private void readyScmFile() {
		try {
			for (int i = 0; i < fileNum; i++) {
				ScmFile file = ScmFactory.File.createInstance(ws);
				file.setFileName(fileName + i);
				file.save();
				fileList.add(file);
			}
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		}
	}

	private ScmQueryBuilder ScmBuilder() throws ScmException {
		// in
		String keyIn = ScmAttributeName.File.FILE_NAME;
		List<Object> inList = new ArrayList<>();
		inList.add(fileList.get(1).getFileName());
		inList.add(fileList.get(2).getFileName());
		BSONObject in = ScmQueryBuilder.start(keyIn).in(inList).get();

		// nin
		String keyNin = ScmAttributeName.File.FILE_ID;
		List<String> ninList = new ArrayList<>();
		ninList.add(fileList.get(2).getFileId().get());
		ninList.add(fileList.get(3).getFileId().get());
		BSONObject nin = ScmQueryBuilder.start(keyNin).notIn(ninList).get();

		// greaterThan
		String keyGt = ScmAttributeName.File.MAJOR_VERSION;
		BSONObject greaterThan = ScmQueryBuilder.start(keyGt).greaterThan(0).get();

		// lessThan
		String keyLt = ScmAttributeName.File.MINOR_VERSION;
		BSONObject lessThan = ScmQueryBuilder.start(keyLt).lessThan(1).get();

		// not
		String keyNot = ScmAttributeName.File.MAJOR_VERSION;
		BSONObject notCond = ScmQueryBuilder.start(keyNot).lessThan(0).get();

		// element
		String keyE = ScmAttributeName.File.SITE_ID;
		String keyE2 = ScmAttributeName.File.LAST_ACCESS_TIME;
		String keyE3 = ScmAttributeName.File.SITE_LIST;
		int valueE = site.getSiteId();
		int valueE2 = 12345;
		BSONObject objE = ScmQueryBuilder.start(keyE).is(valueE).and(keyE2).greaterThan(valueE2).get();

		ScmQueryBuilder builder = ScmQueryBuilder.start(keyIn).in(inList).put(keyNin).notIn(ninList).or(in, nin)
				.put(ScmAttributeName.File.TITLE).exists(1).put(ScmAttributeName.File.CREATE_TIME).greaterThan(100000)
				.put(ScmAttributeName.File.UPDATE_TIME).greaterThanEquals(1000000).put(ScmAttributeName.File.SIZE)
				.lessThan(10).put(ScmAttributeName.File.MINOR_VERSION).lessThanEquals(2).not(notCond)
				.and(lessThan, greaterThan).put(keyE3).elemMatch(objE);

		// check builder
		String bsStr1 = "{ " + "\"name\" : { \"$in\" : [ \"" + fileName + "1\" , \"" + fileName + "2\"]} , "
				+ "\"id\" : { \"$nin\" : [ \"" + fileList.get(2).getFileId().get() + "\" , \""
				+ fileList.get(3).getFileId().get() + "\"]} , " + "\"$or\" : [ { \"name\" : { \"$in\" : [ \"" + fileName
				+ "1\" , \"" + fileName + "2\"]}} , " + "{ \"id\" : { \"$nin\" : [ \""
				+ fileList.get(2).getFileId().get() + "\" , \"" + fileList.get(3).getFileId().get() + "\"]}}] , "
				+ "\"title\" : { \"$exists\" : 1} , " + "\"create_time\" : { \"$gt\" : 100000} , "
				+ "\"update_time\" : { \"$gte\" : 1000000} , " + "\"size\" : { \"$lt\" : 10} , "
				+ "\"minor_version\" : { \"$lte\" : 2} , " + "\"$not\" : [ { \"major_version\" : { \"$lt\" : 0}}] , "
				+ "\"$and\" : [ { \"minor_version\" : { \"$lt\" : 1}} , { \"major_version\" : { \"$gt\" : 0}}] , "
				+ "\"site_list\" : { \"$elemMatch\" : { \"site_id\" : " + site.getSiteId()
				+ " , \"last_access_time\" : { \"$gt\" : 12345}}}" + "}";

		Assert.assertEquals(builder.get().toString().replaceAll("\\s*",""), bsStr1.replaceAll("\\s*",""));

		return builder;
	}
}