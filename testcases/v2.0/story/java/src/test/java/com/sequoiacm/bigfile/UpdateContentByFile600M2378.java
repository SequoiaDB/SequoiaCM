package com.sequoiacm.bigfile;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.VersionUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * @Description：通过文件路径方式更新600M文件
 * @author fanyu
 * @Date:2019年02月14日
 * @version:1.0
 */

public class UpdateContentByFile600M2378 extends TestScmBase {
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;	
	private ScmId fileId = null;

	private String fileName = "updatefile600M";
	private int fileSize = 0;
	private long updateFileSize = 1024 * 1024 * 600;
	private File localPath = null;
	private String filePath = null;
	private String updateFilePath = null;

	@BeforeClass
	private void setUp() throws IOException, ScmException {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		updateFilePath = localPath + File.separator + "localFile_" + updateFileSize + ".txt";

		TestTools.LocalFile.removeFile(localPath);
		TestTools.LocalFile.createDir(localPath.toString());
		TestTools.LocalFile.createFile(filePath, fileSize);
		TestTools.LocalFile.createFile(updateFilePath,updateFileSize);

		site = ScmInfo.getSite();
		wsp = ScmInfo.getWs();
		session = TestScmTools.createSession(site);
		ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
	}

	@Test(groups = {"fourSite"})
	private void test() throws Exception {
		fileId = VersionUtils.createFileByFile( ws, fileName, filePath );
		//test a:updateContent by 600M file
		updateContentByFile(updateFilePath);
		//check result
		int currentVersion = 2;
		VersionUtils.CheckFileContentByFile(  ws, fileName, currentVersion, updateFilePath, localPath );
		checkFileSize( currentVersion, updateFileSize);
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

	//test a:updateContent by 600M file
	private void updateContentByFile(String filePath) throws ScmException{
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		file.updateContent(filePath);		
	}	

	private void checkFileSize(int version, long expSize) throws ScmException {
		ScmFile file = ScmFactory.File.getInstance(ws, fileId, version, 0);
		Assert.assertEquals(file.getSize(), expSize);
		Assert.assertEquals(file.getMajorVersion(), version);
	}
}