
package com.sequoiacm.scmfile.oper;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmClassProperties;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @Description:SCM-1944 :: SCM-1946 :: 通过断点文件更新文件，添加/更新/删除自定义属性和标签 
 * @author fanyu
 * @Date:2018年7月11日
 * @version:1.0
 */
public class UpdateFileThenCUDTagAttr1946 extends TestScmBase {
	private boolean runSuccess = false;
	private SiteWrapper site = null;
	private WsWrapper wsp = null;
	private ScmSession session = null;
	private ScmWorkspace ws = null;
	private String name = "UpdateFileThenCUDTagAttr1946";
	private int fileSize = 1024*5;
	private ScmId fileId = null;
	private ScmTags tags = null;
	private ScmClass class1 = null;
	private ScmAttribute attr = null;
	private ScmClassProperties properties = null;

	private File localPath = null;
	private String filePath = null;

	@BeforeClass
	private void setUp() throws IOException, ScmException {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";

		TestTools.LocalFile.removeFile(localPath);
		TestTools.LocalFile.createDir(localPath.toString());
		TestTools.LocalFile.createFile(filePath, fileSize);

		List<SiteWrapper> siteList = ScmInfo.getAllSites();
		for (int i = 0; i < siteList.size(); i++) {
			if (siteList.get(i).getDataType() == DatasourceType.SEQUOIADB) {
				site = siteList.get(i);
				break;
			}
			if(i ==  siteList.size()-1){
				throw new SkipException("NO Sequoiadb Datasourse, Skip!");
			}
		}
		site = ScmInfo.getRootSite();
		wsp = ScmInfo.getWs();
		session = TestScmTools.createSession(site);
		ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);

		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(name).get();
		ScmFileUtils.cleanFile(wsp, cond);
		tags = new ScmTags();
		tags.addTag(name);
		attr = craeteAttr(name);
		class1 = ScmFactory.Class.createInstance(ws, name, name + "_desc");
		class1.attachAttr(attr.getId());
		properties = createProperties(class1, 5);
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() throws Exception {
		//create file and breakpointFile
		createFile(name);
		ScmBreakpointFile breakpointFile = createBreakpointFile(name, filePath);
		//update file
		updateFile(fileId,breakpointFile,properties,tags);
		check(fileId, tags,properties);
		// update
		tags.removeTag(name);
		tags.addTag(name+"-update");
		properties.addProperty(name, 20);
		updateFile(fileId, breakpointFile,properties,tags);
		check(fileId,tags,properties);
		// delete
		tags.removeTag(name+"-update");
		properties.deleteProperty(name);
		updateFile(fileId, breakpointFile,properties,tags);
		checkDel(fileId);
		runSuccess = true;
	}

	@AfterClass
	private void tearDown() throws ScmException {
		try {
			if (!runSuccess || TestScmBase.forceClear) {
				if (fileId != null ) {
					System.out.println("fileId = " + fileId.get());
				}
			}
			ScmFactory.File.deleteInstance(ws, fileId, true);
			ScmFactory.Class.deleteInstance(ws, class1.getId());
			ScmFactory.Attribute.deleteInstance(ws, attr.getId());
			TestTools.LocalFile.removeFile(localPath);
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}

	private ScmBreakpointFile createBreakpointFile(String name, String filePath) throws ScmException {
		// create file
		ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile.createInstance(ws, name);
		breakpointFile.upload(new File(filePath));
		return breakpointFile;
	}
	
	private ScmId createFile(String name) throws ScmException {
		ScmFile file = ScmFactory.File.createInstance(ws);
		file.setFileName(name);
		file.setAuthor(name);
		fileId = file.save();
		return fileId;
	}

	private void updateFile(ScmId fileId, ScmBreakpointFile breakpointFile, ScmClassProperties properties, ScmTags tag)
			throws ScmException {
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		try {
			file.updateContent(breakpointFile);
		} catch (ScmException e) {
			if (e.getError() != ScmError.INVALID_ARGUMENT) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}

		}
		file.setTags(tag);
		file.setClassProperties(properties);
	}

	
	private ScmClassProperties createProperties(ScmClass class1, int value) {
		ScmClassProperties properties = new ScmClassProperties(class1.getId().get());
		for (int i = 0; i < class1.listAttrs().size(); i++) {
			properties.addProperty(class1.listAttrs().get(i).getName(), value);
		}
		return properties;
	}

	private ScmAttribute craeteAttr(String name) throws ScmException {
		ScmAttributeConf conf = new ScmAttributeConf();
		conf.setName(name);
		conf.setDescription(name + "_desc");
		conf.setDisplayName(name + "_display");
		conf.setRequired(false);
		conf.setType(AttributeType.INTEGER);
		ScmAttribute attr = ScmFactory.Attribute.createInstance(ws, conf);
		return attr;
	}

	private void check(ScmId fileId, ScmTags tag,ScmClassProperties properties) throws ScmException {
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		Assert.assertEquals(file.getFileName(), name);
		Assert.assertEquals(file.getAuthor(), name);
		Assert.assertEquals(file.getSize(), fileSize);
		Assert.assertEquals(file.getTags().toSet().size(),1);
		Assert.assertEquals(file.getTags().toString(), tag.toString());
		Assert.assertEquals(file.getClassProperties().keySet().size(), 1);
		Assert.assertEquals(file.getClassProperties().getProperty(name), properties.getProperty(name));
	}

	private void checkDel(ScmId fileId) throws ScmException {
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		ScmTags tag = file.getTags();
		Assert.assertEquals(tag.toSet().size(),0,tag.toSet().toString());
		ScmClassProperties properties = file.getClassProperties();
		Assert.assertFalse(properties.contains(name));
	}
}
