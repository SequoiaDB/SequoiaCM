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

public class TestDeleteAttr extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestDeleteAttr.class);
    private ScmSession ss;
    private ScmWorkspace ws;

    private ScmId classId;
    private List<ScmId> attrIds = new ArrayList<>();

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
    }

    @Test
    public void testDeleteInstance1() throws ScmException {
        ScmAttributeConf conf = new ScmAttributeConf().setName(ScmTestTools.getClassName())
                .setType(AttributeType.STRING).setCheckRule(new ScmStringRule(10));
        ScmAttribute attr = ScmFactory.Attribute.createInstance(ws, conf);
        attrIds.add(attr.getId());

        logger.info("create attr:" + attr.toString());

        attr.delete();
        Assert.assertEquals(attr.isExist(), false);

        // delete unexist attr, do nothing
        attr.delete();

        // get unexist attr
        try {
            ScmFactory.Attribute.getInstance(ws, attr.getId());
            Assert.fail("get not exist attr should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_ATTR_NOT_EXIST, e.getMessage());
        }
    }

    @Test(dependsOnMethods = { "testDeleteInstance1" })
    public void testDeleteInstance2() throws ScmException {
        ScmAttributeConf conf = new ScmAttributeConf().setName(ScmTestTools.getClassName())
                .setType(AttributeType.STRING).setCheckRule(new ScmStringRule(10));
        ScmAttribute attr = ScmFactory.Attribute.createInstance(ws, conf);
        attrIds.add(attr.getId());

        logger.info("create attr:" + attr.toString());

        ScmFactory.Attribute.deleteInstance(ws, attr.getId());
        Assert.assertEquals(attr.isExist(), true);

        // delete unexist class, do nothing
        try {
            ScmFactory.Attribute.deleteInstance(ws, attr.getId());
        }
        catch (ScmException e) {
            if (e.getError() != ScmError.METADATA_ATTR_NOT_EXIST) {
                throw e;
            }
        }

        // get unexist class
        try {
            ScmFactory.Attribute.getInstance(ws, attr.getId());
            Assert.fail("get not exist attr should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_ATTR_NOT_EXIST, e.getMessage());
        }
    }

    @Test
    public void testDeleteAttrAreAttached() throws ScmException {
        ScmClass scmClass = ScmFactory.Class.createInstance(ws, "testDeleteAttrAreAttached", "");
        classId = scmClass.getId();
        ScmAttributeConf conf = new ScmAttributeConf().setName("testDeleteAttrAreAttached")
                .setType(AttributeType.STRING).setCheckRule(new ScmStringRule(10));
        ScmAttribute attr = ScmFactory.Attribute.createInstance(ws, conf);
        attrIds.add(attr.getId());
        scmClass.attachAttr(attr.getId());

        try {
            attr.delete();
            Assert.fail("attr are attached by class, delete should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_ATTR_DELETE_FAILED, e.getMessage());
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {

        try {
            ScmFactory.Class.deleteInstance(ws, classId);
        }
        catch (ScmException e) {
            if (e.getError() != ScmError.METADATA_CLASS_NOT_EXIST) {
                ss.close();
                throw e;
            }
        }
        for (ScmId attrId : attrIds) {
            try {
                ScmFactory.Attribute.deleteInstance(ws, attrId);
            }
            catch (ScmException e) {
                if (e.getError() != ScmError.METADATA_ATTR_NOT_EXIST) {
                    ss.close();
                    throw e;
                }
            }
        }
        ss.close();

    }
}
