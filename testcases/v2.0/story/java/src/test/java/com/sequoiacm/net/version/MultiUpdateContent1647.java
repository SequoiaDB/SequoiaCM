package com.sequoiacm.net.version;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
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
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:update file contexts multiple times 
 * testlink-case:SCM-1647
 * 
 * @author wuyan
 * @Date 2018.06.01
 * @version 1.00
 */

public class MultiUpdateContent1647 extends TestScmBase {
	@DataProvider(name = "datasizeProvider")
	public Object[][] generateDataSize(){
		return new Object[][]{
			//the parameter : major_version / datasize
			//the first write to the file, version is 1
			new Object[]{1, 1024 * 100 },
			//the first updateContext, version is 2
			new Object[]{2, 1024 * 255 },
			//the second updateContext, version is 3
			new Object[]{3, 1024 * 2   },		
			//the 3rd updateContext, version is 4
			new Object[]{4, 1024 * 20   },	
			//the 4th updateContext, version is 5
			new Object[]{5, 1024 * 1024 * 2   },	
			//the 5th updateContext, version is 6
			new Object[]{6, 1024 * 3    },	
			//the sixth updateContext, version is 7
			new Object[]{7, 1024 * 2   },	
			//the 7th updateContext, version is 8
			new Object[]{8, 1024 * 2   },	
			//the 8th updateContext, version is 9
			new Object[]{9,  1024 * 500   },	
			//the 9th updateContext, version is 10
			new Object[]{10, 1024 * 800   },	
			//the 10th updateContext, version is 11
			new Object[]{11, 1024 * 1024 * 2   },			
		};
	}
	
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;	
	private ScmId fileId = null;
	private String fileName = "versionfile1647";		

	@BeforeClass
	private void setUp() throws IOException, ScmException {
		site = ScmInfo.getSite();
		wsp = ScmInfo.getWs();
		session = TestScmTools.createSession(site);
		ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);		
		
	}

	@Test(dataProvider = "datasizeProvider",groups = { "oneSite", "twoSite", "fourSite"})
	private void testUpdateContent(int version, int dataSize) throws Exception {
		byte[] contentdata = new byte[dataSize];
		//version is 1,first write to the file
		if ( version == 1 ){			
			fileId = VersionUtils.createFileByStream( ws, fileName, contentdata );
		}else{
			//updatecontent of the file
			updateContentByStream(contentdata);
		}
		
		//check result		
		VersionUtils.CheckFileContentByStream(  ws, fileName, version, contentdata );		
		VersionUtils.checkFileCurrentVersion(ws, fileId, version);
		checkFileSize( version, dataSize);
	}

	@AfterClass
	private void tearDown() {
		try {			
			ScmFactory.File.deleteInstance(ws, fileId, true);			
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}			
		}
	}	

	private void updateContentByStream(byte[] contentdata) throws ScmException{		
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		new Random().nextBytes(contentdata);	
		file.updateContent( new ByteArrayInputStream(contentdata) );		
	}	
	
	private void checkFileSize(int version, int expSize) throws ScmException {		
		ScmFile file = ScmFactory.File.getInstance(ws, fileId, version, 0);
		Assert.assertEquals(file.getSize(), expSize);
		Assert.assertEquals(file.getMajorVersion(), version);
				
	}
}