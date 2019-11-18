
package com.sequoiacm.directory;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Description:  SCM-2582::在目录下不指定排序分页列取文件列表
 * @author fanyu
 * @Date:2019年09月04日
 * @version:1.0
 */
public class ListFileInPaDir2582 extends TestScmBase{
	private AtomicInteger expSuccessTestCount = new AtomicInteger(0);
	private SiteWrapper site;
	private WsWrapper wsp;
	private ScmSession session;
	private ScmWorkspace ws;
	private String dirPath = "/dir2582";
	private ScmDirectory scmDirectory;
	private String fileNamePrefix = "file2582";
	private int fileNum = 100;
	private List<ScmId> fileIdList = new ArrayList<>();
	private BSONObject filter;
	private List<ScmFileBasicInfo> fileList = new ArrayList<>();

	@BeforeClass(alwaysRun = true)
	private void setUp() throws ScmException {
		site = ScmInfo.getSite();
		wsp = ScmInfo.getWs();
		session = TestScmTools.createSession(site);
		ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
		//clean
		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(fileNamePrefix).get();
		ScmFileUtils.cleanFile(wsp, cond);
		if (ScmFactory.Directory.isInstanceExist(ws, dirPath)) {
			ScmFactory.Directory.deleteInstance(ws, dirPath);
		}
		scmDirectory = ScmFactory.Directory.createInstance(ws, dirPath);
		//prepare file
		for (int i = 0; i < fileNum; i++) {
			String fileName = fileNamePrefix + "-" + i;
			ScmFile scmFile = ScmFactory.File.createInstance(ws);
			scmFile.setAuthor(fileNamePrefix);
			scmFile.setDirectory(scmDirectory);
			scmFile.setTitle(fileName + "-" + (fileNum - i));
			scmFile.setFileName(fileName);
			if (i % 2 == 0) {
				scmFile.addTag("transfer");
			} else {
				scmFile.addTag("tally");
			}
			fileIdList.add(scmFile.save());
		}
		//prepare filter
		//all file
		filter = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(fileNamePrefix).get();
		getInitScmFileInfo(filter, fileList);
	}

	@DataProvider(name = "dataProvider", parallel = true)
	public Object[][] generateRangData() throws Exception {
		return new Object[][]{
				//orderby:null
				{filter, null, 0, 10},
				{filter, null, 3, 10}
		};
	}

	@Test(dataProvider = "dataProvider")
	private void test(BSONObject filter, BSONObject orderby, int skip, int limit) throws Exception {
		int actPageSize = 0;
		int tmpSkip = skip;
		int totalNum = 0;
		ScmSession session = null;
		try {
			session = TestScmTools.createSession(site);
			ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
			ScmDirectory directory = ScmFactory.Directory.getInstance(ws,dirPath);
			while (tmpSkip < fileNum) {
				ScmCursor<ScmFileBasicInfo> cursor =  directory.listFiles(filter,tmpSkip, limit,orderby);
				int count = 0;
				while (cursor.hasNext()) {
					ScmFileBasicInfo act = cursor.getNext();
					try {
						Assert.assertEquals(act.getFileName().contains(fileNamePrefix), true);
						count++;
					} catch (AssertionError e) {
						throw new Exception("filter = " + filter + ",orderby = " + orderby
								+ ",skip = " + skip + ",limit = " + limit + "，act = " + act, e);
					}
				}
				if (limit == 0 || count == 0) {
					break;
				}
				tmpSkip += count;
				totalNum += count;
				actPageSize++;
			}
		} finally {
			if (session != null) {
				session.close();
			}
		}
		try {
			int size = fileNum;
			if (skip < size && limit != 0) {
				Assert.assertEquals(totalNum, size - skip);
				if (limit == -1) {
					Assert.assertEquals(actPageSize, 1);
				} else {
					Assert.assertEquals(actPageSize, (int) Math.ceil(((double) size / limit)));
				}
			} else {
				Assert.assertEquals(totalNum, 0, "orderby = " + orderby);
				Assert.assertEquals(actPageSize, 0);
			}
		} catch (AssertionError e) {
			throw new Exception("filter = " + filter + ",orderby = " + orderby
					+ ",skip = " + skip + ",limit = " + limit, e);
		}
		expSuccessTestCount.getAndIncrement();
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		try {
			if (expSuccessTestCount.get() == 2 || TestScmBase.forceClear) {
				for (ScmId fileId : fileIdList) {
					ScmFactory.File.deleteInstance(ws, fileId, true);
				}
				ScmFactory.Directory.deleteInstance(ws, dirPath);
			}
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}

	private void getInitScmFileInfo(BSONObject filter, List<ScmFileBasicInfo> fileList) throws ScmException {
		ScmCursor<ScmFileBasicInfo> cursor = null;
		try {
			cursor = scmDirectory.listFiles(filter);
			while (cursor.hasNext()) {
				fileList.add(cursor.getNext());
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}
}


