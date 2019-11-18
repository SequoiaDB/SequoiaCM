package com.sequoiacm.scmfile.oper;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * test content: update Content,and add tag
 * testlink-case:SCM-1941
 * 
 * @author wuyan
 * @Date 2018.07.11
 * @version 1.00
 */

public class UpdateContentAndAddTag1941 extends TestScmBase {	
	private boolean runSuccess = false;
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;	
	private ScmId fileId = null;
	private String fileName = "updatefile1941";
	private String authorName = "updatefile1941";
	private int fileSize = 1024 * 3;
	private File localPath = null;
	private String filePath = null;		

	@BeforeClass
	private void setUp() throws IOException, ScmException {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";

		TestTools.LocalFile.removeFile(localPath);
		TestTools.LocalFile.createDir(localPath.toString());
		TestTools.LocalFile.createFile(filePath, fileSize);

		site = ScmInfo.getSite();		
		wsp = ScmInfo.getWs();
		session = TestScmTools.createSession(site);
		ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);		
		
		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
		ScmFileUtils.cleanFile(wsp, cond);
		fileId = VersionUtils.createFileByFile( ws, fileName, filePath, authorName );
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite"})
	private void test( ) throws Exception {		
		updateContentAndAddTag();	
		runSuccess = true;
	}

	@AfterClass
	private void tearDown() throws ScmException {
		try {
			if( runSuccess ){
				ScmFactory.File.deleteInstance(ws, fileId, true);		
				TestTools.LocalFile.removeFile(localPath);
			}			
		}  finally {
			if (session != null) {
				session.close();
			}			
		}
	}
	
	private void updateContentAndAddTag() throws Exception{
		//update content
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		byte[] testdata = new byte[1024 * 2];
		new Random().nextBytes(testdata);		
		file.updateContent(new ByteArrayInputStream(testdata));	
		
		//add tags
		ScmTags tags = new ScmTags();
		tags.addTag("我是一个标签1941                                                                                                                                                                                      "
				+ "                                ");
		tags.addTag("THIS IS TAG 1941!");
		tags.addTag( "tag *&^^^^^*90234@#$%!~asf");
		file.setTags(tags);
		
		//check update content
		int majorVersion = file.getMajorVersion();
		VersionUtils.CheckFileContentByStream(  ws, fileName, majorVersion ,testdata);		
		
		//check tags
		file = ScmFactory.File.getInstance(ws, fileId);
		ScmTags fileTags = file.getTags();
		Assert.assertEquals(fileTags.toString(), tags.toString());		
	}
	
}