
package com.sequoiacm.definemeta;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmClass;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.element.metadata.ScmIntegerRule;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiadb.exception.BaseException;

/**
 * @Description: SCM-1873 :: 模型解除属性 
 * @author fanyu
 * @Date:2018年7月4日
 * @version:1.0
 */
public class DefineAttr_Class_ClassDetachAttr1873 extends TestScmBase {
	private boolean runSuccess = false;
	private String name = "ClassDetachAttr1873";
	private String desc = "ClassDetachAttr1873";
	private List<ScmClass> classList = new ArrayList<ScmClass>();
	private List<ScmAttribute> attrList = new ArrayList<ScmAttribute>();
	private int num = 2;
	
	private SiteWrapper site = null;
	private WsWrapper wsp = null;
	private ScmSession session = null;
	private ScmWorkspace ws = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			site = ScmInfo.getSite();
			wsp = ScmInfo.getWs();
			session = TestScmTools.createSession(site);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
			for (int i = 0; i < num; i++) {
				ScmClass class1 = ScmFactory.Class.createInstance(ws, name + "_" + i, desc);
				classList.add(class1);
				attrList.add(craeteAttr( name + "_" + i));
			}
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() {
		//one to one
		try {
			 classList.get(0).attachAttr(attrList.get(0).getId());
			 classList.get(0).detachAttr(attrList.get(0).getId());
			 check(classList.get(0), attrList.subList(0,0));
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		
		// one to two
		try {
			classList.get(0).attachAttr(attrList.get(0).getId());
			classList.get(0).attachAttr(attrList.get(1).getId());
			classList.get(0).detachAttr(attrList.get(0).getId());
			check(classList.get(0), attrList.subList(1, 2));
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		
		// two to two
		try {
			classList.get(0).attachAttr(attrList.get(0).getId());
			classList.get(1).attachAttr(attrList.get(1).getId());
			classList.get(0).detachAttr(attrList.get(0).getId());
			check(classList.get(1), attrList.subList(0, 1));
			check(classList.get(0), attrList.subList(1, 2));
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				for (int i = 0; i < num; i++) {
					ScmFactory.Class.deleteInstance(ws, classList.get(i).getId());
					ScmFactory.Attribute.deleteInstance(ws, attrList.get(i).getId());
				}
			}
			if (!runSuccess && classList.size() != 0) {
				System.out.println("class = " + classList.toString());
				for (int i = 0; i < num; i++) {
					ScmFactory.Class.deleteInstance(ws, classList.get(i).getId());
					ScmFactory.Attribute.deleteInstance(ws, attrList.get(i).getId());
				}
			}
		} catch (BaseException | ScmException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}
	
	private ScmAttribute craeteAttr(String name) throws ScmException {
		ScmAttributeConf conf = new ScmAttributeConf();
		conf.setName(name);
		conf.setDescription(desc);
		conf.setDisplayName(name + "_display");
		conf.setRequired(true);
		conf.setType(AttributeType.INTEGER);

		ScmIntegerRule rule = new ScmIntegerRule();
		rule.setMinimum(0);
		rule.setMaximum(10);
		conf.setCheckRule(rule);
		ScmAttribute attr = ScmFactory.Attribute.createInstance(ws, conf);
		return attr;
	}
	
	private void check(ScmClass class1, List<ScmAttribute> attrList) {
		try {
			ScmClass class2 = ScmFactory.Class.getInstance(ws, class1.getId());
			List<ScmAttribute> list = class2.listAttrs();
			Assert.assertEquals(list.size(), attrList.size(),list.toString());
			for(int i = 0; i < list.size(); i++){
				Assert.assertEquals(list.get(i).getDescription(), attrList.get(i).getDescription());
			}
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
}
