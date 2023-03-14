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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-1838:更新模型和新模型自定义属性
 * @author huangxiaoni init
 * @date 2017.6.18
 */

public class DefineAttr_updateClassAndProperties1838 extends TestScmBase {
    private static final String NAME = "definemeta1838";
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    // private static final String CLASS_ID = "test_class_id_all_001";
    // private static final String CLASS_ID_NEW = "test_class_id_bool_005";
    private String CLASS_ID = null;
    private String CLASS_ID_NEW = null;
    private ScmClass class1 = null;
    private ScmClass class2 = null;
    private List< ScmAttribute > attrList = new ArrayList< ScmAttribute >();
    private Map< String, Object > attrMap = new HashMap<>();
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

        this.readyScmFile();
    }

    @Test(groups = { GroupTags.base })
    private void test_updatePro() throws Exception {
        attrMap.clear();

        class2 = ScmFactory.Class.createInstance( ws, NAME + "_NEW",
                NAME + "_desc" );
        CLASS_ID_NEW = class2.getId().get();

        createModel( class2, "test_attr_name_bool_1838_1", "boolean", false );
        createModel( class2, "test_attr_name_bool_1838_2", "boolean", false );
        createModel( class2, "test_attr_name_bool_1838_3", "boolean", false );

        // set class properties
        attrMap.put( "test_attr_name_bool_1838_1", true );
        attrMap.put( "test_attr_name_bool_1838_2", true );
        attrMap.put( "test_attr_name_bool_1838_3", true );

        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID_NEW );
        properties.addProperties( attrMap );
        file.setClassProperties( properties );

        // check results
        file = ScmFactory.File.getInstance( ws, fileId );
        Assert.assertEquals( file.getClassId().get(), CLASS_ID_NEW );

        properties = file.getClassProperties();
        Assert.assertEquals( properties.toMap(), attrMap );

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                ScmFactory.Class.deleteInstance( ws, class1.getId() );
                ScmFactory.Class.deleteInstance( ws, class2.getId() );

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
        class1 = ScmFactory.Class.createInstance( ws, NAME, NAME + "_desc" );
        CLASS_ID = class1.getId().get();

        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( NAME );

        createModel( class1, "test_attr_name_int_1838", "int", false );
        createModel( class1, "test_attr_name_string_1838", "string", false );
        createModel( class1, "test_attr_name_date_1838", "date", false );
        createModel( class1, "test_attr_name_bool_1838", "boolean", false );
        createModel( class1, "test_attr_name_double_1838", "double", false );

        attrMap.put( "test_attr_name_int_1838", 1 );
        attrMap.put( "test_attr_name_string_1838", "2" );
        attrMap.put( "test_attr_name_date_1838", "0003-01-01-01:01:00.000" );
        attrMap.put( "test_attr_name_bool_1838", true );
        attrMap.put( "test_attr_name_double_1838", 5.0 );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperties( attrMap );
        file.setClassProperties( properties );

        fileId = file.save();
    }

    private void createModel( ScmClass class1, String name, String type,
            boolean required ) throws ScmException {
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
            rule.setMinimum( 0 );
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