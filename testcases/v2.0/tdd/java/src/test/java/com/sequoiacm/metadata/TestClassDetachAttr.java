package com.sequoiacm.metadata;

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

public class TestClassDetachAttr extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestClassDetachAttr.class);
    private ScmSession ss;
    private ScmWorkspace ws;
    private ScmId classId;
    private ScmId attrId;

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);

        ScmClass scmClass = ScmFactory.Class.createInstance(ws, ScmTestTools.getClassName(), "");
        classId = scmClass.getId();
        logger.info("create class:" + scmClass.toString());
        
        ScmAttributeConf conf = new ScmAttributeConf().setName(ScmTestTools.getClassName())
                .setType(AttributeType.STRING).setCheckRule(new ScmStringRule(10));
        ScmAttribute attr = ScmFactory.Attribute.createInstance(ws, conf);
        attrId = attr.getId();
        logger.info("create attr:" + attr.toString());
        scmClass.attachAttr(attrId);
    }

    @Test
    public void testDetach() throws ScmException {
        ScmClass savedClass = ScmFactory.Class.getInstance(ws, classId);
        List<ScmAttribute> attrs = savedClass.listAttrs();
        Assert.assertEquals(attrs.size(), 1);
        Assert.assertEquals(attrs.get(0).getId().get(), attrId.get());

        savedClass.detachAttr(attrId);
        attrs = savedClass.listAttrs();
        Assert.assertEquals(attrs.size(), 0);
        
        // detach again 
        try {
            savedClass.detachAttr(attrId);
            Assert.fail("detach attr which not exist in class should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_ATTR_NOT_IN_CLASS, 
                    e.getMessage());
        }
        
        savedClass = ScmFactory.Class.getInstance(ws, classId);
        attrs = savedClass.listAttrs();
        Assert.assertEquals(attrs.size(), 0);
        
        // attr can be delete now
        ScmFactory.Attribute.deleteInstance(ws, attrId);
        attrId = null;
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            ScmFactory.Class.deleteInstance(ws, classId);
            if (attrId != null) {
                ScmFactory.Attribute.deleteInstance(ws, attrId);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            ScmTestTools.releaseSession(ss);
        }
    }
}
