package com.sequoiacm.definemeta;

import java.io.IOException;
import java.util.HashMap;
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
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-1603:更新模型中不存在的属性
 *            SCM-1604:文件已有自定义文件属性，更新文件，addProperties指定json为空
 * @author huangxiaoni init
 * @date 2017.6.18
 */

public class DefineAttr_updateProp_1603_1604 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( DefineAttr_updateProp_1603_1604.class );
    private static final String NAME = "definemeta1603";
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private int failTimes = 0;
    private ScmClass class1 = null;
    private ScmAttribute attr = null;
    private String CLASS_ID = null;
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
    private void test_classNotThePro() throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        try {
            file.setClassProperty( "test", 123 );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "there is no the property in the class, errorMsg = ["
                    + e.getError() + "]" );
        }

        // check results
        file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = file.getClassProperties();

        Map< String, Object > expMap = new HashMap<>();
        expMap.put( "test_attr_name_int_1603", 1 );
        Assert.assertEquals( properties.toMap(), expMap );

        runSuccess = true;
    }

    @Test
    private void test_proIsEmptyJson() throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        file.setClassProperties( properties );

        // check results
        file = ScmFactory.File.getInstance( ws, fileId );
        properties = file.getClassProperties();

        Map< String, Object > expMap = new HashMap<>();
        Assert.assertEquals( properties.toMap(), expMap );

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                ScmFactory.Class.deleteInstance( ws, class1.getId() );
                ScmFactory.Attribute.deleteInstance( ws, attr.getId() );
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
        attrMap.put( "test_attr_name_int_1603", 1 );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        properties.addProperties( attrMap );
        file.setClassProperties( properties );
        fileId = file.save();
    }

    private void createModel( String name ) throws ScmException {
        // create class
        class1 = ScmFactory.Class.createInstance( ws, name, name + "_desc" );
        // create attr
        ScmAttributeConf conf = new ScmAttributeConf();
        conf.setName( "test_attr_name_int_1603" );
        conf.setDescription( "test_attr_name_int_1603" );
        conf.setDisplayName( name + "_display" );
        conf.setType( AttributeType.INTEGER );
        conf.setRequired( false );
        attr = ScmFactory.Attribute.createInstance( ws, conf );
        // attr attach class
        class1.attachAttr( attr.getId() );
        CLASS_ID = class1.getId().get();
    }
}