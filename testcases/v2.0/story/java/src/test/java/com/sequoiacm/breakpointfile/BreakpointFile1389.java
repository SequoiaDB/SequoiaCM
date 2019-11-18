/**
 * 
 */
package com.sequoiacm.breakpointfile;

import java.io.File;
import java.io.IOException;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.util.json.JSONException;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description BreakpointFile1389.java, 删除空断点文件 
 * @author luweikang
 * @date 2018年5月21日
 */
public class BreakpointFile1389 extends TestScmBase {

	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;

	private String fileName = "scmfile1389";
	private int fileSize = 0;
	private File localPath = null;
	private String filePath = null;
	
	@BeforeClass(alwaysRun = true)
	private void setUp() throws IOException, ScmException {
		BreakpointUtil.checkDBDataSource();
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		TestTools.LocalFile.removeFile(localPath);
		TestTools.LocalFile.createDir(localPath.toString());
		BreakpointUtil.createFile(filePath, fileSize);

		site = ScmInfo.getSite();
		wsp = ScmInfo.getWs();
		session = TestScmTools.createSession(site);
		ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
	}
	
	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() throws JSONException, ScmException {
		
		this.createBreakpointFile();
		
		ScmFactory.BreakpointFile.deleteInstance(ws, fileName);
		
		this.checkBreakpointFile();
		
	}
	
	@AfterClass
	private void tearDown() {
		try {
			TestTools.LocalFile.removeFile(localPath);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}

		}
	}
	
	private void createBreakpointFile() throws ScmException {
		ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile.createInstance(ws, fileName);
		breakpointFile.upload(new File(filePath));
	}
	
	private void checkBreakpointFile() throws ScmException{
		ScmCursor<ScmBreakpointFile> cursor = ScmFactory.BreakpointFile.listInstance(ws,new BasicBSONObject("file_name", fileName));
		Assert.assertEquals(cursor.getNext(), null, "checkBreakpointFile should null");
	}
	
}
