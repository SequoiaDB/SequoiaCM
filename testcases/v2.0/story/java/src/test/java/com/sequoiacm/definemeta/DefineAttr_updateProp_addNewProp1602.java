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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-1602:更新添加新的属性
 * @author huangxiaoni init
 * @date 2017.6.18
 */

public class DefineAttr_updateProp_addNewProp1602 extends TestScmBase {
    private static final String NAME = "definemeta1602";
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
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( NAME ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        class1 = ScmFactory.Class.createInstance( ws, NAME, NAME + "_desc" );
        CLASS_ID = class1.getId().get();
        createModel( "test_attr_name_int_1602", "int", false );
        createModel( "test_attr_name_string_1602", "string", false );
        this.readyScmFile();
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
    private void test_updatePro01() throws Exception {
        // set class properties
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        file.setClassProperty( "test_attr_name_int_1602", 1 );

        // check results
        file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = file.getClassProperties();

        Map< String, Object > expMap = new HashMap<>();
        expMap.put( "test_attr_name_int_1602", 1 );

        Assert.assertEquals( properties.toMap(), expMap );

        runSuccess = true;
    }

    @Test()
    private void test_updatePro02() throws Exception {
        // set class properties
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        file.setClassProperty( "test_attr_name_string_1602", "test" );

        // check results
        file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = file.getClassProperties();

        Map< String, Object > expMap = new HashMap<>();
        expMap.put( "test_attr_name_int_1602", 1 );
        expMap.put( "test_attr_name_string_1602", "test" );

        Assert.assertEquals( properties.toMap(), expMap );

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

    private void readyScmFile() throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( NAME );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        file.setClassProperties( properties );
        fileId = file.save();
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