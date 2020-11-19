package com.sequoiacm.metadata;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmClass;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.element.metadata.ScmIntegerRule;
import com.sequoiacm.client.element.metadata.ScmStringRule;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

/**
 * 查询模型详细信息
 * @author yanglei
 *
 **/
public class TestGetClassInfo extends ScmTestMultiCenterBase {
    
    private final static Logger logger = LoggerFactory.getLogger(TestGetClassInfo.class);
    private ScmSession ss;
    private ScmWorkspace ws;
    private ScmClass scmClass;
    private String attrName1 = "test_get_class_attr001";
    private String attrName2 = "test_get_class_attr002";
    private ScmId attrId1;
    private ScmId attrId2;
    
    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
        
        scmClass = ScmFactory.Class.createInstance(ws, ScmTestTools.getClassName(), "查询模型详细信息");
        
        ScmAttributeConf conf = new ScmAttributeConf().setName(attrName1)
                .setType(AttributeType.STRING).setCheckRule(new ScmStringRule(10));
        ScmAttribute attr = ScmFactory.Attribute.createInstance(ws, conf);
        attrId1 = attr.getId();
        scmClass.attachAttr(attrId1);
        
        conf.setName(attrName2).setType(AttributeType.INTEGER).setCheckRule(null)
                .setDisplayName("属性字段2").setDescription("属性字段2").setRequired(true);
        attr = ScmFactory.Attribute.createInstance(ws, conf);
        attrId2 = attr.getId();
        scmClass.attachAttr(attrId2);
        
        logger.info("create class:" + scmClass.toString());
    }

    @Test
    public void testGetClass() throws ScmException, IOException {
        ScmClass savedClass = ScmFactory.Class.getInstance(ws, scmClass.getId());
        
//        Assert.assertEquals(savedClass.toString(), scmClass.toString());
        Assert.assertEquals(savedClass.getId(), scmClass.getId());
        Assert.assertEquals(savedClass.getName(), ScmTestTools.getClassName());
        Assert.assertEquals(savedClass.getDescription(), "查询模型详细信息");
        Assert.assertEquals(savedClass.getCreateUser(), getScmUser());
        Assert.assertEquals(savedClass.getUpdateUser(), getScmUser());
        Assert.assertEquals(savedClass.getWorkspace().getName(), getWorkspaceName());
        Assert.assertEquals(savedClass.isExist(), true);
        List<ScmAttribute> attrs = savedClass.listAttrs();
        Assert.assertEquals(attrs.size(), 2);
        
        for (ScmAttribute scmAttribute : attrs) {
            if (attrName1.equals(scmAttribute.getName())) {
                Assert.assertEquals(scmAttribute.getId(), attrId1);
                Assert.assertEquals(scmAttribute.getDisplayName(), "");
                Assert.assertEquals(scmAttribute.getDescription(), "");
                Assert.assertEquals(scmAttribute.getType(), AttributeType.STRING);
                ScmStringRule rule = (ScmStringRule) scmAttribute.getCheckRule();
                Assert.assertEquals(rule.getMaxLength(), 10);
                Assert.assertEquals(scmAttribute.isRequired(), false);
                Assert.assertEquals(savedClass.isExist(), true);
            }
            else if (attrName2.equals(scmAttribute.getName())) {
                Assert.assertEquals(scmAttribute.getId(), attrId2);
                Assert.assertEquals(scmAttribute.getDisplayName(), "属性字段2");
                Assert.assertEquals(scmAttribute.getDescription(), "属性字段2");
                Assert.assertEquals(scmAttribute.getType(), AttributeType.INTEGER);
                ScmIntegerRule checkRule = (ScmIntegerRule) scmAttribute.getCheckRule();
                Assert.assertEquals(checkRule.getMinimum(), Integer.MIN_VALUE);
                Assert.assertEquals(checkRule.getMaximum(), Integer.MAX_VALUE);
                Assert.assertEquals(scmAttribute.isRequired(), true);
                Assert.assertEquals(savedClass.isExist(), true);
            }
            else {
                Assert.fail("found unknown attribute");
            }
        }
    }
    
    @Test
    public void testGetClass1() throws ScmException, IOException {
        ScmClass savedClass = ScmFactory.Class.getInstanceByName(ws, scmClass.getName());

        // Assert.assertEquals(savedClass.toString(), scmClass.toString());
        Assert.assertEquals(savedClass.getId(), scmClass.getId());
        Assert.assertEquals(savedClass.getName(), ScmTestTools.getClassName());
        Assert.assertEquals(savedClass.getDescription(), "查询模型详细信息");
        Assert.assertEquals(savedClass.getCreateUser(), getScmUser());
        Assert.assertEquals(savedClass.getUpdateUser(), getScmUser());
        Assert.assertEquals(savedClass.getWorkspace().getName(), getWorkspaceName());
        Assert.assertEquals(savedClass.isExist(), true);
        List<ScmAttribute> attrs = savedClass.listAttrs();
        Assert.assertEquals(attrs.size(), 2);

        for (ScmAttribute scmAttribute : attrs) {
            if (attrName1.equals(scmAttribute.getName())) {
                Assert.assertEquals(scmAttribute.getId(), attrId1);
                Assert.assertEquals(scmAttribute.getDisplayName(), "");
                Assert.assertEquals(scmAttribute.getDescription(), "");
                Assert.assertEquals(scmAttribute.getType(), AttributeType.STRING);
                ScmStringRule rule = (ScmStringRule) scmAttribute.getCheckRule();
                Assert.assertEquals(rule.getMaxLength(), 10);
                Assert.assertEquals(scmAttribute.isRequired(), false);
                Assert.assertEquals(savedClass.isExist(), true);
            }
            else if (attrName2.equals(scmAttribute.getName())) {
                Assert.assertEquals(scmAttribute.getId(), attrId2);
                Assert.assertEquals(scmAttribute.getDisplayName(), "属性字段2");
                Assert.assertEquals(scmAttribute.getDescription(), "属性字段2");
                Assert.assertEquals(scmAttribute.getType(), AttributeType.INTEGER);
                ScmIntegerRule checkRule = (ScmIntegerRule) scmAttribute.getCheckRule();
                Assert.assertEquals(checkRule.getMinimum(), Integer.MIN_VALUE);
                Assert.assertEquals(checkRule.getMaximum(), Integer.MAX_VALUE);
                Assert.assertEquals(scmAttribute.isRequired(), true);
                Assert.assertEquals(savedClass.isExist(), true);
            }
            else {
                Assert.fail("found unknown attribute");
            }
        }
    }

    @Test
    public void testGetUnexistClass() throws ScmException, IOException {
        ScmId unexistId = new ScmId("ffffffffffffffffffffffff");
        try {
            ScmFactory.Class.getInstance(ws, unexistId);
            Assert.fail("get not exist class should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CLASS_NOT_EXIST, e.getMessage());
        }
    }
    
    @AfterClass
    public void tearDown() throws ScmException {
        try {
            ScmFactory.Class.deleteInstance(ws, scmClass.getId());
            ScmFactory.Attribute.deleteInstance(ws, attrId1);
            ScmFactory.Attribute.deleteInstance(ws, attrId2);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            ScmTestTools.releaseSession(ss);
        }
    }
    
}