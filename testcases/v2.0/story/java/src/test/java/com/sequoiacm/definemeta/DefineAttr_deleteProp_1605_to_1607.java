package com.sequoiacm.definemeta;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmClassProperties;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.element.metadata.ScmDoubleRule;
import com.sequoiacm.client.element.metadata.ScmIntegerRule;
import com.sequoiacm.client.element.metadata.ScmStringRule;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author huangxiaoni init
 * @Testcase: SCM-1605:删除已存在的属性 SCM-1606:删除不存在的属性 SCM-1607:获取不存在的自定义属性
 * @date 2017.6.18
 */

public class DefineAttr_deleteProp_1605_to_1607 extends TestScmBase {
    private static final Logger logger = Logger.getLogger(DefineAttr_deleteProp_1605_to_1607.class);
    private boolean runSuccess = false;

    private static final String NAME = "definemeta1605";
    private String CLASS_ID = null;

    private ScmClass class1 = null;
    private List<ScmAttribute> attrList = new ArrayList<ScmAttribute>();

    private Map<String, Object> attrMap = new HashMap<>();

    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession(site);
        ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);

        BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(NAME).get();
        ScmFileUtils.cleanFile(wsp, cond);
        class1 = ScmFactory.Class.createInstance(ws, NAME, NAME + "_desc");
        CLASS_ID = class1.getId().get();
        this.readyScmFile();
    }

    @Test
    private void test() throws Exception {
        test_deleteProperties();
        test_deleteProperty();
        test_deleteNotExistProperty();
        test_getNotExistProperty();
        runSuccess = true;
    }

    // properties: 1. not required; 2. cover all data type
    private void test_deleteProperties() throws ScmException {
        ScmFile file = ScmFactory.File.getInstance(ws, fileId);
        ScmClassProperties properties = file.getClassProperties();
        properties.deleteProperty("test_attr_name_int_1605");
        properties.deleteProperty("test_attr_name_string_1605");
        properties.deleteProperty("test_attr_name_date_1605");
        properties.deleteProperty("test_attr_name_bool_1605");
        properties.deleteProperty("test_attr_name_double_1605");
        file.setClassProperties(properties);

        // check results
        file = ScmFactory.File.getInstance(ws, fileId);
        properties = file.getClassProperties();

        Map<String, Object> expMap = new HashMap<>();
        Assert.assertEquals(properties.toMap(), expMap);
    }

    // property is required
    private void test_deleteProperty() throws ScmException {
        // prepare file properties
        ScmFile file = ScmFactory.File.getInstance(ws, fileId);

        // create attr
        createModel("test_attr_name_bool_1605_1", "boolean", true);
        createModel("test_attr_name_bool_1605_2", "boolean", true);

        ScmClassProperties properties = new ScmClassProperties(CLASS_ID);

        Map<String, Object> newAttrMap = new HashMap<>();
        newAttrMap.put("test_attr_name_bool_1605_1", true);
        newAttrMap.put("test_attr_name_bool_1605_2", true);
        properties.addProperties(newAttrMap);
        file.setClassProperties(properties);

        file = ScmFactory.File.getInstance(ws, fileId);
        properties = file.getClassProperties();
        Assert.assertEquals(properties.toMap(), newAttrMap);

        // delete required property
        file = ScmFactory.File.getInstance(ws, fileId);
        properties = file.getClassProperties();
        properties.deleteProperty("test_attr_name_bool_1605_1");
        try {
            file.setClassProperties(properties);
            Assert.fail("expect failed but actual succ.");
        } catch (ScmException e) {
            logger.info("attr is required, errorMsg = [" + e.getError() + "]");
        }

        // check results
        file = ScmFactory.File.getInstance(ws, fileId);
        properties = file.getClassProperties();
        Assert.assertEquals(properties.toMap(), newAttrMap);
    }

    private void test_deleteNotExistProperty() throws ScmException {
        ScmFile file = ScmFactory.File.getInstance(ws, fileId);
        ScmClassProperties properties = file.getClassProperties();
        properties.deleteProperty("test");
        file.setClassProperties(properties);

        // check results
        file = ScmFactory.File.getInstance(ws, fileId);
        properties = file.getClassProperties();

        Map<String, Object> newAttrMap = new HashMap<>();
        newAttrMap.put("test_attr_name_bool_1605_1", true);
        newAttrMap.put("test_attr_name_bool_1605_2", true);
        Assert.assertEquals(properties.toMap(), newAttrMap);
    }

    private void test_getNotExistProperty() throws Exception {
        ScmFile file = ScmFactory.File.getInstance(ws, fileId);
        ScmClassProperties properties = file.getClassProperties();
        Object proVal = properties.getProperty("test");
        Assert.assertNull(proVal);
        Assert.assertFalse(properties.contains("test"));
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if (runSuccess || TestScmBase.forceClear) {
                ScmFactory.File.deleteInstance(ws, fileId, true);
                ScmFactory.Class.deleteInstance(ws, class1.getId());
                for (ScmAttribute attr : attrList) {
                    ScmFactory.Attribute.deleteInstance(ws, attr.getId());
                }
            }
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private void readyScmFile() throws ScmException {
        ScmFile file = ScmFactory.File.createInstance(ws);
        file.setFileName(NAME);
        createModel("test_attr_name_int_1605", "int", false);
        createModel("test_attr_name_string_1605", "string", false);
        createModel("test_attr_name_date_1605", "date", false);
        createModel("test_attr_name_bool_1605", "boolean", false);
        createModel("test_attr_name_double_1605", "double", false);

        attrMap.put("test_attr_name_int_1605", 1);
        attrMap.put("test_attr_name_string_1605", "2");
        attrMap.put("test_attr_name_date_1605", "2018-01-01-01:01:01.000");
        attrMap.put("test_attr_name_bool_1605", true);
        attrMap.put("test_attr_name_double_1605", 5.0);

        ScmClassProperties properties = new ScmClassProperties(CLASS_ID);
        properties.addProperties(attrMap);
        file.setClassProperties(properties);

        fileId = file.save();
    }

    private void createModel(String name, String type, boolean required) throws ScmException {
        // createattr
        ScmAttributeConf conf = new ScmAttributeConf();
        conf.setName(name);
        conf.setDescription(name);
        conf.setDisplayName(name + "_display");
        conf.setRequired(required);
        switch (type) {
            case "int":
                conf.setType(AttributeType.INTEGER);
                ScmIntegerRule rule = new ScmIntegerRule();
                rule.setMinimum(0);
                rule.setMaximum(1605);
                conf.setCheckRule(rule);
                break;
            case "string":
                conf.setType(AttributeType.STRING);
                ScmStringRule rule1 = new ScmStringRule(10);
                conf.setCheckRule(rule1);
                break;
            case "date":
                conf.setType(AttributeType.DATE);
                break;
            case "boolean":
                conf.setType(AttributeType.BOOLEAN);
                break;
            case "double":
                conf.setType(AttributeType.DOUBLE);
                ScmDoubleRule rule2 = new ScmDoubleRule();
                rule2.setMinimum(0L);
                rule2.setMaximum(1165L);
                conf.setCheckRule(rule2);
                break;
        }
        ScmAttribute attr = ScmFactory.Attribute.createInstance(ws, conf);
        // attr attch class
        class1.attachAttr(attr.getId());
        attrList.add(attr);
    }
}