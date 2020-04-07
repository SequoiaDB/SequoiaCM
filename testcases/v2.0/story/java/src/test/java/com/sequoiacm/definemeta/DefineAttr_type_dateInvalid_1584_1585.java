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
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-1584:属性类型为date，属性表未配置校验规则，非必填
 *			   SCM-1585:属性类型为date，属性表配置date时间格式，必填
 * @author huangxiaoni init
 * @date 2017.6.18
 */

public class DefineAttr_type_dateInvalid_1584_1585 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( DefineAttr_type_dateInvalid_1584_1585.class );
    private static final String NAME = "definemeta_dateInvalid_1584";
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private int failTimes = 0;
    //private static final String CLASS_ID = "test_class_id_date_004";
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
    private void test_setPropInvalid01() throws Exception {
        String date = "0001-01-01 00:00:00.999999";
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperty( "test_attr_name_date_1584_1", date );
        properties.addProperty( "test_attr_name_date_1584_3", date );
        try {
            file.setClassProperties( properties );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "attr value is invalid, errorMsg = [" + e.getError() +
                    "]" );
        }
        runSuccess = true;
    }

    @Test
    private void test_setPropInvalid02() throws Exception {
        String date = "000t-12-31-23:59:59.999";
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperty( "test_attr_name_date_1584_1", date );
        properties.addProperty( "test_attr_name_date_1584_3", date );
        try {
            file.setClassProperties( properties );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "attr value is invalid, errorMsg = [" + e.getError() +
                    "]" );
        }
        runSuccess = true;
    }

    @Test
    private void test_setPropInvalid03() throws Exception {
        String date = "2018-12-31";
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperty( "test_attr_name_date_1584_1", date );
        properties.addProperty( "test_attr_name_date_1584_3", date );
        try {
            file.setClassProperties( properties );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "attr value is invalid, errorMsg = [" + e.getError() +
                    "]" );
        }
        runSuccess = true;
    }

    @Test
    private void test_setPropInvalid04() throws Exception {
        String date = "";
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperty( "test_attr_name_date_1584_1", date );
        properties.addProperty( "test_attr_name_date_1584_3", date );
        try {
            file.setClassProperties( properties );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "attr value is invalid, errorMsg = [" + e.getError() +
                    "]" );
        }
        runSuccess = true;
    }

    @Test
    private void test_setPropInvalid05() throws Exception {
        String date = null;
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperty( "test_attr_name_date_1584_1", date );
        properties.addProperty( "test_attr_name_date_1584_3",
                "2018-01-01-00:00:000" );
        try {
            file.setClassProperties( properties );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
//			e.printStackTrace();
            logger.info( "attr value is invalid, errorMsg = [" + e.getError() +
                    "]" );
        }
        runSuccess = true;
    }

    @Test
    private void test_setPropInvalid06() throws Exception {
        String date = "2018-00-01-00:00:000";
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperty( "test_attr_name_date_1584_1", date );
        properties.addProperty( "test_attr_name_date_1584_3",
                "2018-01-01-00:00:000" );
        try {
            file.setClassProperties( properties );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
//			e.printStackTrace();
            logger.info( "attr value is invalid, errorMsg = [" + e.getError() +
                    "]" );
        }
        runSuccess = true;
    }

    @Test
    private void test_setPropInvalid07() throws Exception {
        String date = "2018-13-01-00:00:000";
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperty( "test_attr_name_date_1584_1", date );
        properties.addProperty( "test_attr_name_date_1584_3",
                "2018-01-01-00:00:000" );
        try {
            file.setClassProperties( properties );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
//			e.printStackTrace();
            logger.info( "attr value is invalid, errorMsg = [" + e.getError() +
                    "]" );
        }
        runSuccess = true;
    }

    @Test
    private void test_setPropInvalid08() throws Exception {
        String date = "2018-02-29-00:00:000";
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperty( "test_attr_name_date_1584_1", date );
        properties.addProperty( "test_attr_name_date_1584_3",
                "2018-01-01-00:00:000" );
        try {
            file.setClassProperties( properties );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
//			e.printStackTrace();
            logger.info( "attr value is invalid, errorMsg = [" + e.getError() +
                    "]" );
        }
        runSuccess = true;
    }

    @Test
    private void test_setPropInvalid09() throws Exception {
        String date = "2018-02-29-00:00:000";
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperty( "test_attr_name_date_1584_1", date );
        properties.addProperty( "test_attr_name_date_1584_3",
                "2018-01-01-00:00:000" );
        try {
            file.setClassProperties( properties );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
//			e.printStackTrace();
            logger.info( "attr value is invalid, errorMsg = [" + e.getError() +
                    "]" );
        }
        runSuccess = true;
    }

    @Test
    private void test_setPropInvalidA() throws Exception {
        String date = "2018-12-31--1:00:000";
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperty( "test_attr_name_date_1584_1", date );
        properties.addProperty( "test_attr_name_date_1584_3",
                "2018-01-01-00:00:000" );
        try {
            file.setClassProperties( properties );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
//			e.printStackTrace();
            logger.info( "attr value is invalid, errorMsg = [" + e.getError() +
                    "]" );
        }
        runSuccess = true;
    }

    @Test
    private void test_setPropInvalidB() throws Exception {
        String date = "2018-12-31-24:00:000";
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperty( "test_attr_name_date_1584_1", date );
        properties.addProperty( "test_attr_name_date_1584_3",
                "2018-01-01-00:00:000" );
        try {
            file.setClassProperties( properties );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
//			e.printStackTrace();
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
        for ( int i = 1; i < 4; i++ ) {
            // createattr
            ScmAttributeConf conf = new ScmAttributeConf();
            conf.setName( "test_attr_name_date_1584_" + i );
            conf.setDescription( "test_attr_name_date_1584_" + i );
            conf.setDisplayName( name + "_display" );
            if ( i % 2 == 0 ) {
                conf.setRequired( false );
            } else {
                conf.setRequired( true );
            }
            conf.setType( AttributeType.DATE );
            ScmAttribute attr = ScmFactory.Attribute.createInstance( ws, conf );
            // attr attch class
            class1.attachAttr( attr.getId() );
            attrList.add( attr );
        }
    }
}