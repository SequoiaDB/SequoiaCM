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

public class TestClassAttachAttr extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestClassAttachAttr.class);
    private ScmSession ss;
    private ScmWorkspace ws;
    private ScmClass scmClass1;
    private ScmClass scmClass2;
    
    private ScmId attrId;

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);

        ScmAttributeConf conf = new ScmAttributeConf().setName(ScmTestTools.getClassName())
                .setType(AttributeType.STRING).setCheckRule(new ScmStringRule(10));
        ScmAttribute attr = ScmFactory.Attribute.createInstance(ws, conf);
        attrId = attr.getId();
        logger.info("create attr:" + attr.toString());
    }

    @Test
    public void testAttach() throws ScmException {
        scmClass1 = ScmFactory.Class.createInstance(ws, ScmTestTools.getClassName(), "");
        scmClass1.attachAttr(attrId);
        
        ScmClass savedClass = ScmFactory.Class.getInstance(ws, scmClass1.getId());
        List<ScmAttribute> attrs = savedClass.listAttrs();
        Assert.assertEquals(attrs.size(), 1);
        Assert.assertEquals(attrs.get(0).getId().get(), attrId.get());
        Assert.assertNotEquals(savedClass.getUpdateTime().getTime(), 
                scmClass1.getUpdateTime().getTime());

        // class attach same attr 
        try {
            scmClass1.attachAttr(attrId);
            Assert.fail("attach same attr again should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(),
                    ScmError.METADATA_ATTR_ALREADY_IN_CLASS, e.getMessage());
        }

        // class attach unexist attr
        try {
            ScmId id = new ScmId("ffffffffffffffffffffffff");
            scmClass1.attachAttr(id);
            Assert.fail("attach unexist attr should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(),
                    ScmError.METADATA_ATTR_NOT_EXIST, e.getMessage());
        }
        
        // mutiple class can attach the same attr
        scmClass2 = ScmFactory.Class.createInstance(ws, ScmTestTools.getClassName() + "2", "");
        scmClass2.attachAttr(attrId);
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            if (scmClass1 != null) {
                scmClass1.delete();
            }
            if (scmClass2 != null) {
                scmClass2.delete();
            }
            ScmFactory.Attribute.deleteInstance(ws, attrId);
        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            ScmTestTools.releaseSession(ss);
        }
    }
}
