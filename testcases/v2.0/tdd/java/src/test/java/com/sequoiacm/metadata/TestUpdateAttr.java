package com.sequoiacm.metadata;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmAttribute;
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

public class TestUpdateAttr extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestUpdateAttr.class);
    private ScmSession ss;
    private ScmWorkspace ws;
    private ScmId attrId;

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
        
        ScmAttributeConf attrConf = new ScmAttributeConf().setName(ScmTestTools.getClassName())
                .setDisplayName("").setDescription("").setRequired(false)
                .setType(AttributeType.INTEGER).setCheckRule(new ScmIntegerRule(0, 10));
        ScmAttribute attr = ScmFactory.Attribute.createInstance(ws, attrConf);
        attrId = attr.getId();
        logger.info("create attr:" + attr.toString());
    }

    @Test
    public void testUpdate() throws ScmException {
        ScmAttribute oldAttr = ScmFactory.Attribute.getInstance(ws, attrId);
        logger.info("oldClass: " + oldAttr.toString());
        Assert.assertEquals(oldAttr.isExist(), true);
        Assert.assertEquals(oldAttr.getName(), ScmTestTools.getClassName());
        Assert.assertEquals(oldAttr.getDescription(), "");
        Assert.assertEquals(oldAttr.getCreateUser(), getScmUser());
        Assert.assertEquals(oldAttr.getUpdateUser(), getScmUser());
        Assert.assertEquals(oldAttr.getDisplayName(), "");
        Assert.assertEquals(oldAttr.getType(), AttributeType.INTEGER);
        ScmIntegerRule rule = (ScmIntegerRule) oldAttr.getCheckRule();
        Assert.assertEquals(rule.getMinimum(), 0);
        Assert.assertEquals(rule.getMaximum(), 10);
        Assert.assertEquals(oldAttr.isRequired(), false);
        Assert.assertEquals(oldAttr.getCreateTime(), oldAttr.getUpdateTime());
        
        
        // set new value
        Date tmpTime = oldAttr.getUpdateTime();
        oldAttr.setDisplayName("display");
        Assert.assertNotEquals(oldAttr.getUpdateTime(), tmpTime,
                "create and update time should not equal");
        oldAttr.setDescription("中文描述");
        oldAttr.setRequired(true);
        oldAttr.setCheckRule(new ScmIntegerRule(10, 20));
        
        ScmAttribute newAttr = ScmFactory.Attribute.getInstance(ws, attrId);
        logger.info("oldClass: " + newAttr.toString());
        Assert.assertEquals(newAttr.isExist(), true);
        Assert.assertEquals(newAttr.getName(), ScmTestTools.getClassName());
        Assert.assertEquals(newAttr.getDescription(), "中文描述");
        Assert.assertEquals(newAttr.getCreateUser(), getScmUser());
        Assert.assertEquals(newAttr.getUpdateUser(), getScmUser());
        Assert.assertEquals(newAttr.getDisplayName(), "display");
        Assert.assertEquals(newAttr.getType(), AttributeType.INTEGER);
        rule = (ScmIntegerRule) oldAttr.getCheckRule();
        Assert.assertEquals(rule.getMinimum(), 10);
        Assert.assertEquals(rule.getMaximum(), 20);
        Assert.assertEquals(newAttr.isRequired(), true);
        Assert.assertNotEquals(newAttr.getCreateTime(), newAttr.getUpdateTime(), 
                "create and update time should not equal");
        
        
        /*
         *  test set null
         */
        try {
            newAttr.setDisplayName(null);
            Assert.fail("set attr display_name=null should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }
        
        try {
            newAttr.setDescription(null);
            Assert.fail("set attr description=null should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }
        
        
        newAttr.setCheckRule(null);
        newAttr = ScmFactory.Attribute.getInstance(ws, attrId);
        Assert.assertEquals(newAttr.getType(), AttributeType.INTEGER);
        rule = (ScmIntegerRule) newAttr.getCheckRule();
        Assert.assertEquals(rule.getMinimum(), Integer.MIN_VALUE);
        Assert.assertEquals(rule.getMaximum(), Integer.MAX_VALUE);
        
        try {
            newAttr.setCheckRule(new ScmStringRule(10));
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            ScmFactory.Attribute.deleteInstance(ws, attrId);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            ScmTestTools.releaseSession(ss);
        }
    }

}
