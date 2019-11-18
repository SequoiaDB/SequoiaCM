package com.sequoiacm.metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmClass;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmClassProperties;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.element.metadata.ScmStringRule;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestMetadataReload extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestMetadataReload.class);
    private ScmSession ss;
    private ScmWorkspace ws;
    private ScmClass scmClass;
    private ScmId classId;
    private ScmId attrId1;
    private ScmId attrId2;
    private ScmId batchId;
    private String attrName1 = "test_reload_metadata_attr001";
    private String attrName2 = "test_reload_metadata_attr002";

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);

        scmClass = ScmFactory.Class.createInstance(ws, ScmTestTools.getClassName(), "");
        classId = scmClass.getId();
        logger.info(scmClass.toString());
        
        ScmAttributeConf conf = new ScmAttributeConf().setName(attrName1)
                .setType(AttributeType.STRING).setCheckRule(new ScmStringRule(5));
        ScmAttribute attr = ScmFactory.Attribute.createInstance(ws, conf);
        attrId1 = attr.getId();
        logger.info(attr.toString());
        scmClass.attachAttr(attrId1);
        
        conf.setName(attrName2);
        attr = ScmFactory.Attribute.createInstance(ws, conf);
        attrId2 = attr.getId();
        logger.info(attr.toString());
    }

    @Test
    public void testReload() throws ScmException {
        ScmClassProperties properties = new ScmClassProperties(scmClass.getId().get());
        properties.addProperty(attrName1, "123");
        
        ScmBatch batch = ScmFactory.Batch.createInstance(ws);
        batch.setName(ScmTestTools.getClassName());
        batch.setClassProperties(properties);
        batchId = batch.save();
        
        batch = ScmFactory.Batch.getInstance(ws, batchId);
        Assert.assertEquals(batch.getClassId(), scmClass.getId());
        Assert.assertEquals(batch.getClassProperties().keySet().size(), 1);
        
        try {
            batch.setClassProperty(attrName2, "");
            Assert.fail("attr not in class, set should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(),
                    ScmError.METADATA_ATTR_NOT_IN_CLASS, e.getMessage());
        }
        
        // class attch attr2
        scmClass.attachAttr(attrId2);
        // update attr1
        ScmAttribute attr = ScmFactory.Attribute.getInstance(ws, attrId1);
        attr.setCheckRule(new ScmStringRule(10));
        
        batch.setClassProperty(attrName1, "123456");
        batch.setClassProperty(attrName2, "");
        batch = ScmFactory.Batch.getInstance(ws, batchId);
        Assert.assertEquals(batch.getClassProperties().keySet().size(), 2);
        Assert.assertEquals(batch.getClassProperties().getProperty(attrName2), "");
        
        // branchsite1
        ScmTestTools.releaseSession(ss);
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer2().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
        batch = ScmFactory.Batch.getInstance(ws, batchId);
        batch.setClassProperty(attrName1, "123456");
        batch.setClassProperty(attrName2, "bs1");
        batch = ScmFactory.Batch.getInstance(ws, batchId);
        Assert.assertEquals(batch.getClassProperties().keySet().size(), 2);
        Assert.assertEquals(batch.getClassProperties().getProperty(attrName2), "bs1");
        
        // branchsite2
        ScmTestTools.releaseSession(ss);
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer3().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
        batch = ScmFactory.Batch.getInstance(ws, batchId);
        batch.setClassProperty(attrName1, "123456");
        try {
            batch.setClassProperty(attrName2, "12345678910");
            Assert.fail("value against rule, set should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(),
                    ScmError.METADATA_CHECK_ERROR, e.getMessage());
        }
    }
    
    @AfterClass
    public void tearDown() throws ScmException {
        try {
            if (classId != null) {
                ScmFactory.Class.deleteInstance(ws, classId);
            }
            if (attrId1 != null) {
                ScmFactory.Attribute.deleteInstance(ws, attrId1);
            }
            if (attrId2 != null) {
                ScmFactory.Attribute.deleteInstance(ws, attrId2);
            }
            if (batchId != null) {
                ScmFactory.Batch.deleteInstance(ws, batchId);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            ScmTestTools.releaseSession(ss);
        }
    }
}
