package com.sequoiacm.version;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:write to file from siteA,than update Content of  the current scm file from siteB 
 * testlink-case:SCM-1649
 * 
 * @author wuyan
 * @Date 2018.06.04
 * @version 1.00
 */

public class UpdateContent1649 extends TestScmBase {
	private final int branSitesNum = 2;
	private static WsWrapper wsp = null;
	private List<SiteWrapper> branSites = null;
	private ScmSession sessionA = null;
	private ScmWorkspace wsA = null;
	private ScmSession sessionB = null;
	private ScmWorkspace wsB = null;
	private ScmId fileId = null;

	private String fileName = "file1649";
	private int writeSize = 1024 * 100;
	private int updateSize = 1024 * 1024;
	private byte[] filedata = new byte[writeSize];
	private File localPath = null;
	private String filePath = null;	
	

	@BeforeClass
	private void setUp() throws IOException, ScmException {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + updateSize + ".txt";

		TestTools.LocalFile.removeFile(localPath);
		TestTools.LocalFile.createDir(localPath.toString());
		TestTools.LocalFile.createFile(filePath, updateSize);

		branSites = ScmInfo.getBranchSites(branSitesNum);
		wsp = ScmInfo.getWs();

		sessionA = TestScmTools.createSession(branSites.get(0));
		wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);
		sessionB = TestScmTools.createSession(branSites.get(1));
		wsB = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionB);
	}

	@Test(groups = { "fourSite"})
	private void test() throws Exception {
		fileId = VersionUtils.createFileByStream( wsA, fileName, filedata );
		//updateContent from siteB
		updateContentByFile();		
		
		//check result
		int currentVersion = 2;
		int historyVersion = 1;
		//check the sitelist/currentversion/size
		SiteWrapper[] expSiteListA = { branSites.get(0) };
		SiteWrapper[] expSiteListB = { branSites.get(1) };
		VersionUtils.checkSite(wsA, fileId, currentVersion, expSiteListB);
		VersionUtils.checkSite(wsA, fileId, historyVersion, expSiteListA);
		VersionUtils.checkFileCurrentVersion(wsA, fileId, currentVersion);
		VersionUtils.checkFileSize(wsA, fileId, currentVersion, updateSize);
		VersionUtils.checkFileSize(wsA, fileId, historyVersion, writeSize);	
		//check fileContent
		VersionUtils.CheckFileContentByStream(wsA, fileName, historyVersion, filedata);
		VersionUtils.CheckFileContentByFile(wsB, fileName, currentVersion, filePath, localPath);				
		
	}

	@AfterClass
	private void tearDown() {
		try {
			ScmFactory.File.deleteInstance(wsB, fileId, true);
			TestTools.LocalFile.removeFile(localPath);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (sessionA != null) {
				sessionA.close();
			}	
			if (sessionB != null) {
				sessionB.close();
			}
		}
	}	

	private void updateContentByFile() throws ScmException{		
		ScmFile file = ScmFactory.File.getInstance(wsB, fileId);
		file.updateContent(filePath);
		file.setFileName(fileName);		
	}	
	
	
}