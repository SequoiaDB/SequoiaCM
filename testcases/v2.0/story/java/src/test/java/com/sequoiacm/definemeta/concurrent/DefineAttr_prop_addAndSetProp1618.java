package com.sequoiacm.definemeta.concurrent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
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
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @FileName SCM-1618:多线程并发添加/更新不同自定义属性
 * @Author huangxioni
 * @Date 2018/6/26
 */

public class DefineAttr_prop_addAndSetProp1618 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( DefineAttr_prop_addAndSetProp1618.class );
    private static final String NAME = "definemeta1618";
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
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        class1 = ScmFactory.Class.createInstance( ws, NAME, NAME + "_desc" );
        CLASS_ID = class1.getId().get();

        // create class properties
        createModel( "test_attr_name_int_1618", "int", false );
        createModel( "test_attr_name_string_1618", "string", false );
        createModel( "test_attr_name_date_1618", "date", false );
        createModel( "test_attr_name_bool_1618", "boolean", false );
        createModel( "test_attr_name_double_1618", "double", false );

        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( NAME ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        this.prepareScmFile();
    }

    @Test
    private void test() throws ScmException {
        Random random = new Random();
        AddProperties addProp = new AddProperties();
        UpdateProperty updProp = new UpdateProperty();
        addProp.start( random.nextInt( 10 ) + 20 );
        updProp.start( random.nextInt( 10 ) + 20 );
        if ( !( addProp.isSuccess() && updProp.isSuccess() ) ) {
            Assert.fail( addProp.getErrorMsg() + updProp.getErrorMsg() );
        }

        // check results
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = file.getClassProperties();
        Map< String, Object > actMap = properties.toMap();

        Map< String, Object > expMap1 = new HashMap<>();
        expMap1.put( "test_attr_name_date_1618", "0003-01-01-01:01:00.000" );
        expMap1.put( "test_attr_name_bool_1618", true );

        Map< String, Object > expMap2 = new HashMap<>();
        expMap2.put( "test_attr_name_date_1618", "0003-01-01-01:01:00.000" );
        expMap2.put( "test_attr_name_bool_1618", true );
        expMap2.put( "test_attr_name_double_1618", 5.0 );

        if ( !actMap.equals( expMap1 ) && !actMap.equals( expMap2 ) ) {
            logger.info( "actMap = " + actMap );
            Assert.fail( "check results failed.actMap = " + actMap.toString() );
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
            if ( null != session ) {
                session.close();
            }
        }
    }

    private void prepareScmFile() throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( NAME );

        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperty( "test_attr_name_int_1618", 1 );
        properties.addProperty( "test_attr_name_string_1618", "2" );
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

    private class AddProperties extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace tmpws = ScmFactory.Workspace
                        .getWorkspace( ws.getName(), session );
                ScmFile file = ScmFactory.File.getInstance( tmpws, fileId );

                Map< String, Object > propMap = new HashMap<>();
                propMap.put( "test_attr_name_date_1618",
                        "0003-01-01-01:01:00.000" );
                propMap.put( "test_attr_name_bool_1618", true );

                ScmClassProperties properties = new ScmClassProperties(
                        CLASS_ID );
                properties.addProperties( propMap );
                file.setClassProperties( properties );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class UpdateProperty extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace tmpws = ScmFactory.Workspace
                        .getWorkspace( ws.getName(), session );
                ScmFile file = ScmFactory.File.getInstance( tmpws, fileId );
                file.setClassProperty( "test_attr_name_double_1618", 5.0 );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
