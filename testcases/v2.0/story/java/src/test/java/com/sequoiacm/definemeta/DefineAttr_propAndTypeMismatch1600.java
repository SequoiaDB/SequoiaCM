package com.sequoiacm.definemeta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmClass;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmClassProperties;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.element.metadata.ScmDoubleRule;
import com.sequoiacm.client.element.metadata.ScmIntegerRule;
import com.sequoiacm.client.element.metadata.ScmStringRule;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase:  SCM-1600:上传文件配置的自定义属性值跟该属性类型不匹配
 * @author huangxiaoni init
 * @date 2017.6.18
 */

public class DefineAttr_propAndTypeMismatch1600 extends TestScmBase {
	private static final Logger logger = Logger.getLogger(DefineAttr_propAndTypeMismatch1600.class);
	private boolean runSuccess = false;
	private int failTimes = 0;

	private static final String NAME = "definemeta1600";
	private Map<String, Object> attrMap = new HashMap<>();
	private String CLASS_ID = null;

	private ScmClass class1 = null;
	private List<ScmAttribute> attrList = new ArrayList<ScmAttribute>();

	
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;
	private ScmId fileId = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() throws IOException, ScmException {		
		site = ScmInfo.getSite();
		wsp = ScmInfo.getWs();
		session = TestScmTools.createSession(site);
		ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);

		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(NAME).get();
		ScmFileUtils.cleanFile(wsp, cond);
		
		class1 = ScmFactory.Class.createInstance(ws, NAME, NAME + "_desc");
		CLASS_ID = class1.getId().get();

		// create class properties
		createModel("test_attr_name_int_1600", "int", false);
		createModel("test_attr_name_string_1600", "string", false);
		createModel("test_attr_name_date_1600", "date", false);
		createModel("test_attr_name_bool_1600", "boolean", false);
		createModel("test_attr_name_double_1600", "double", false);

