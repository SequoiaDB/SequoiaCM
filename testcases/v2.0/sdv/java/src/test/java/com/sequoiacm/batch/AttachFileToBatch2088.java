package com.sequoiacm.batch;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Random;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmBatchInfo;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content: setContent file by breakpointfile,than attach file to the batch
 * testlink-case:SCM-2088
 * 
 * @author wuyan
 * @Date 2018.07.16
 * @version 1.00
 */

public class AttachFileToBatch2088 extends TestScmBase {
	private boolean runSuccess = false;	
	private SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private ScmSession session = null;
	private ScmWorkspace ws = null;	
	private byte[] writeData = new byte[ 1024 * 1024 ];		
	private ScmId batchId = null;
	private ScmId fileId = null;
	private String fileName = "file_batch_2088";
	private String batchName = "batch_2088";
	
	
	@BeforeClass()
	private void setUp() throws ScmException {
		List<SiteWrapper> siteList = ScmInfo.getAllSites();		
		for (int i = 0; i < siteList.size(); i++) {			
			DatasourceType dataType = siteList.get(i).getDataType();			
			if ( dataType.equals(DatasourceType.SEQUOIADB)){
				site = siteList.get(i);
				break;
			}			
		}
		
		session = TestScmTools.createSession(site);
		wsp = ScmInfo.getWs();
		ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);	
	   //ScmFactory.File.deleteInstance(ws, new ScmId("5b4d84d64000010000000216"), true);
	   
		//clean batch
		BSONObject tagBson = new BasicBSONObject("tags", "tag2088");			
		ScmCursor<ScmBatchInfo> cursor =ScmFactory.Batch.
				listInstance(ws, new BasicBSONObject("tags", tagBson));
		while (cursor.hasNext()) {
			ScmBatchInfo info = cursor.getNext();
	        ScmId batchId = info.getId();
	        ScmFactory.Batch.deleteInstance(ws, batchId);
	    }
	    cursor.close();	 
	    
	}
	
	@Test(groups = { "fourSite" })
	private void test() throws Exception {
		createBreakpointFileAndSetContent(ws, fileName);	
		batchId = createBatchAndAttachFile( ws, batchName, fileId);		
		checkBatchInfoByFile(ws, fileId, batchId);			
		runSuccess = true;				
	}		

	@AfterClass()
	private void tearDown() throws Exception {
		try {
			if( runSuccess ){				
				ScmFactory.Batch.deleteInstance(ws, batchId);			
			}			
		}finally {
			if (session != null) {
				session.close();
			}
		}
	}		
	
	private void createBreakpointFileAndSetContent(ScmWorkspace ws, String fileName) throws ScmException {
		// create breakpointfile
		ScmChecksumType checksumType = ScmChecksumType.CRC32;
		ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
				.createInstance(ws, fileName, checksumType);		
		new Random().nextBytes(writeData);		
		breakpointFile.upload(new ByteArrayInputStream(writeData));
		
		// save to file, than down file check the file data
		ScmFile file = ScmFactory.File.createInstance(ws);		
		file.setContent(breakpointFile);
		file.setFileName(fileName);
		fileId = file.save();		
	}
	
	private ScmId createBatchAndAttachFile( ScmWorkspace ws, String batchName,ScmId fileId) throws ScmException  {		
		ScmBatch batch = ScmFactory.Batch.createInstance(ws);
		batch.setName(batchName);
		ScmId batchId = batch.save();	
		batch.attachFile(fileId);
		
		//add tags
		ScmTags tags = new ScmTags();
		tags.addTag( "tag2088");
		batch.setTags(tags);		
		return batchId;
	}
	
	private void checkBatchInfoByFile(ScmWorkspace ws,ScmId fileId, ScmId batchId) throws Exception{
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		ScmId getBatchId = file.getBatchId();
		Assert.assertEquals(getBatchId, batchId);
		
		//batch contains a file
		ScmBatch batch = ScmFactory.Batch.getInstance(ws, batchId);			
		List<ScmFile> files = batch.listFiles();	
		ScmFile fileInfo = files.get(0);
		Assert.assertEquals(files.size(), 1);
		Assert.assertEquals(file.toString(), fileInfo.toString());
		
		//check file content		
		VersionUtils.CheckFileContentByStream(ws, fileInfo.getFileName(), 
												fileInfo.getMajorVersion(), writeData);
	}		
}