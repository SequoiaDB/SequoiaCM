package com.sequoiacm.scmfile.oper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
 * test content: update Content,then delete defineAttributes 
 * testlink-case:SCM-1940
 * 
 * @author wuyan
 * @Date 2018.07.11
 * @version 1.00
 */

public class UpdateContentAndDefineAttr1940 extends TestScmBase {	
	private boolean runSuccess = false;
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;	
	private ScmId fileId = null;
	private ArrayList<ScmAttribute> attrList = new ArrayList<ScmAttribute>();
	private String fileName = "updatefile1940";
	private String className = "class1940";
	private ScmId scmClassId = null;
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
		ScmClassProperties properties = setDefineAttr();
		byte[] updateData1 = new byte[1024 * 2];
		byte[] updateData2 = new byte[1024 * 100];
		updateContentAndCheckAttr(updateData1,properties);
		updateContentAndCheckAttr(updateData2,properties);
		
		deleteProperties();
		runSuccess = true;
	}

	@AfterClass
	private void tearDown() {
		try {
			if( runSuccess ){
				//clean property and class
				ScmFactory.Class.deleteInstance(ws, scmClassId);
				for (ScmAttribute attribute : attrList) {
					ScmFactory.Attribute.deleteInstance(ws, attribute.getId());
				}
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
		
	private ScmClassProperties setDefineAttr() throws ScmException{
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		//set property
		String attrStr1 = "{name:'attr1940_string', display_name:'name1940_1', description:'Attribute 1940_1', type:'STRING', required:true}";
		String attrStr2 = "{name:'attr1940_date', display_name:'name1940_2', description:'Attribute 1938_2', type:'DATE', required:false}";
		String attrStr3 = "{name:'attr1940_double', display_name:'name1940_3', description:'中文', type:'DOUBLE', required:false}";
		String attrStr4 = "{name:'attr1940_int', display_name:'name1940_4', description:'test int', type:'INTEGER', required:true}";
		String attrStr5 = "{name:'attr1940_bool', display_name:'name1940_5', description:'test bool', type:'BOOLEAN', required:true}";
		
		attrList.add(createAttr(attrStr1));
		attrList.add(createAttr(attrStr2));
		attrList.add(createAttr(attrStr3));
		attrList.add(createAttr(attrStr4));
		attrList.add(createAttr(attrStr5));
			
		ScmClass scmClass = ScmFactory.Class.createInstance(ws, className, "i am a class1940");		
		scmClassId = scmClass.getId();		

		for (ScmAttribute attribute : attrList) {
			scmClass.attachAttr(attribute.getId());
		}
		ScmClassProperties properties = new ScmClassProperties(scmClassId.toString());
		properties.addProperty(attrList.get(0).getName(), "test1940 set define attr!");
		properties.addProperty(attrList.get(1).getName(), "2018-07-11-11:01:00.000");
		properties.addProperty(attrList.get(2).getName(), 1940.01);
		properties.addProperty(attrList.get(3).getName(), 2147483647);
		properties.addProperty(attrList.get(4).getName(), true);
		file.setClassProperties(properties);
		
		//check property
		Assert.assertEquals(file.getClassProperties().toString(), properties.toString());	
		return properties;
		
	}
	private void updateContentAndCheckAttr(byte[] updateData,ScmClassProperties properties) throws Exception{
		//update content
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);		
		new Random().nextBytes(updateData);		
		file.updateContent(new ByteArrayInputStream(updateData));
		
		//check update content
		int majorVersion = file.getMajorVersion();
		VersionUtils.CheckFileContentByStream( ws, fileName, majorVersion ,updateData);
		
		//check property 
		checkSetPropertyResult(scmClassId, properties);			
	}
	
	private void deleteProperties() throws ScmException {
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		ScmClassProperties properties = file.getClassProperties();	
		//only non-required attributes can be deleted
		properties.deleteProperty(attrList.get(1).getName());
		properties.deleteProperty(attrList.get(2).getName());
		
		file.setClassProperties(properties);		
	   
		// check results
		file = ScmFactory.File.getInstance(ws, fileId);	
		ScmClassProperties getProperties = file.getClassProperties();
		Assert.assertEquals(getProperties.toString(), properties.toString());	
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