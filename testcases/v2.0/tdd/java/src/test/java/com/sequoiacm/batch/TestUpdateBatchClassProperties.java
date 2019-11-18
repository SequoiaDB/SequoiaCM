package com.sequoiacm.batch;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmClass;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmClassProperties;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.MetadataTools;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

/**
 * 更新批次自定义属性
 * @author yanglei
 *
 **/
public class TestUpdateBatchClassProperties extends ScmTestMultiCenterBase {
    
    private final static Logger logger = LoggerFactory.getLogger(TestUpdateBatchClassProperties.class);
    private ScmSession ss;
    private ScmWorkspace ws;
    
    private ScmBatch batch;
    private ScmBatch testSetNullValBatch;
    private ScmId classId1;
    private ScmId classId2;
    private ScmClassProperties properties = null;
    private List<ScmId> attrIds;
    
    @BeforeClass
    public void setUp() throws ScmException {
        
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
        
        ScmClass scmClass1 = ScmFactory.Class.createInstance(ws, ScmTestTools.getClassName()+1, "");
        classId1 = scmClass1.getId();
        ScmClass scmClass2 = ScmFactory.Class.createInstance(ws, ScmTestTools.getClassName()+2, "");
        classId2 = scmClass2.getId();
        
        attrIds = MetadataTools.prepareAttrAndAttachToClass(ws, scmClass1, scmClass2);
        
        batch = ScmFactory.Batch.createInstance(ws);
        batch.setName(ScmTestTools.getClassName());
        properties = new ScmClassProperties(classId1.get());
        properties.addProperty("ID_NUM", "613435199105687894");
        properties.addProperty("ID_NAME", "身份证");
        properties.addProperty("FILE_TYPE", "PIC");
        properties.addProperty("ID_ADD", "北京市朝阳区XX路XX号院");
        properties.addProperty("DATE_BEGIN", "2018-05-03-15:54:20.000");
        properties.addProperty("DATE_END", "2018-05-06-15:54:20.000");
        properties.addProperty("TIME_NUM", 150);
        properties.addProperty("HANDER_PRICE", 100000.56);
        properties.addProperty("IS_ENABLE", true);
        batch.setClassProperties(properties);
        
        logger.info("classId=" + classId1 + ",properties=" + properties.toString());
        batch.save();
    }

