package com.sequoiacm.file;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmClass;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
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
 * 文件上传，设置文件自定义属性
 * @author zhangping
 *
 **/
public class TestUploadFileWithClassProperties extends ScmTestMultiCenterBase {
    
    private final static Logger logger = LoggerFactory.getLogger(TestUploadFileWithClassProperties.class);
    private ScmSession ss;
    private ScmWorkspace ws;
    
    private ScmId fileId;
    private ScmId classId;
    private List<ScmId> attrIds;
    
    @BeforeClass
    public void setUp() throws ScmException {
        
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
        
        ScmClass scmClass = ScmFactory.Class.createInstance(ws, ScmTestTools.getClassName(), "");
        classId = scmClass.getId();
        
        attrIds = MetadataTools.prepareAttrAndAttachToClass(ws, scmClass);
    }

    @Test
    public void testNormal() throws ScmException, IOException {
        ScmFile createdFile = ScmFactory.File.createInstance(ws);
        createdFile.setFileName(ScmTestTools.getClassName() + "-normal");
        ScmClassProperties properties = new ScmClassProperties(classId.get());
        properties.addProperty("ID_NUM", "613435199105687894");
        properties.addProperty("ID_NAME", "身份证");
        properties.addProperty("FILE_TYPE", "PIC");
        properties.addProperty("ID_ADD", "北京市朝阳区XX路XX号院");
        properties.addProperty("DATE_BEGIN", "2018-05-03-15:54:20.000");
        properties.addProperty("DATE_END", "2018-05-06-15:54:20.000");
        properties.addProperty("TIME_NUM", 150);
        properties.addProperty("HANDER_PRICE", 100000.56);
        properties.addProperty("IS_ENABLE", true);
        createdFile.setClassProperties(properties);
        
        logger.info("classId=" + classId + ",properties=" + properties.toString());
        fileId = createdFile.save();
        ScmFile savedFile = ScmFactory.File.getInstance(ws, fileId);
        
        Assert.assertEquals(savedFile.getClassId(), classId);
        ScmClassProperties savedProps = savedFile.getClassProperties();
        Assert.assertEquals(savedProps.getProperty("ID_NUM"), properties.getProperty("ID_NUM"));
        Assert.assertEquals(savedProps.getProperty("ID_NAME"), properties.getProperty("ID_NAME"));
        Assert.assertEquals(savedProps.getProperty("FILE_TYPE"), properties.getProperty("FILE_TYPE"));
        Assert.assertEquals(savedProps.getProperty("ID_ADD"), properties.getProperty("ID_ADD"));
        Assert.assertEquals(savedProps.getProperty("DATE_BEGIN"), properties.getProperty("DATE_BEGIN"));
        Assert.assertEquals(savedProps.getProperty("DATE_END"), properties.getProperty("DATE_END"));
        Assert.assertEquals(savedProps.getProperty("TIME_NUM"), properties.getProperty("TIME_NUM"));
        Assert.assertEquals(savedProps.getProperty("HANDER_PRICE"), properties.getProperty("HANDER_PRICE"));
        Assert.assertEquals(savedProps.getProperty("IS_ENABLE"), properties.getProperty("IS_ENABLE"));
    }
    
