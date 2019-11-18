package com.sequoiacm.definemeta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmClass;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmClassBasicInfo;
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

/**
 * @Testcase: SCM-1722:获取模型信息
 * @author huangxiaoni init
 * @date 2017.6.18
 */

public class DefineAttr_class_getClassInfo1722 extends TestScmBase {
	private boolean runSuccess = false;

	private String name = "definemeta1722";
	private String CLASS_ID = null;
	private ScmClass class1 = null;
	private List<ScmAttribute> attrs = new ArrayList<ScmAttribute>();
	private String[] types = { "date", "string", "boolean", "double", "int", "string" };
	private String[] names = { "test_attr_name_date_1722", "test_attr_name_string1722", "test_attr_name_bool_1722",
			"test_attr_name_double_1722", "test_attr_name_int_1722", "test_attr_name_string_1722" };
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() throws IOException, ScmException {
		site = ScmInfo.getSite();
		wsp = ScmInfo.getWs();
		session = TestScmTools.createSession(site);
		ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
		class1 = ScmFactory.Class.createInstance(ws, name, name);
		CLASS_ID = class1.getId().get();
		// create attr and class attach attr
		for (int i = 0; i < types.length; i++) {
			createModel(names[i], types[i], false);
		}
	}

	@Test
	private void test_getInstance() throws ScmException {
		try {
			ScmClass scmClass = ScmFactory.Class.getInstance(ws, new ScmId(CLASS_ID));
			Assert.assertEquals(scmClass.getId().get(), CLASS_ID);
			Assert.assertEquals(scmClass.getName(), name);
			Assert.assertNotNull(scmClass.getCreateTime());
			Assert.assertEquals(scmClass.getCreateUser(), TestScmBase.scmUserName);
			Assert.assertEquals(scmClass.getWorkspace().getName(), wsp.getName());
			Assert.assertEquals(scmClass.listAttrs().toString(), attrs.toString());
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		runSuccess = true;
	}

	@Test
	private void test_listInstance() throws ScmException {
		try {
			BSONObject matcher = ScmQueryBuilder.start(ScmAttributeName.Class.NAME).is(name).get();
			ScmCursor<ScmClassBasicInfo> cursor = ScmFactory.Class.listInstance(ws, matcher);
			int cursorSize = 0;
			while (cursor.hasNext()) {
				cursorSize++;
				ScmClassBasicInfo info = cursor.getNext();
				Assert.assertEquals(info.getId().get(), CLASS_ID);
				Assert.assertEquals(info.getName(), name);
			}
			Assert.assertEquals(cursorSize, 1);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws ScmException {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				ScmFactory.Class.deleteInstance(ws, class1.getId());
				for (ScmAttribute attr : attrs) {
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
		attrs.add(attr);
	}
}