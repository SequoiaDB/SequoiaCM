package com.sequoiacm.definemeta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
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
import com.sequoiacm.client.element.metadata.ScmIntegerRule;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-1578:属性类型为int，属性表未配置校验规则，非必填
 *            SCM-1579:属性类型为int，属性表配置校验规则为int32有效范围，必填
 * @author huangxiaoni init
 * @date 2017.6.18
 */

public class DefineAttr_type_intInvalid_1578_1579 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( DefineAttr_type_intInvalid_1578_1579.class );
    private static final String NAME = "definemeta1579_intInvalid";
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private int failTimes = 0;
    private String CLASS_ID = null;
    private Map< String, Object > attrMap = new HashMap<>();
    private ScmClass class1 = null;
    private List< ScmAttribute > attrList = new ArrayList< ScmAttribute >();
    private ScmWorkspace ws = null;
    private ScmId fileId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( NAME ).get();
        ScmFileUtils.cleanFile( wsp, cond );

        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( NAME );
        fileId = file.save();
        createModel( NAME );
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
    private void test_setPropInvalidLeftBound01() throws Exception {
        attrMap.put( "test_attr_name_int_1578_1", -2147483649L ); // invalid
        attrMap.put( "test_attr_name_int_1578_3", 1 );
        attrMap.put( "test_attr_name_int_1578_4", 1 );

        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperties( attrMap );
        try {
            file.setClassProperties( properties );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            // e.printStackTrace();
            logger.info( "attr value is invalid, errorMsg = [" + e.getError()
                    + "]" );
        }

        runSuccess = true;
    }

    @Test
    private void test_setPropInvalidLeftBound02() throws Exception {
        attrMap.put( "test_attr_name_int_1578_1", 1 );
        attrMap.put( "test_attr_name_int_1578_3", -2147483649L );// invalid
        attrMap.put( "test_attr_name_int_1578_4", 1 );

        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperties( attrMap );
        try {
            file.setClassProperties( properties );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "attr value is invalid, errorMsg = [" + e.getError()
                    + "]" );
        }

        runSuccess = true;
    }

    @Test
    private void test_setPropInvalidLeftBound03() throws Exception {
        attrMap.put( "test_attr_name_int_1578_1", 1 );
        attrMap.put( "test_attr_name_int_1578_3", 1 );
        attrMap.put( "test_attr_name_int_1578_4", -1 );// invalid

        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperties( attrMap );
        try {
            file.setClassProperties( properties );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "attr value is invalid, errorMsg = [" + e.getError()
                    + "]" );
        }

        runSuccess = true;
    }

    @Test
    private void test_setPropInvalidRightBound01() throws Exception {
        attrMap.put( "test_attr_name_int_1578_1", 2147483648L );// invalid
        attrMap.put( "test_attr_name_int_1578_3", 1 );
        attrMap.put( "test_attr_name_int_1578_4", 1 );

        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperties( attrMap );
        try {
            file.setClassProperties( properties );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            // e.printStackTrace();
            logger.info( "attr value is invalid, errorMsg = [" + e.getError()
                    + "]" );
        }

        runSuccess = true;
    }

    @Test
    private void test_setPropInvalidRightBound02() throws Exception {
        attrMap.put( "test_attr_name_int_1578_1", 1 );
        attrMap.put( "test_attr_name_int_1578_3", 2147483648L );// invalid
        attrMap.put( "test_attr_name_int_1578_4", 1 );

        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperties( attrMap );
        try {
            file.setClassProperties( properties );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "attr value is invalid, errorMsg = [" + e.getError()
                    + "]" );
        }

        runSuccess = true;
    }

    @Test
    private void test_setPropInvalidRightBound03() throws Exception {
        attrMap.put( "test_attr_name_int_1578_1", 1 );
        attrMap.put( "test_attr_name_int_1578_3", 1 );
        attrMap.put( "test_attr_name_int_1578_4", 101 );// invalid

        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperties( attrMap );
        try {
            file.setClassProperties( properties );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "attr value is invalid, errorMsg = [" + e.getError()
                    + "]" );
        }

        runSuccess = true;
    }

    @Test
    private void test_setPropInValidIsNull() throws Exception {
        attrMap.put( "test_attr_name_int_1578_1", 1 );
        attrMap.put( "test_attr_name_int_1578_3", 1 );
        attrMap.put( "test_attr_name_int_1578_4", null );// invalid

        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperties( attrMap );
        try {
            file.setClassProperties( properties );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            e.printStackTrace();
            logger.info( "attr value is invalid, errorMsg = [" + e.getError()
                    + "]" );
        }

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
        // create class
        class1 = ScmFactory.Class.createInstance( ws, name, name + "_desc" );
        for ( int i = 1; i < 4; i++ ) {
            // create attr
            ScmAttributeConf conf = new ScmAttributeConf();
            conf.setName( "test_attr_name_int_1578_" + i );
            conf.setDescription( "test_attr_name_int_1578_" + i );
            conf.setDisplayName( name + "_display" );
            conf.setType( AttributeType.INTEGER );

            if ( i % 2 == 0 ) {
                conf.setRequired( false );
                ScmIntegerRule rule = new ScmIntegerRule();
                rule.setMinimum( 0 );
                rule.setMaximum( 100 );
                conf.setCheckRule( rule );
            } else {
                conf.setRequired( true );
            }

            ScmAttribute attr = ScmFactory.Attribute.createInstance( ws, conf );
            // attr attach class
            class1.attachAttr( attr.getId() );
            attrList.add( attr );
        }
        CLASS_ID = class1.getId().get();
    }
}