		ScmFile file = ScmFactory.File.createInstance(ws);
		file.setFileName(NAME);
		ScmClassProperties properties = new ScmClassProperties(CLASS_ID);
		file.setClassProperties(properties);
		fileId = file.save();
	}
	
	@BeforeMethod
	private void initMethod() {
		attrMap.clear();
		
		if (!runSuccess) {
			failTimes++;
		}
		runSuccess = false;
	}
	
	@AfterMethod
	private void afterMethod() {
		if (failTimes > 1) {
			runSuccess = false;
		}
	}

	@Test
	private void test_int() throws Exception {		
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		try {
			file.setClassProperty("test_attr_name_int_1600", "test");
			Assert.fail("expect failed but actual succ.");
		} catch (ScmException e) { 
			logger.info("prop and type mismatch, errorMsg = [" + e.getError() + "]");
		} 	
		
		try {
			file.setClassProperty("test_attr_name_int_1600", null);
			Assert.fail("expect failed but actual succ.");
		} catch (ScmException e) { 
			logger.info("prop and type mismatch, errorMsg = [" + e.getError() + "]");
		} 
		runSuccess = true;
	}

	@Test
	private void test_string() throws Exception {		
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		try {
			file.setClassProperty("test_attr_name_string_1600", 123);
			Assert.fail("expect failed but actual succ.");
		} catch (ScmException e) { 
			logger.info("prop and type mismatch, errorMsg = [" + e.getError() + "]");
		} 	
		
		try {
			file.setClassProperty("test_attr_name_string_1600", null);
			Assert.fail("expect failed but actual succ.");
		} catch (ScmException e) { 
			logger.info("prop and type mismatch, errorMsg = [" + e.getError() + "]");
		} 
		runSuccess = true;
	}

	@Test
	private void test_date() throws Exception {		
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		try {
			file.setClassProperty("test_attr_name_date_1600", 2147483647);
			Assert.fail("expect failed but actual succ.");
		} catch (ScmException e) { 
			logger.info("prop and type mismatch, errorMsg = [" + e.getError() + "]");
		} 	
		
		try {
			file.setClassProperty("test_attr_name_date_1600", new Date());
			Assert.fail("expect failed but actual succ.");
		} catch (ScmException e) { 
			logger.info("prop and type mismatch, errorMsg = [" + e.getError() + "]");
		} 			
		
		try {
			file.setClassProperty("test_attr_name_date_1600", new Date());
			Assert.fail("expect failed but actual succ.");
		} catch (ScmException e) { 
			logger.info("prop and type mismatch, errorMsg = [" + e.getError() + "]");
		} 	
		
		try {
			file.setClassProperty("test_attr_name_date_1600", null);
			Assert.fail("expect failed but actual succ.");
		} catch (ScmException e) { 
			logger.info("prop and type mismatch, errorMsg = [" + e.getError() + "]");
		} 
		runSuccess = true;
	}

	@Test
	private void test_bool() throws Exception {		
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		try {
			file.setClassProperty("test_attr_name_bool_1600", "test");
			Assert.fail("expect failed but actual succ.");
		} catch (ScmException e) { 
			logger.info("prop and type mismatch, errorMsg = [" + e.getError() + "]");
		} 

		try {
			file.setClassProperty("test_attr_name_bool_1600", "");
			Assert.fail("expect failed but actual succ.");
		} catch (ScmException e) { 
			logger.info("prop and type mismatch, errorMsg = [" + e.getError() + "]");
		} 
		
		try {
			file.setClassProperty("test_attr_name_bool_1600", null);
			Assert.fail("expect failed but actual succ.");
		} catch (ScmException e) { 
			logger.info("prop and type mismatch, errorMsg = [" + e.getError() + "]");
		} 
		runSuccess = true;
	}

	@Test
	private void test_double() throws Exception {		
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		try {
			file.setClassProperty("test_attr_name_double_1600", "1.0");
			Assert.fail("expect failed but actual succ.");
		} catch (ScmException e) { 
			logger.info("prop and type mismatch, errorMsg = [" + e.getError() + "]");
		} 

		try {
			file.setClassProperty("test_attr_name_double_1600", 1);
			Assert.fail("expect failed but actual succ.");
		} catch (ScmException e) { 
			logger.info("prop and type mismatch, errorMsg = [" + e.getError() + "]");
		} 
		
		try {
			file.setClassProperty("test_attr_name_double_1600", null);
			Assert.fail("expect failed but actual succ.");
		} catch (ScmException e) { 
			logger.info("prop and type mismatch, errorMsg = [" + e.getError() + "]");
		} 
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws ScmException {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				ScmFactory.File.deleteInstance(ws, fileId, true);
				ScmFactory.Class.deleteInstance(ws, class1.getId());
				for (ScmAttribute attr : attrList) {
					ScmFactory.Attribute.deleteInstance(ws, attr.getId());
				}
			}
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}
	
	private void createModel(String name, String type, boolean required) throws ScmException {
		// createattr
		ScmAttributeConf conf = new ScmAttributeConf();
		conf.setName(name);
		conf.setDescription(name);
		conf.setDisplayName(name + "_display");
		conf.setRequired(required);
		switch (type) {
		case "int":
			conf.setType(AttributeType.INTEGER);
			ScmIntegerRule rule = new ScmIntegerRule();
			rule.setMinimum(0);
			rule.setMaximum(100);
			conf.setCheckRule(rule);
			break;
		case "string":
			conf.setType(AttributeType.STRING);
			ScmStringRule rule1 = new ScmStringRule(10);
			conf.setCheckRule(rule1);
			break;
		case "date":
			conf.setType(AttributeType.DATE);
			break;
		case "boolean":
			conf.setType(AttributeType.BOOLEAN);
			break;
		case "double":
			conf.setType(AttributeType.DOUBLE);
			ScmDoubleRule rule2 = new ScmDoubleRule();
			rule2.setMinimum(0L);
			rule2.setMaximum(100L);
			conf.setCheckRule(rule2);
			break;
		}
		ScmAttribute attr = ScmFactory.Attribute.createInstance(ws, conf);
		// attr attch class
		class1.attachAttr(attr.getId());
		attrList.add(attr);
	}
}