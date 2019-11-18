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
 * @Testcase: SCM-331:所有匹配符+同一个字段组合查询
 * @author huangxiaoni init
 * @date 2017.6.6
 */

public class MthCombin331 extends TestScmBase {
	private boolean runSuccess = false;
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;

	private List<ScmFile> fileList = new ArrayList<>();
	private int fileNum = 3;
	private String authorName = "MthCombin331";

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			site = ScmInfo.getSite();
			wsp = ScmInfo.getWs();
			session = TestScmTools.createSession(site);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);

			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
			ScmFileUtils.cleanFile(wsp, cond);

			readyScmFile();
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testQuery() throws Exception {
		try {
			// same fields
			ScmQueryBuilder builder = ScmBuilder();
			long count = ScmFactory.File.countInstance(ws, ScopeType.SCOPE_CURRENT,
					builder.put(ScmAttributeName.File.AUTHOR).is(fileList.get(0).getAuthor()).get());
			Assert.assertEquals(count, fileNum);

			ScmQueryBuilder builderByIs1 = ScmBuilderByIs1();
			long count2 = ScmFactory.File.countInstance(ws, ScopeType.SCOPE_CURRENT,
					builderByIs1.put(ScmAttributeName.File.AUTHOR).is(fileList.get(0).getAuthor()).get());
			Assert.assertEquals(count2, fileNum);

			// different fields
			ScmQueryBuilder builderByIs2 = ScmBuilderByIs2();
			long count3 = ScmFactory.File.countInstance(ws, ScopeType.SCOPE_CURRENT, builderByIs2.get());
			Assert.assertEquals(count3, 1);

			runSuccess = true;
		} catch (ScmException e) {
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
				file.setFileName(authorName + "_" + i);
				file.setAuthor(authorName);
				file.setTitle(authorName + "_" + i);
				file.save();
				fileList.add(file);
			}
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		}
	}

	private ScmQueryBuilder ScmBuilder() throws ScmException {
		ScmQueryBuilder builder = null;
		String key = ScmAttributeName.File.MAJOR_VERSION;

		// in
		List<Object> inList = new ArrayList<>();
		inList.add(0);
		inList.add(fileList.get(1).getMajorVersion());
		BSONObject in = ScmQueryBuilder.start(key).in(inList).get();

		// nin
		List<Object> ninList = new ArrayList<>();
		ninList.add(0);
		ninList.add(-1);
		BSONObject nin = ScmQueryBuilder.start(key).notIn(ninList).get();

		// greaterThan
		BSONObject greaterThan = ScmQueryBuilder.start(key).greaterThan(-1).get();

		// lessThan
		BSONObject lessThan = ScmQueryBuilder.start(key).lessThan(2).get();

		// not
		BSONObject notCond = ScmQueryBuilder.start(key).lessThan(0).get();

		builder = ScmQueryBuilder.start(key).in(inList).put(key).notIn(ninList).or(in, nin).put(key).exists(1).put(key)
				.greaterThan(-1).put(key).greaterThanEquals(0).put(key).lessThan(10).put(key).lessThanEquals(1)
				.not(notCond).and(lessThan, greaterThan);

		// check builder
		// Note: $in and $nin are conflicted. choose $nin here
		String bsStr1 = "{ \"major_version\" : { " + "\"$nin\" : [ 0 , -1] , " + "\"$exists\" : 1 , "
				+ "\"$gt\" : -1 , " + "\"$gte\" : 0 , " + "\"$lt\" : 10 , " + "\"$lte\" : 1} , "
				+ "\"$or\" : [ { \"major_version\" : { \"$in\" : [ 0 , 1]}} , "
				+ "{ \"major_version\" : { \"$nin\" : [ 0 , -1]}}] , "
				+ "\"$not\" : [ { \"major_version\" : { \"$lt\" : 0}}] , "
				+ "\"$and\" : [ { \"major_version\" : { \"$lt\" : 2}} , " + "{ \"major_version\" : { \"$gt\" : -1}}]}";
		// System.out.println(builder.get().toString() + "\n" + bsStr1);
		Assert.assertEquals(builder.get().toString().replaceAll("\\s*",""), bsStr1.replaceAll("\\s*",""));

		return builder;
	}

	private ScmQueryBuilder ScmBuilderByIs1() throws ScmException {
		// same fields
		String key = ScmAttributeName.File.MAJOR_VERSION;
		// is
		ScmQueryBuilder builder = this.ScmBuilder().put(key).is(fileList.get(0).getMajorVersion());

		// check builder
		String bsStr2 = "{ \"major_version\" : 1 , \"$or\" : [ { \"major_version\" : { \"$in\" : [ 0 , 1]}} , { \"major_version\" : { \"$nin\" : [ 0 , -1]}}] , \"$not\" : [ { \"major_version\" : { \"$lt\" : 0}}] , \"$and\" : [ { \"major_version\" : { \"$lt\" : 2}} , { \"major_version\" : { \"$gt\" : -1}}]}";
		Assert.assertEquals(builder.get().toString().replaceAll("\\s*",""), bsStr2.replaceAll("\\s*",""));

		return builder;
	}

	private ScmQueryBuilder ScmBuilderByIs2() throws ScmException {
		// same fields
		String key = ScmAttributeName.File.TITLE;
		// is
		ScmQueryBuilder builder = this.ScmBuilder().put(key).is(fileList.get(0).getTitle());

		// check builder
		// Note: $in and $nin are conflicted. choose $nin here
		String bsStr2 = "{ \"major_version\" : { " + "\"$nin\" : [ 0 , -1] , " + "\"$exists\" : 1 , "
				+ "\"$gt\" : -1 , " + "\"$gte\" : 0 , " + "\"$lt\" : 10 , " + "\"$lte\" : 1} , "
				+ "\"$or\" : [ { \"major_version\" : { \"$in\" : [ 0 , 1]}} , "
				+ "{ \"major_version\" : { \"$nin\" : [ 0 , -1]}}] , "
				+ "\"$not\" : [ { \"major_version\" : { \"$lt\" : 0}}] , "
				+ "\"$and\" : [ { \"major_version\" : { \"$lt\" : 2}} , " + "{ \"major_version\" : { \"$gt\" : -1}}] , "
				+ "\"title\" : \"" + authorName + "_0\"}";
		Assert.assertEquals(builder.get().toString().replaceAll("\\s*",""), bsStr2.replaceAll("\\s*",""));

		return builder;
	}
}