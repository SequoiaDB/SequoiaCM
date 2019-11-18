package com.sequoiacm.metadata;

import java.util.ArrayList;
import java.util.List;

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
import com.sequoiacm.client.element.metadata.ScmDoubleRule;
import com.sequoiacm.client.element.metadata.ScmIntegerRule;
import com.sequoiacm.client.element.metadata.ScmStringRule;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestCreateAttr extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestCreateAttr.class);
    private ScmSession ss;
    private ScmWorkspace ws;
    List<ScmId> attrIds = new ArrayList<>();

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
    }

    @Test
    public void testCreate() throws ScmException {
        /*
         *  integer
         */
        String tmpName = AttributeType.INTEGER.getName();
        String desc = "元数据属性描述：" + tmpName;
        String displayName = "displayName";
        ScmAttributeConf attrConf = new ScmAttributeConf().setName(tmpName)
                .setDisplayName(displayName).setDescription(desc).setRequired(false)
                .setType(AttributeType.INTEGER).setCheckRule(new ScmIntegerRule(0, 10));
        ScmAttribute scmAttr = ScmFactory.Attribute.createInstance(ws, attrConf);
        attrIds.add(scmAttr.getId());
        logger.info(scmAttr.toString());
        
        ScmAttribute savedAttr = ScmFactory.Attribute.getInstance(ws, scmAttr.getId());
        Assert.assertEquals(savedAttr.isExist(), true);
        Assert.assertEquals(savedAttr.getName(), tmpName);
        Assert.assertEquals(savedAttr.getDescription(), desc);
        Assert.assertEquals(savedAttr.getCreateUser(), getScmUser());
        Assert.assertEquals(savedAttr.getUpdateUser(), getScmUser());
        Assert.assertEquals(savedAttr.getDisplayName(), displayName);
        Assert.assertEquals(savedAttr.getType(), AttributeType.INTEGER);
        ScmIntegerRule rule = (ScmIntegerRule) savedAttr.getCheckRule();
        Assert.assertEquals(rule.getMinimum(), 0);
        Assert.assertEquals(rule.getMaximum(), 10);
        Assert.assertEquals(savedAttr.isRequired(), false);
        
        
        /*
         *  double
         */
        tmpName = AttributeType.DOUBLE.getName();
        attrConf = new ScmAttributeConf().setName(tmpName)
                .setDisplayName(displayName).setDescription("").setRequired(true)
                .setType(AttributeType.DOUBLE).setCheckRule(new ScmDoubleRule(1.0, 1.2));
        scmAttr = ScmFactory.Attribute.createInstance(ws, attrConf);
        attrIds.add(scmAttr.getId());
        logger.info(scmAttr.toString());
        
        savedAttr = ScmFactory.Attribute.getInstance(ws, scmAttr.getId());
        Assert.assertEquals(savedAttr.getDescription(), "");
        Assert.assertEquals(savedAttr.getType(), AttributeType.DOUBLE);
        ScmDoubleRule doubleRule = (ScmDoubleRule) savedAttr.getCheckRule();
        Assert.assertEquals(doubleRule.getMinimun(), 1.0);
        Assert.assertEquals(doubleRule.getMaximun(), 1.2);
        Assert.assertEquals(savedAttr.isRequired(), true);
        
        /*
         *  string
         */
        tmpName = AttributeType.STRING.getName();
        attrConf = new ScmAttributeConf().setName(tmpName)
                .setType(AttributeType.STRING).setCheckRule(new ScmStringRule(10));
        scmAttr = ScmFactory.Attribute.createInstance(ws, attrConf);
        attrIds.add(scmAttr.getId());
        logger.info(scmAttr.toString());
        
        savedAttr = ScmFactory.Attribute.getInstance(ws, scmAttr.getId());
        Assert.assertEquals(savedAttr.getType(), AttributeType.STRING);
        ScmStringRule strRule = (ScmStringRule) savedAttr.getCheckRule();
        Assert.assertEquals(strRule.getMaxLength(), 10);
        
        /*
         *  date
         */
        tmpName = AttributeType.DATE.getName();
        attrConf = new ScmAttributeConf().setName(tmpName)
                .setType(AttributeType.DATE).setCheckRule(null);
        scmAttr = ScmFactory.Attribute.createInstance(ws, attrConf);
        attrIds.add(scmAttr.getId());
        logger.info(scmAttr.toString());
        
        savedAttr = ScmFactory.Attribute.getInstance(ws, scmAttr.getId());
        Assert.assertEquals(savedAttr.getType(), AttributeType.DATE);
        Assert.assertEquals(savedAttr.getCheckRule(), null);
        
        /*
         *  boolean
         */
        tmpName = AttributeType.BOOLEAN.getName();
        attrConf = new ScmAttributeConf().setName(tmpName)
                .setType(AttributeType.BOOLEAN).setCheckRule(null);
        scmAttr = ScmFactory.Attribute.createInstance(ws, attrConf);
        attrIds.add(scmAttr.getId());
        logger.info(scmAttr.toString());
        
        savedAttr = ScmFactory.Attribute.getInstance(ws, scmAttr.getId());
        Assert.assertEquals(savedAttr.getType(), AttributeType.BOOLEAN);
        Assert.assertEquals(savedAttr.getCheckRule(), null);
    }
    
    @Test
    public void testCreateAttrWithoutRequiredField() throws ScmException {
        /*
         * name and type is required
         */
        
        // without name
        ScmAttributeConf attrConf1 = new ScmAttributeConf().setType(AttributeType.STRING);
        try {
            ScmFactory.Attribute.createInstance(ws, attrConf1);
            Assert.fail("create class without name should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }
        
        // without type
        ScmAttributeConf attrConf2 = new ScmAttributeConf().setName("attr_name");
        try {
            ScmFactory.Attribute.createInstance(ws, attrConf2);
            Assert.fail("create class without type should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }
        
        // only set name and type
        attrConf1.setName("attr_name");
        ScmAttribute attribute = ScmFactory.Attribute.createInstance(ws, attrConf1);
        attrIds.add(attribute.getId());
    }
    
    @Test
    public void testSetIllegalArg() throws ScmException {
        // set illegal type
        ScmAttributeConf attrConf = new ScmAttributeConf().setName("testSetIllegalTypeOrCheckRule")
                .setType(AttributeType.getType("timestamp"));
        
        try {
            ScmFactory.Attribute.createInstance(ws, attrConf);
            Assert.fail("set illegal type should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }
        
        // set illegal check_rule
        // string
        attrConf.setType(AttributeType.STRING).setCheckRule(new ScmIntegerRule(1, 2));
        try {
            ScmFactory.Attribute.createInstance(ws, attrConf);
            Assert.fail("set illegal check_rule should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }
        
        // maxlength < 0
        attrConf.setType(AttributeType.STRING).setCheckRule(new ScmStringRule(-1));
        try {
            ScmFactory.Attribute.createInstance(ws, attrConf);
            Assert.fail("set maxlength<0 should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }
        
        // integer
        attrConf.setType(AttributeType.INTEGER).setCheckRule(new ScmStringRule(1));
        try {
            ScmFactory.Attribute.createInstance(ws, attrConf);
            Assert.fail("set illegal check_rule should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }
        
        // min > max
        attrConf.setType(AttributeType.INTEGER).setCheckRule(new ScmIntegerRule(2, 1));
        try {
            ScmFactory.Attribute.createInstance(ws, attrConf);
            Assert.fail("set int min > max should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }
        
        // double
        attrConf.setType(AttributeType.DOUBLE).setCheckRule(new ScmStringRule(1));
        try {
            ScmFactory.Attribute.createInstance(ws, attrConf);
            Assert.fail("set illegal check_rule should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }
        
        // min > max
        attrConf.setType(AttributeType.DOUBLE).setCheckRule(new ScmDoubleRule(2, 1));
        try {
            ScmFactory.Attribute.createInstance(ws, attrConf);
            Assert.fail("set double min > max should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }
        
        // date
        attrConf.setType(AttributeType.DATE).setCheckRule(new ScmStringRule(1));
        try {
            ScmFactory.Attribute.createInstance(ws, attrConf);
            Assert.fail("set illegal check_rule should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }
        
        // boolean
        attrConf.setType(AttributeType.BOOLEAN).setCheckRule(new ScmStringRule(1));
        try {
            ScmFactory.Attribute.createInstance(ws, attrConf);
            Assert.fail("set illegal check_rule should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }
        
        // set illegal name
        attrConf.setName("$abc").setType(AttributeType.STRING);
        try {
            ScmFactory.Attribute.createInstance(ws, attrConf);
            Assert.fail("set illegal attr name should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.ATTRIBUTE_FORMAT_ERROR, e.getMessage());
        }
        
        attrConf.setName("a.b");
        try {
            ScmFactory.Attribute.createInstance(ws, attrConf);
            Assert.fail("set illegal attr name should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.ATTRIBUTE_FORMAT_ERROR, e.getMessage());
        }
        
        attrConf.setName("");
        try {
            ScmFactory.Attribute.createInstance(ws, attrConf);
            Assert.fail("set illegal attr name should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.ATTRIBUTE_FORMAT_ERROR, e.getMessage());
        }
    }
    
    @Test
    public void testCreateWithDuplicateName() throws ScmException {
        String attrName = "unique_name";
        ScmAttributeConf attrConf = new ScmAttributeConf().setName(attrName)
                .setType(AttributeType.STRING);
        ScmAttribute attr = ScmFactory.Attribute.createInstance(ws, attrConf);
        attrIds.add(attr.getId());
        
        try {
            ScmFactory.Attribute.createInstance(ws, attrConf);
            Assert.fail("creare a attr with duplicate name should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_ATTR_EXIST, e.getMessage());
        }
    }
    
    @Test
    public void testSetNull() throws ScmException {
        ScmAttributeConf attrConf = new ScmAttributeConf();
        
        try {
            attrConf.setName(null);
            Assert.fail("attr name cannot be set null");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }
        
        try {
            attrConf.setDisplayName(null);
            Assert.fail("attr display name cannot be set null");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }
        
        try {
            attrConf.setDescription(null);
            Assert.fail("attr description cannot be set null");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }
        
        try {
            attrConf.setType(null);
            Assert.fail("attr type cannot be set null");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }
    }
    
    @AfterClass
    private void tearDown() throws Exception {
        try {
            for (ScmId scmId : attrIds) {
                ScmFactory.Attribute.deleteInstance(ws, scmId);
            }
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            ScmTestTools.releaseSession(ss);
        }
    }
}
