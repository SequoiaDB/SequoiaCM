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
import com.sequoiacm.client.core.ScmClass;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.element.metadata.ScmStringRule;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestDeleteClass extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestDeleteClass.class);
    private ScmSession ss;
    private ScmWorkspace ws;
    private List<ScmId> classIds = new ArrayList<>();
    private List<ScmId> attrIds = new ArrayList<>();

    private String attrName1 = "test_delete_class_attr001";
    private String attrName2 = "test_delete_class_attr002";

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
    }

    @Test
    public void testDeleteInstance1() throws ScmException {
        ScmClass scmClass = ScmFactory.Class.createInstance(ws, ScmTestTools.getClassName(),
                "delete class");
        classIds.add(scmClass.getId());

        ScmAttributeConf conf = new ScmAttributeConf().setName(attrName1)
                .setType(AttributeType.STRING).setCheckRule(new ScmStringRule(10));
        ScmAttribute attr = ScmFactory.Attribute.createInstance(ws, conf);
        ScmId attrId1 = attr.getId();
        attrIds.add(attrId1);
        scmClass.attachAttr(attrId1);

        conf.setName(attrName2).setType(AttributeType.INTEGER).setCheckRule(null)
        .setDisplayName("属性字段2").setDescription("属性字段2").setRequired(true);
        attr = ScmFactory.Attribute.createInstance(ws, conf);
        ScmId attrId2 = attr.getId();
        attrIds.add(attrId2);
        scmClass.attachAttr(attrId2);

        logger.info("create class:" + scmClass.toString());

        scmClass.delete();

        Assert.assertEquals(scmClass.isExist(), false);

        // delete unexist class, do nothing
        scmClass.delete();

        // get unexist class
        try {
            ScmFactory.Class.getInstance(ws, scmClass.getId());
            Assert.fail("get not exist class should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CLASS_NOT_EXIST, e.getMessage());
        }

        // no class attached attr1 and attr2, can be delete.
        ScmFactory.Attribute.deleteInstance(ws, attrId1);
        ScmFactory.Attribute.deleteInstance(ws, attrId2);
        attrIds.clear();
    }

    @Test(dependsOnMethods = { "testDeleteInstance1" })
    public void testDeleteInstance2() throws ScmException {
        ScmClass scmClass = ScmFactory.Class.createInstance(ws, ScmTestTools.getClassName(),
                "delete class");
        classIds.add(scmClass.getId());

        ScmAttributeConf conf = new ScmAttributeConf().setName(attrName1)
                .setType(AttributeType.STRING).setCheckRule(new ScmStringRule(10));
        ScmAttribute attr = ScmFactory.Attribute.createInstance(ws, conf);
        ScmId attrId1 = attr.getId();
        attrIds.add(attrId1);
        scmClass.attachAttr(attrId1);

        conf.setName(attrName2).setType(AttributeType.INTEGER).setCheckRule(null)
        .setDisplayName("属性字段2").setDescription("属性字段2").setRequired(true);
        attr = ScmFactory.Attribute.createInstance(ws, conf);
        ScmId attrId2 = attr.getId();
        attrIds.add(attrId2);
        scmClass.attachAttr(attrId2);

        logger.info("create class:" + scmClass.toString());

        ScmFactory.Class.deleteInstance(ws, scmClass.getId());

        Assert.assertEquals(scmClass.isExist(), true);

        // delete unexist class, do nothing
        try {
            ScmFactory.Class.deleteInstance(ws, scmClass.getId());
        }
        catch (ScmException e) {
            if (e.getError() != ScmError.METADATA_CLASS_NOT_EXIST) {
                throw e;
            }
        }
        try {
            scmClass.delete();
        }
        catch (ScmException e) {
            if (e.getError() != ScmError.METADATA_CLASS_NOT_EXIST) {
                throw e;
            }
        }

        // get unexist class
        try {
            ScmFactory.Class.getInstance(ws, scmClass.getId());
            Assert.fail("get not exist class should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CLASS_NOT_EXIST, e.getMessage());
        }

        // no class attached attr1 and attr2, can be delete.
        ScmFactory.Attribute.deleteInstance(ws, attrId1);
        ScmFactory.Attribute.deleteInstance(ws, attrId2);
        attrIds.clear();
    }

    @Test(dependsOnMethods = { "testDeleteInstance2" })
    public void testDeleteInstance3() throws ScmException {
        String className = ScmTestTools.getClassName();
        ScmClass scmClass = ScmFactory.Class.createInstance(ws, className, "delete class");
        classIds.add(scmClass.getId());

        ScmAttributeConf conf = new ScmAttributeConf().setName(attrName1)
                .setType(AttributeType.STRING).setCheckRule(new ScmStringRule(10));
        ScmAttribute attr = ScmFactory.Attribute.createInstance(ws, conf);
        ScmId attrId1 = attr.getId();
        attrIds.add(attrId1);
        scmClass.attachAttr(attrId1);

        conf.setName(attrName2).setType(AttributeType.INTEGER).setCheckRule(null)
                .setDisplayName("属性字段2").setDescription("属性字段2").setRequired(true);
        attr = ScmFactory.Attribute.createInstance(ws, conf);
        ScmId attrId2 = attr.getId();
        attrIds.add(attrId2);
        scmClass.attachAttr(attrId2);

        logger.info("create class:" + scmClass.toString());

        ScmFactory.Class.deleteInstanceByName(ws, className);

        Assert.assertEquals(scmClass.isExist(), true);

        // delete unexist class, do nothing
        try {
            ScmFactory.Class.deleteInstanceByName(ws, className);
        }
        catch (ScmException e) {
            if (e.getError() != ScmError.METADATA_CLASS_NOT_EXIST) {
                throw e;
            }
        }

        // get unexist class
        try {
            ScmFactory.Class.getInstanceByName(ws, className);
            Assert.fail("get not exist class should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CLASS_NOT_EXIST, e.getMessage());
        }

        // no class attached attr1 and attr2, can be delete.
        ScmFactory.Attribute.deleteInstance(ws, attrId1);
        ScmFactory.Attribute.deleteInstance(ws, attrId2);
        attrIds.clear();
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            for (ScmId scmId : attrIds) {
                ScmFactory.Attribute.deleteInstance(ws, scmId);
            }
            for (ScmId scmId : classIds) {
                ScmFactory.Class.deleteInstance(ws, scmId);
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
