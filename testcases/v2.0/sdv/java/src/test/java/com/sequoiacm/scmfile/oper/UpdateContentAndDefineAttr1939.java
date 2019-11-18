package com.sequoiacm.scmfile.oper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.bson.BSONObject;
import org.bson.util.JSON;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmClass;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmClassProperties;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content: update Content,then update defineAttributes and add new defineAttributes
 * testlink-case:SCM-1939
 * 
 * @author wuyan
 * @Date 2018.07.11
 * @version 1.00
 */

public class UpdateContentAndDefineAttr1939 extends TestScmBase {	
	private boolean runSuccess = false;
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;	
	private ScmId fileId = null;
	private String fileName = "updatefile1939";
	private String className = "class1939";
	private ScmId scmClassId = null;
	private ScmId setAttrId = null;
	private ScmId updateAttrId = null;
	private int fileSize = 1024 * 10;
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
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite"})
	private void test() throws Exception {
		fileId = VersionUtils.createFileByFile( ws, fileName, filePath );
		String oldAttrName = setDefineAttr();
		updateContentAndSetAttr(oldAttrName);
		runSuccess = true;
	}

	@AfterClass
	private void tearDown() {
		try {
			if( runSuccess ){
				//clean property and class
				ScmFactory.Class.deleteInstance(ws, scmClassId);
				ScmFactory.Attribute.deleteInstance(ws, setAttrId);
				ScmFactory.Attribute.deleteInstance(ws, updateAttrId);
				ScmFactory.File.deleteInstance(ws, fileId, true);		
				TestTools.LocalFile.removeFile(localPath);
			}			
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}			
		}
	}	
		
	private String setDefineAttr() throws ScmException{
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		//set property
		String attrStr = "{name:'attr1939_string', display_name:'name1939_1', description:'Attribute 1938_1', type:'STRING', required:true}";
		ScmAttribute attribute = createAttr(attrStr);		
		ScmClass scmClass = ScmFactory.Class.createInstance(ws, className, "i am a class1939");
		setAttrId = attribute.getId();
		scmClass.attachAttr(setAttrId);
		scmClassId = scmClass.getId();
		ScmClassProperties properties = new ScmClassProperties(scmClassId.toString());
		properties.addProperty(attribute.getName(), "test1939 set define attr!");
		file.setClassProperties(properties);
		
		//check property
		Assert.assertEquals(file.getClassProperties().toString(), properties.toString());		
		return attribute.getName();
	}
	private void updateContentAndSetAttr(String oldAttrName) throws Exception{
		//update content
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		byte[] testdata = new byte[1024 * 2];
		new Random().nextBytes(testdata);		
		file.updateContent(new ByteArrayInputStream(testdata));
		
		//set new property
		String attrStr = "{name:'attr1939_date', display_name:'dispalyName1939_2', description:'I am a Attribute 1939_2', type:'DATE', required:false}";
		ScmAttribute attribute = createAttr(attrStr);		
		ScmClass scmClass = ScmFactory.Class.getInstance(ws, scmClassId);
		updateAttrId = attribute.getId();
		scmClass.attachAttr(updateAttrId);			
		ScmClassProperties properties = new ScmClassProperties(scmClassId.toString());		
		properties.addProperty(attribute.getName(), "2018-07-11-01:01:00.000");
		properties.addProperty(oldAttrName, "update the defineAttr !");		
		file.setClassProperties(properties);		
		
		//check update content
		int majorVersion = file.getMajorVersion();
		VersionUtils.CheckFileContentByStream( ws, fileName, majorVersion ,testdata);
		
		//check property 
		checkSetPropertyResult(scmClassId, properties);			
	}
	
	private void checkSetPropertyResult(ScmId scmClassId, ScmClassProperties expProperty) throws ScmException{
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		Assert.assertEquals(file.getClassId(), scmClassId);
		Assert.assertEquals(file.getClassProperties().toString(), expProperty.toString());		
	}	
	
	private ScmAttribute createAttr(String attrObjString) throws ScmException{
		BSONObject attrObj = (BSONObject) JSON.parse(attrObjString);
		ScmAttributeConf attr = new ScmAttributeConf();
		attr.setName(attrObj.get("name").toString());
		attr.setType(AttributeType.getType(attrObj.get("type").toString()));
		attr.setDescription(attrObj.get("description").toString());
		attr.setDisplayName(attrObj.get("display_name").toString());
		attr.setCheckRule(null);
		attr.setRequired((boolean)attrObj.get("required"));		
		return ScmFactory.Attribute.createInstance(ws, attr);
	}
}