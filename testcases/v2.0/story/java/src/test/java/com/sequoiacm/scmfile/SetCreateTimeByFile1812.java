package com.sequoiacm.scmfile;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Random;

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
import com.sequoiacm.testcommon.WsWrapper;

/**
 * test content:set createTime when create file
 * testlink-case:SCM-1812
 * 
 * @author wuyan
 * @Date 2018.06.21
 * @version 1.00
 */

public class SetCreateTimeByFile1812 extends TestScmBase {	
	private static WsWrapper wsp = null;
	private SiteWrapper site = null;
	private ScmSession session = null;
	private ScmWorkspace ws = null;	
	private ScmId fileId1 = null;
	private ScmId fileId2 = null;
	private ScmId fileId3 = null;
	private byte[] filedata1 = new byte[ 1024 * 100 ];
	private byte[] filedata2 = new byte[ 1024 * 20 ];
	private byte[] filedata3 = new byte[ 1024 * 50 ];

	@BeforeClass
	private void setUp() throws IOException, ScmException {		
		site = ScmInfo.getSite();
		wsp = ScmInfo.getWs();
		session = TestScmTools.createSession(site);
		ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);					
	}

	@Test(groups = { "twoSite", "fourSite"})
	private void test() throws Exception {		
		long currentTimestamp = new Date().getTime();
		
		//test a: the setCreateTime interval within one month
		long timestamp1 = currentTimestamp - 10000;
		String fileName1 = "file1812a";
		fileId1 = createFileByStream( ws, fileName1, filedata1 ,timestamp1);
		checkResult(fileId1, filedata1, timestamp1);
		
		
		//test b :at least 31 days between different months,the timestamp is 9678400000ms		
		long timestamp2 = currentTimestamp - 9678400000l;
		String fileName2= "file1812b";
		fileId2 = createFileByStream( ws, fileName2, filedata2 ,timestamp2);
		checkResult(fileId2, filedata2, timestamp2);
		
		//test c :not the same year at least 365 days,,the timestamp is 31536000000ms
		long timestamp3 = currentTimestamp - 31536000000l;
		String fileName3 = "file1812c";
		fileId3 = createFileByStream( ws, fileName3, filedata3 ,timestamp3);
		checkResult(fileId3, filedata3, timestamp3);
	}

	@AfterClass
	private void tearDown() {
		try {			
			ScmFactory.File.deleteInstance(ws, fileId1, true);	
			ScmFactory.File.deleteInstance(ws, fileId2, true);
			ScmFactory.File.deleteInstance(ws, fileId3, true);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}
			
		}
	}	
	
	private  ScmId createFileByStream( ScmWorkspace ws, String fileName, byte[] data, long timestamp ) throws ScmException  {		
		ScmFile file = ScmFactory.File.createInstance(ws);			
		new Random().nextBytes(data);		
		file.setContent(new ByteArrayInputStream(data));
		file.setFileName(fileName);
		file.setAuthor(fileName);	
		
		Date date = new Date(timestamp);
		file.setCreateTime(date);	
		ScmId fileId = file.save();		
		return fileId;
	}
	
	private void checkResult(ScmId fileId, byte[] expData, long timestamp) throws ScmException{
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		//check the createTime
		Date actdate = file.getCreateTime();
		long actTimestamp = actdate.getTime();
		Assert.assertEquals(actTimestamp, timestamp);
		
		// down file and check file data		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		file.getContent(outputStream);		
		byte[] fileData = outputStream.toByteArray();
		Assert.assertEquals(fileData, expData);			
	}
	
	
}