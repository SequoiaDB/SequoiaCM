package com.sequoiacm.definemeta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
 * @Testcase: SCM-1581:属性类型为string，属性表未配置校验规则，非必填
 *            SCM-1582:属性类型为string，属性表配置string长度，必填
 * @author huangxiaoni init
 * @date 2017.6.18
 */

public class DefineAttr_type_stringInvalid_1581_1582 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( DefineAttr_type_stringInvalid_1581_1582.class );
    private static final String NAME = "definemeta_strInvalid_1581";
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private int failTimes = 0;
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
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( NAME );
        fileId = file.save();
        createModel( NAME );
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
    private void test_setPropInvalidIsNull01() throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties(
                class1.getId().get() );

        properties.addProperty( "test_attr_name_str_1581_1", null ); //invalid
        properties.addProperty( "test_attr_name_str_1581_2", "valid" );
        properties.addProperty( "test_attr_name_str_1581_3", "valid" );
        properties.addProperty( "test_attr_name_str_1581_4", "valid" );
        properties.addProperty( "test_attr_name_str_1581_5", "valid" );
        try {
            file.setClassProperties( properties );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            //e.printStackTrace();
            logger.info( "attr value is invalid, errorMsg = [" + e.getError() +
                    "]" );
        }
        runSuccess = true;
    }

    @Test
    private void test_setPropInvalidIsNull02() throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties(
                class1.getId().get() );
        properties.addProperty( "test_attr_name_str_1581_1", "valid" );
        properties.addProperty( "test_attr_name_str_1581_2", null );//invalid
        properties.addProperty( "test_attr_name_str_1581_3", "valid" );
        properties.addProperty( "test_attr_name_str_1581_4", "valid" );
        properties.addProperty( "test_attr_name_str_1581_5", "valid" );
        try {
            file.setClassProperties( properties );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            // e.printStackTrace();
            logger.info( "attr value is invalid, errorMsg = [" + e.getError() +
                    "]" );
        }
        runSuccess = true;
    }

    @Test
    private void test_setPropInvalidRightBound() throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties(
                class1.getId().get() );
        properties.addProperty( "test_attr_name_str_1581_1", "valid" );
        properties.addProperty( "test_attr_name_str_1581_2", "valid" );
        properties.addProperty( "test_attr_name_str_1581_3", "valid" );
        properties.addProperty( "test_attr_name_str_1581_4",
                "01234567890" ); // invalid
        properties.addProperty( "test_attr_name_str_1581_5", "valid" );
        try {
            file.setClassProperties( properties );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            // e.printStackTrace();
            logger.info( "attr value is invalid, errorMsg = [" + e.getError() +
                    "]" );
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
        // createclass
        class1 = ScmFactory.Class.createInstance( ws, name, name + "_desc" );
        for ( int i = 1; i < 6; i++ ) {
            // createattr
            ScmAttributeConf conf = new ScmAttributeConf();
            conf.setName( "test_attr_name_str_00" + i );
            conf.setDescription( "test_attr_name_str_1581_" + i );
            conf.setDisplayName( name + "_display" );
            conf.setType( AttributeType.STRING );
            if ( i % 2 == 0 ) {
                conf.setRequired( true );
                ScmStringRule rule = new ScmStringRule( 10 );
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
