package com.sequoiacm.definemeta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import com.sequoiacm.client.element.metadata.ScmStringRule;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-1581:属性类型为string，属性表未配置校验规则，非必填
 *            SCM-1582:属性类型为string，属性表配置string长度，必填
 * @author huangxiaoni init
 * @date 2017.6.18
 */

public class DefineAttr_type_stringValid_1581_1582 extends TestScmBase {
    private static final String NAME = "definemeta_strValid_1582";
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private int failTimes = 0;
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

        createModel( NAME );
        CLASS_ID = class1.getId().get();

        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( NAME );
        fileId = file.save();
    }

    @BeforeMethod
    private void initMethod() {
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
    private void test_setPropValidLeftBound() throws Exception {
        // set class properties
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperty( "test_attr_name_str_1582_1", "" );
        properties.addProperty( "test_attr_name_str_1582_2", "" );
        properties.addProperty( "test_attr_name_str_1582_3", "" );
        properties.addProperty( "test_attr_name_str_1582_4", "" );
        properties.addProperty( "test_attr_name_str_1582_5", "" );
        file.setClassProperties( properties );

        // check results
        file = ScmFactory.File.getInstance( ws, fileId );
        properties = file.getClassProperties();
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_str_1582_1" ), "" );
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_str_1582_2" ), "" );
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_str_1582_3" ), "" );
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_str_1582_4" ), "" );
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_str_1582_5" ), "" );

        runSuccess = true;
    }

    @Test
    private void test_setPropValidRightBound() throws Exception {
        // set class properties
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperty( "test_attr_name_str_1582_1", "1" );
        properties.addProperty( "test_attr_name_str_1582_2", "1" );
        properties.addProperty( "test_attr_name_str_1582_3", "1" );
        properties.addProperty( "test_attr_name_str_1582_4", "0123456789" );

        String val = TestTools.getRandomString( 1024 * 6 );
        properties.addProperty( "test_attr_name_str_1582_5", val );

        file.setClassProperties( properties );

        // check results
        file = ScmFactory.File.getInstance( ws, fileId );
        properties = file.getClassProperties();
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_str_1582_1" ), "1" );
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_str_1582_2" ), "1" );
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_str_1582_3" ), "1" );
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_str_1582_4" ),
                "0123456789" );
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_str_1582_5" ), val );

        runSuccess = true;
    }

    @Test
    private void test_setPropNotRequired() throws Exception {
        // set class properties
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );

        String val = TestTools.getRandomString( 1024 * 6 );
        properties.addProperty( "test_attr_name_str_1582_1", val );
        properties.addProperty( "test_attr_name_str_1582_2", "1" );
        properties.addProperty( "test_attr_name_str_1582_4", "0123456789" );
        properties.addProperty( "test_attr_name_str_1582_5", "test" );

        file.setClassProperties( properties );

        // check results
        file = ScmFactory.File.getInstance( ws, fileId );
        properties = file.getClassProperties();
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_str_1582_1" ), val );
        Assert.assertNull(
                properties.getProperty( "test_attr_name_str_1582_3" ) );
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_str_1582_2" ), "1" );
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_str_1582_4" ),
                "0123456789" );
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_str_1582_5" ), "test" );

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

    private void createModel( String name ) throws ScmException {
        // createclass
        class1 = ScmFactory.Class.createInstance( ws, name, name + "_desc" );
        for ( int i = 1; i < 6; i++ ) {
            // createattr
            ScmAttributeConf conf = new ScmAttributeConf();
            conf.setName( "test_attr_name_str_1582_" + i );
            conf.setDescription( "test_attr_name_str_1582_" + i );
            conf.setDisplayName( name + "_display" );
            conf.setType( AttributeType.STRING );
            if ( i % 2 == 0 ) {
                conf.setRequired( true );
                ScmStringRule rule = new ScmStringRule( 1024 * 6 );
                conf.setCheckRule( rule );
            } else {
                conf.setRequired( false );
            }
            ScmAttribute attr = ScmFactory.Attribute.createInstance( ws, conf );
            // attr attch class
            class1.attachAttr( attr.getId() );
            attrList.add( attr );
        }
    }
}