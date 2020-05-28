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
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-1587:属性类型为bool，属性表未配置校验规则，required覆盖必填和非必填
 *            SCM-1835:属性类型为bool，属性表配置校验规则，required覆盖必填和非必填
 * @author huangxiaoni init
 * @date 2017.6.18
 */

public class DefineAttr_type_boolValid_1587_1835 extends TestScmBase {
    private static final String NAME = "definemeta_boolValid_1587";
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    // private static final Logger logger = Logger.getLogger
    // (DefineAttr_dateValid_1584_1585.class);
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

    @Test // jira-310
    private void test_setPropIsTrue() throws Exception {
        // set class properties
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperty( "test_attr_name_bool_001", true );
        properties.addProperty( "test_attr_name_bool_002", "true" );
        properties.addProperty( "test_attr_name_bool_003", "TRUE" );
        file.setClassProperties( properties );

        // check results
        file = ScmFactory.File.getInstance( ws, fileId );
        properties = file.getClassProperties();
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_bool_001" ), true );
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_bool_002" ), true );
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_bool_003" ), true );

        runSuccess = true;
    }

    @Test // jira-310
    private void test_setPropIsFalse() throws Exception {
        // set class properties
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperty( "test_attr_name_bool_001", false );
        properties.addProperty( "test_attr_name_bool_002", "false" );
        properties.addProperty( "test_attr_name_bool_003", "FALSE" );
        file.setClassProperties( properties );

        // check results
        file = ScmFactory.File.getInstance( ws, fileId );
        properties = file.getClassProperties();
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_bool_001" ), false );
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_bool_002" ), false );
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_bool_003" ), false );

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
            conf.setName( "test_attr_name_bool_00" + i );
            conf.setDescription( "test_attr_name_bool_00" + i );
            conf.setDisplayName( name + "_display" );
            if ( i % 2 == 0 ) {
                conf.setRequired( false );
            } else {
                conf.setRequired( true );
            }
            conf.setType( AttributeType.BOOLEAN );
            ScmAttribute attr = ScmFactory.Attribute.createInstance( ws, conf );
            // attr attch class
            class1.attachAttr( attr.getId() );
            attrList.add( attr );
        }
    }
}