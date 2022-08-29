package com.sequoiacm.definemeta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
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
 * @Testcase: SCM-1601:更新已存在自定义属性值，覆盖所有类型
 * @author huangxiaoni init
 * @date 2017.6.18
 */

public class DefineAttr_updateProp_propExisted1601 extends TestScmBase {
    private static final String NAME = "definemeta1601";
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
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
        createModel( "test_attr_name_int_1601", "int", false );
        createModel( "test_attr_name_string_1601", "string", false );
        createModel( "test_attr_name_date_1601", "date", false );
        createModel( "test_attr_name_bool_1601", "boolean", false );
        createModel( "test_attr_name_double_1601", "double", false );

        this.readyScmFile();
    }

    @Test(groups = { GroupTags.base })
    private void test_updatePro() throws Exception {
        // set class properties
        int intVal = 2;
        String strVal = "3";
        String dateVal = "0004-01-01-01:01:00.000";
        boolean boolVal = false;
        double doubleVal = 6.0;
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        file.setClassProperty( "test_attr_name_int_1601", intVal );
        file.setClassProperty( "test_attr_name_string_1601", strVal );
        file.setClassProperty( "test_attr_name_date_1601", dateVal );
        file.setClassProperty( "test_attr_name_bool_1601", boolVal );
        file.setClassProperty( "test_attr_name_double_1601", doubleVal );

        // check results
        file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = file.getClassProperties();
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_int_1601" ), intVal );
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_string_1601" ),
                strVal );
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_date_1601" ), dateVal );
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_bool_1601" ), boolVal );
        Assert.assertEquals(
                properties.getProperty( "test_attr_name_double_1601" ),
                doubleVal );

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

        Map< String, Object > attrMap = new HashMap<>();
        attrMap.put( "test_attr_name_int_1601", 1 );
        attrMap.put( "test_attr_name_string_1601", "2" );
        attrMap.put( "test_attr_name_date_1601", "0003-01-01-01:01:00.000" );
        attrMap.put( "test_attr_name_bool_1601", true );
        attrMap.put( "test_attr_name_double_1601", 5.0 );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperties( attrMap );
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