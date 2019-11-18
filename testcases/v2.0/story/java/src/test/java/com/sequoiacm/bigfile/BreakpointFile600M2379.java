package com.sequoiacm.bigfile;

import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @Description:断点续传600M文件
 * @author fanyu
 * @Date:2019年02月14日
 * @version:1.0
 */
public class BreakpointFile600M2379 extends TestScmBase {
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;
	private String fileName = "breakpointfile600M";
	private ScmId fileId = null;
	private long fileSize = 1024 * 1024 * 600;
	private File localPath = null;
	private String filePath = null;
	
	@BeforeClass(alwaysRun = true)
	private void setUp() throws IOException, ScmException {
		BreakpointUtil.checkDBDataSource();
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		TestTools.LocalFile.removeFile(localPath);
		TestTools.LocalFile.createDir(localPath.toString());
		TestTools.LocalFile.createFile(filePath,fileSize);
		site = ScmInfo.getSite();
		wsp = ScmInfo.getWs();
		session = TestScmTools.createSession(site);
		ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
	}
	
	@Test(groups = {"fourSite" })
	private void test() throws ScmException, IOException {
		//创建断点文件
		ScmBreakpointFile breakpointFile = this.createBreakpointFile();
		//创建scm文件,并将创建的断点文件作为文件的内容
		breakpointFile2ScmFile( breakpointFile );
	}
	
	@AfterClass
	private void tearDown() throws ScmException {
		try {
			ScmFactory.File.deleteInstance(ws, fileId, true);
			TestTools.LocalFile.removeFile(localPath);
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}
	
	private ScmBreakpointFile createBreakpointFile() throws ScmException, IOException{
		ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile.createInstance(ws, fileName);
		InputStream inputStream = new FileInputStream(filePath);
		breakpointFile.upload( inputStream );
		inputStream.close();
		return breakpointFile;
	}
	
	private void breakpointFile2ScmFile(ScmBreakpointFile breakpointFile) throws ScmException {
		ScmFile file = ScmFactory.File.createInstance(ws);
		file.setContent(breakpointFile);
		file.setFileName(fileName);
		file.setTitle(fileName);
		fileId = file.save();
		// check file's attribute
		checkFileAttributes(file);
	}

	private void checkFileAttributes(ScmFile file) {
		Assert.assertEquals(file.getWorkspaceName(), wsp.getName());
		Assert.assertEquals(file.getFileId(), fileId);
		Assert.assertEquals(file.getFileName(), fileName);
		Assert.assertEquals(file.getAuthor(), "");
		Assert.assertEquals(file.getTitle(), fileName);
		Assert.assertEquals(file.getSize(), fileSize);
		Assert.assertEquals(file.getMinorVersion(), 0);
		Assert.assertEquals(file.getMajorVersion(), 1);
		Assert.assertEquals(file.getUser(), TestScmBase.scmUserName);
		Assert.assertNotNull(file.getCreateTime().getTime());
	}
}
