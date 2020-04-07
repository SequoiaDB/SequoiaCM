package com.sequoiacm.definemeta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmClass;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmClassProperties;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.element.metadata.ScmDoubleRule;
import com.sequoiacm.client.element.metadata.ScmIntegerRule;
import com.sequoiacm.client.element.metadata.ScmStringRule;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-1578:属性类型为int，属性表未配置校验规则，非必填
 *            SCM-1579:属性类型为int，属性表配置校验规则为int32有效范围，必填
 * @author huangxiaoni init
 * @date 2017.6.18
 */

public class DefineAttr_multiProp_1594_to_1599 extends TestScmBase {
    private static final String NAME = "definemeta1594";
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private int failTimes = 0;
    // private static final String CLASS_ID = "test_class_id_all_001";
    private Map< String, Object > attrMap = new HashMap<>();

    // private Map<String, Object> attrMap = new HashMap<>();
    private String CLASS_ID = null;
    private ScmClass class1 = null;
    private List< ScmAttribute > attrList = new ArrayList< ScmAttribute >();
    private ScmWorkspace ws = null;
    private ScmId fileId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( NAME ).get();
        ScmFileUtils.cleanFile( wsp, cond );

        class1 = ScmFactory.Class.createInstance( ws, NAME, NAME + "_desc" );
        CLASS_ID = class1.getId().get();

        // create class properties
        createModel( "test_attr_name_int_1594", "int", false );
        createModel( "test_attr_name_string_1594", "string", false );
        createModel( "test_attr_name_date_1594", "date", false );
        createModel( "test_attr_name_bool_1594", "boolean", false );
        createModel( "test_attr_name_double_1594", "double", false );

        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( NAME );
        fileId = file.save();
    }

    @BeforeMethod
    private void initMethod() {
        attrMap.clear();

        if ( !runSuccess ) {
            failTimes++;
        }
        runSuccess = false;
    }

    @AfterMethod
    private void afterMethod() {
        if ( failTimes > 1 ) {
            runSuccess = false;
        }
    }

    @Test
    private void test_setMultiProNotRepeat() throws Exception {
        // set class properties
        attrMap.put( "test_attr_name_int_1594", 100 );
        attrMap.put( "test_attr_name_string_1594", "test123" );
        attrMap.put( "test_attr_name_date_1594", "2018-01-01-01:01:00.000" );
        attrMap.put( "test_attr_name_bool_1594", true );
        attrMap.put( "test_attr_name_double_1594", 1.0 );

        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperties( attrMap );
        file.setClassProperties( properties );

        // check results
        file = ScmFactory.File.getInstance( ws, fileId );
        properties = file.getClassProperties();
        Assert.assertEquals( properties.toMap(), attrMap );

        runSuccess = true;
    }

    /**
     * SCM-1595/SCM-1596
     */
    @Test
    private void test_setMultiPropRepeat() throws Exception {
        // set class properties
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperty( "test_attr_name_int_1594", 1 );
        properties.addProperty( "test_attr_name_int_1594", 10 );
        properties.addProperty( "test_attr_name_string_1594", "hello" );
        file.setClassProperties( properties );

        // check results
        file = ScmFactory.File.getInstance( ws, fileId );
        properties = file.getClassProperties();
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_int_1594" ), 10 );
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_string_1594" ),
                "hello" );
        Assert.assertNull(
                properties.getProperty( "test_attr_name_date_1594" ) );
        Assert.assertNull(
                properties.getProperty( "test_attr_name_bool_1594" ) );
        Assert.assertNull(
                properties.getProperty( "test_attr_name_double_1594" ) );

        runSuccess = true;
    }

    @Test
    private void test_setSinglePropNotRepeat() throws Exception {
        // set class properties
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperty( "test_attr_name_int_1594", 0 );
        file.setClassProperties( properties );

        // check results
        file = ScmFactory.File.getInstance( ws, fileId );
        properties = file.getClassProperties();
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_int_1594" ), 0 );
        Assert.assertNull(
                properties.getProperty( "test_attr_name_string_1594" ) );
        Assert.assertNull(
                properties.getProperty( "test_attr_name_date_1594" ) );
        Assert.assertNull(
                properties.getProperty( "test_attr_name_bool_1594" ) );
        Assert.assertNull(
                properties.getProperty( "test_attr_name_double_1594" ) );

        runSuccess = true;
    }

    @Test
    private void test_setSinglePropRepeat() throws Exception {
        // set class properties
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperty( "test_attr_name_int_1594", -1 );
        properties.addProperty( "test_attr_name_int_1594", -2 );
        file.setClassProperties( properties );

        // check results
        file = ScmFactory.File.getInstance( ws, fileId );
        properties = file.getClassProperties();
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_int_1594" ), -2 );

        runSuccess = true;
    }

    @Test
    private void test_setClassIdNotContainPro() throws Exception {
        // set class properties
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        file.setClassProperties( properties );

        // check results
        file = ScmFactory.File.getInstance( ws, fileId );
        properties = file.getClassProperties();
        Assert.assertEquals( properties.toMap().toString(), "{}" );

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                ScmFactory.Class.deleteInstance( ws, class1.getId() );
                for ( ScmAttribute attr : attrList ) {
                    ScmFactory.Attribute.deleteInstance( ws, attr.getId() );
                }
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void createModel( String name, String type, boolean required )
            throws ScmException {
        // createattr
        ScmAttributeConf conf = new ScmAttributeConf();
        conf.setName( name );
        conf.setDescription( name );
        conf.setDisplayName( name + "_display" );
        conf.setRequired( required );
        switch ( type ) {
        case "int":
            conf.setType( AttributeType.INTEGER );
            ScmIntegerRule rule = new ScmIntegerRule();
            rule.setMinimum( -100 );
            rule.setMaximum( 100 );
            conf.setCheckRule( rule );
            break;
        case "string":
            conf.setType( AttributeType.STRING );
            ScmStringRule rule1 = new ScmStringRule( 10 );
            conf.setCheckRule( rule1 );
            break;
        case "date":
            conf.setType( AttributeType.DATE );
            break;
        case "boolean":
            conf.setType( AttributeType.BOOLEAN );
            break;
        case "double":
            conf.setType( AttributeType.DOUBLE );
            ScmDoubleRule rule2 = new ScmDoubleRule();
            rule2.setMinimum( 0L );
            rule2.setMaximum( 100L );
            conf.setCheckRule( rule2 );
            break;
        }
        ScmAttribute attr = ScmFactory.Attribute.createInstance( ws, conf );
        // attr attch class
        class1.attachAttr( attr.getId() );
        attrList.add( attr );
    }
}