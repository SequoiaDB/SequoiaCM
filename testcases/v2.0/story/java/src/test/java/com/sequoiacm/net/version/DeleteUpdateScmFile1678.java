package com.sequoiacm.net.version;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content: update Content of  the scmfile ,than delete the scmfile
 * testlink-case:SCM-1678
 * 
 * @author wuyan
 * @Date 2018.06.02
 * @version 1.00
 */

public class DeleteUpdateScmFile1678 extends TestScmBase {
	private final int branSitesNum = 2;
	private static WsWrapper wsp = null;
	private List<SiteWrapper> branSites = null;
	private ScmSession sessionA = null;
	private ScmWorkspace wsA = null;
	private ScmSession sessionB = null;
	private ScmWorkspace wsB = null;
	private ScmId fileId = null;

	private String fileName = "file1678";
	private int fileSize = 1024 * 800;
	private byte[] updateData = new byte[ 1024 * 1024 ];		
	private File localPath = null;
	private String filePath = null;	
	private boolean runSuccess = false;
	

	@BeforeClass
	private void setUp() throws IOException, ScmException {        
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";

		TestTools.LocalFile.removeFile(localPath);
		TestTools.LocalFile.createDir(localPath.toString());
		TestTools.LocalFile.createFile(filePath, fileSize);

		branSites = ScmInfo.getBranchSites(branSitesNum);
		wsp = ScmInfo.getWs();

		sessionA = TestScmTools.createSession(branSites.get(0));
		wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);
		sessionB = TestScmTools.createSession(branSites.get(1));
		wsB = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionB);	
		
		fileId = VersionUtils.createFileByFile( wsA, fileName, filePath );
		VersionUtils.updateContentByStream(wsA, fileId, updateData);
	}

	@Test(groups = { "fourSite"})
	private void test() throws Exception {	
		ScmFactory.File.deleteInstance(wsB, fileId, true);
		checkDeleteResult();
		runSuccess = true;
	}

	@AfterClass
	private void tearDown() {
		try {	
			if( runSuccess ){
				TestTools.LocalFile.removeFile(localPath);
			}			
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

	private void checkDeleteResult(){
		try {
            ScmFactory.File.getInstanceByPath(wsA, fileName);
	    	Assert.fail("get  file must bu fail!");
	    } catch (ScmException e) {
			if ( ScmError.FILE_NOT_FOUND != e.getError()) {
				Assert.fail("expErrorCode:-262  actError:"+e.getError()+e.getMessage());
			}
		}		
		
		//get current version file
		try {
            ScmFactory.File.getInstanceByPath(wsA, fileName, 2, 0);
	    	Assert.fail("get currentVersion file must bu fail!");
	    } catch (ScmException e) {
			if ( ScmError.FILE_NOT_FOUND != e.getError()) {
				Assert.fail("expErrorCode:-262  actError:"+e.getError()+e.getMessage());
			}
		}	
		//get history version file
		try {
            ScmFactory.File.getInstanceByPath(wsA, fileName, 1, 0);
	    	Assert.fail("get  historyVersion file must bu fail!");
	    } catch (ScmException e) {
			if ( ScmError.FILE_NOT_FOUND != e.getError()) {
				Assert.fail("expErrorCode:-262  actError:"+e.getError()+e.getMessage());
			}
		}	
	}
	
	

	
}