    @Test
    public void testNotExistClass() throws ScmException, IOException {
        String unexistClassId = "ffffffffffffffffffffffff";
        ScmFile createdFile = ScmFactory.File.createInstance(ws);
        createdFile.setFileName(ScmTestTools.getClassName() + "-unexistclass");
        ScmClassProperties properties = new ScmClassProperties(unexistClassId);
        createdFile.setClassProperties(properties);
        try {
            createdFile.save();
            Assert.fail("set not exist class should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CLASS_NOT_EXIST, e.getMessage());
        }
    }
    
    @Test
    public void testInvalidProp() throws ScmException, IOException {
        ScmFile createdFile = ScmFactory.File.createInstance(ws);
        createdFile.setFileName(ScmTestTools.getClassName() + "-invalid");
        ScmClassProperties properties = new ScmClassProperties(classId.get());
        
        /*
         *  1. has required fields does not set
         */
        properties.addProperty("ID_NUM", "613435199105687894");
        properties.addProperty("ID_NAME", "身份证");
        // miss required field "FILE_TYPE"
        // properties.addProperty("FILE_TYPE", "PIC");
        createdFile.setClassProperties(properties);
        try {
            createdFile.save();
            Assert.fail("missing required field should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CHECK_ERROR, e.getMessage());
        }
        
        /*
         *  2. set value against the rules (not fit check rule) 
         */
        // add required field
        properties.addProperty("FILE_TYPE", "PIC");
        // 2.1 set error value, Integer. min:100
        properties.addProperty("TIME_NUM", 80);
        try {
            createdFile.save();
            Assert.fail("integer property against the rules should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CHECK_ERROR, e.getMessage());
        }
        
        // Integer. max:1000
        properties.addProperty("TIME_NUM", 1001);
        try {
            createdFile.save();
            Assert.fail("integer property against the rules should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CHECK_ERROR, e.getMessage());
            properties.deleteProperty("TIME_NUM");
        }
        
        // 2.2 set error value, Double. min:10000
        properties.addProperty("HANDER_PRICE", 8000.88);
        try {
            createdFile.save();
            Assert.fail("double property against the rules should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CHECK_ERROR, e.getMessage());
            properties.deleteProperty("HANDER_PRICE");
        }
        
        // 2.3 set error value, Date. format:yyyy-MM-dd-h24:mm:ss.SSS
        properties.addProperty("DATE_BEGIN", "2018-06-07");
        try {
            createdFile.save();
            Assert.fail("date property against the rules should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CHECK_ERROR, e.getMessage());
            properties.deleteProperty("DATE_BEGIN");
        }
        
        properties.addProperty("DATE_BEGIN", "2018-06-07-11:11:11.0");
        try {
            createdFile.save();
            Assert.fail("date property against the rules should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CHECK_ERROR, e.getMessage());
            properties.deleteProperty("DATE_BEGIN");
        }
        
        // 2.4 set error value, String. maxLength:18
        properties.addProperty("ID_NUM", "12345678912345678910");
        try {
            createdFile.save();
            Assert.fail("string property against the rules should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CHECK_ERROR, e.getMessage());
            // restore field ID_NUM
            properties.addProperty("ID_NUM", "613435199105687894");
        }
        
        /*
         *  3. set value against the rules (not fit type) 
         */
        // 3.1 except 'Integer', set 'String'
        properties.addProperty("TIME_NUM", "150");
        try {
            createdFile.save();
            Assert.fail("field type incorrect, save() should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CHECK_ERROR, e.getMessage());
            properties.deleteProperty("TIME_NUM");
        }
        
        // 3.2 except 'Double', set 'String'
        properties.addProperty("HANDER_PRICE", "120000.1");
        try {
            createdFile.save();
            Assert.fail("field type incorrect, save() should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CHECK_ERROR, e.getMessage());
            properties.deleteProperty("HANDER_PRICE");
        }
        
        // 3.3 except 'Date', set 'Integer'
        properties.addProperty("DATE_BEGIN", 2018);
        try {
            createdFile.save();
            Assert.fail("field type incorrect, save() should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CHECK_ERROR, e.getMessage());
            properties.deleteProperty("DATE_BEGIN");
        }
        
        // 3.4 except 'Boolean', set 'Integer'
        properties.addProperty("IS_ENABLE", 1);
        try {
            createdFile.save();
            Assert.fail("field type incorrect, save() should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CHECK_ERROR, e.getMessage());
            properties.deleteProperty("IS_ENABLE");
        }
        
        /*
         *  4. set property that class not included 
         */
        properties.addProperty("UNKNOWN_KEY", 0);
        try {
            createdFile.save();
            Assert.fail("set property that class not included, save() should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_ATTR_NOT_IN_CLASS, e.getMessage());
            properties.deleteProperty("UNKNOWN_KEY");
        }
        
        properties.addProperty("", "");
        try {
            createdFile.save();
            Assert.fail("set property that class not included, save() should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_ATTR_NOT_IN_CLASS, e.getMessage());
            properties.deleteProperty("");
        }
        
        /*
         *  5. set property key = null 
         */
        properties.addProperty(null, "");
        try {
            createdFile.save();
            Assert.fail("property key cannot be null");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }
    }
    
    @AfterClass
    public void tearDown() throws ScmException {
        try {
            if (fileId != null) {
                ScmFactory.File.deleteInstance(ws, fileId, true);
            }
            if (classId != null) {
                ScmFactory.Class.deleteInstance(ws, classId);
            }
            for (ScmId attrId : attrIds) {
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