    @Test
    public void testUpdateClassProperties() throws ScmException, IOException {
        /*
         * 1. update one property correct
         */
        batch.setClassProperty("ID_NUM", "123456789");
        ScmBatch savedBatch = ScmFactory.Batch.getInstance(ws, batch.getId());
        ScmClassProperties savedProps = savedBatch.getClassProperties();
        Assert.assertEquals(savedBatch.getClassId(), classId1);
        Assert.assertEquals(savedProps.getProperty("ID_NUM"), "123456789");
        Assert.assertEquals(savedProps.getProperty("ID_NAME"), properties.getProperty("ID_NAME"));
        Assert.assertEquals(savedProps.getProperty("FILE_TYPE"), properties.getProperty("FILE_TYPE"));
        Assert.assertEquals(savedProps.getProperty("ID_ADD"), properties.getProperty("ID_ADD"));
        Assert.assertEquals(savedProps.getProperty("DATE_BEGIN"), properties.getProperty("DATE_BEGIN"));
        Assert.assertEquals(savedProps.getProperty("DATE_END"), properties.getProperty("DATE_END"));
        Assert.assertEquals(savedProps.getProperty("TIME_NUM"), properties.getProperty("TIME_NUM"));
        Assert.assertEquals(savedProps.getProperty("HANDER_PRICE"), properties.getProperty("HANDER_PRICE"));
        Assert.assertEquals(savedProps.getProperty("IS_ENABLE"), properties.getProperty("IS_ENABLE"));
        
        /*
         * 2. update one property incorrect
         */
        // > maxLength
        try {
            batch.setClassProperty("ID_NUM", "12345678912345678910");
            Assert.fail("update property against the rule should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CHECK_ERROR, e.getMessage());
        }
        
        /*
         * 3. update Boolean (string rep)
         */
        
        batch.setClassProperty("IS_ENABLE", "False");
        savedBatch = ScmFactory.Batch.getInstance(ws, batch.getId());
        savedProps = savedBatch.getClassProperties();
        Assert.assertEquals(savedProps.getProperty("IS_ENABLE"), false);
        
        /*
         * 4. overlay update (class & properties)
         */
        logger.debug("oldClassId=" + properties.getClassId());
        properties.setClassId(classId2.get());
        logger.debug("newClassId=" + properties.getClassId());
        
        try {
            // property key cannot be null
            properties.addProperty(null, "false");
            batch.setClassProperties(properties);
            Assert.fail("property's key cannot be null");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }
        properties.deleteProperty(null);
        batch.setClassProperties(properties);
        savedBatch = ScmFactory.Batch.getInstance(ws, batch.getId());
        savedProps = savedBatch.getClassProperties();
        Assert.assertEquals(savedBatch.getClassId(), classId2);
        Assert.assertEquals(savedProps.getProperty("ID_NUM"), properties.getProperty("ID_NUM"));
        Assert.assertEquals(savedProps.getProperty("ID_NAME"), properties.getProperty("ID_NAME"));
        Assert.assertEquals(savedProps.getProperty("FILE_TYPE"), properties.getProperty("FILE_TYPE"));
        Assert.assertEquals(savedProps.getProperty("ID_ADD"), properties.getProperty("ID_ADD"));
        Assert.assertEquals(savedProps.getProperty("DATE_BEGIN"), properties.getProperty("DATE_BEGIN"));
        Assert.assertEquals(savedProps.getProperty("DATE_END"), properties.getProperty("DATE_END"));
        Assert.assertEquals(savedProps.getProperty("TIME_NUM"), properties.getProperty("TIME_NUM"));
        Assert.assertEquals(savedProps.getProperty("HANDER_PRICE"), properties.getProperty("HANDER_PRICE"));
        Assert.assertEquals(savedProps.getProperty("IS_ENABLE"), properties.getProperty("IS_ENABLE"));
        
        /*
         * 5. update unexist class
         */
        String unexistClassId = "unknown";
        properties.setClassId(unexistClassId);
        try {
            batch.setClassProperties(properties);
            Assert.fail("set not exist class should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CLASS_NOT_EXIST, e.getMessage());
        }
    }
    
    @Test
    public void testSetNUll() throws ScmException {
        testSetNullValBatch = ScmFactory.Batch.createInstance(ws);
        testSetNullValBatch.setName(ScmTestTools.getClassName() + "-nullvalue");
        
        // set class properties = null
        try {
            testSetNullValBatch.setClassProperties(null);
            Assert.fail("batch class properties cannot be set null");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }
        
        ScmClassProperties properties = new ScmClassProperties(classId1.get());
        properties.addProperty("ID_NUM", null);
        properties.addProperty("ID_NAME", null);
        properties.addProperty("FILE_TYPE", null);
        testSetNullValBatch.setClassProperties(properties);
        
        try {
            testSetNullValBatch.save();
            Assert.fail("field against the rules should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CHECK_ERROR, e.getMessage());
        }
        
        try {
            properties.addProperty("ID_NUM", "613435199105687894");
            testSetNullValBatch.save();
            Assert.fail("field against the rules should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CHECK_ERROR, e.getMessage());
        }
        
        try {
            properties.addProperty("ID_NAME", "身份证");
            testSetNullValBatch.save();
            Assert.fail("field against the rules should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CHECK_ERROR, e.getMessage());
        }
        
        properties.addProperty("FILE_TYPE", "PIC");
        // can be save
        testSetNullValBatch.save();
        
        try {
            testSetNullValBatch.setClassProperty("TIME_NUM", null);
            Assert.fail("field against the rules should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CHECK_ERROR, e.getMessage());
            properties.deleteProperty("TIME_NUM");
        }
        
        try {
            testSetNullValBatch.setClassProperty("HANDER_PRICE", null);
            Assert.fail("field against the rules should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CHECK_ERROR, e.getMessage());
            properties.deleteProperty("HANDER_PRICE");
        }
        
        try {
            testSetNullValBatch.setClassProperty("DATE_BEGIN", null);
            Assert.fail("field against the rules should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CHECK_ERROR, e.getMessage());
            properties.deleteProperty("DATE_BEGIN");
        }
        
        try {
            testSetNullValBatch.setClassProperty("ID_ADD", null);
            Assert.fail("field against the rules should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CHECK_ERROR, e.getMessage());
            properties.deleteProperty("ID_ADD");
        }
        
        try {
            testSetNullValBatch.setClassProperty("IS_ENABLE", null);
            Assert.fail("field against the rules should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CHECK_ERROR, e.getMessage());
            properties.deleteProperty("IS_ENABLE");
        }
        
        try {
            testSetNullValBatch.setClassProperty(null, 1);
            Assert.fail("property's key cannot be null");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }
    }
    
    @AfterClass
    public void tearDown() throws ScmException {
        try {
            ScmFactory.Class.deleteInstance(ws, classId1);
            ScmFactory.Class.deleteInstance(ws, classId2);
            for (ScmId attrId : attrIds) {
                ScmFactory.Attribute.deleteInstance(ws, attrId);
            }
            if (null != batch) {
                batch.delete();
            }
            if (null != testSetNullValBatch) {
                testSetNullValBatch.delete();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            ScmTestTools.releaseSession(ss);
        }
    }



}