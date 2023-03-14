package com.sequoiacm.definemeta;

import java.io.IOException;

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
 * @Testcase: SCM-1580:属性类型为string，属性表未配置校验规则，必填
 * @author huangxiaoni init
 * @date 2017.6.18
 */

public class DefineAttr_type_stringReq_notSet_1580 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( DefineAttr_type_stringReq_notSet_1580.class );
    private static final String NAME = "definemeta1580";
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private String CLASS_ID = null;
    private ScmClass class1 = null;
    private ScmAttribute attr = null;
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
        createModel( NAME );
        CLASS_ID = class1.getId().get();

        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( NAME );
        fileId = file.save();
    }

    @Test
    private void test_setProperty() throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( CLASS_ID );
        try {
            file.setClassProperties( properties );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "attr is required, but not set the attr, errorMsg = ["
                    + e.getError() + "]" );
        }
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

    private void createModel( String name ) throws ScmException {
        // createclass
        class1 = ScmFactory.Class.createInstance( ws, name, name + "_desc" );
        // createattr
        ScmAttributeConf conf = new ScmAttributeConf();
        conf.setName( "test_attr_name_str_1580" );
        conf.setDescription( "test_attr_name_str_1580" );
        conf.setDisplayName( name + "_display" );
        conf.setRequired( true );
        conf.setType( AttributeType.STRING );
        ScmStringRule rule = new ScmStringRule( 10 );
        conf.setCheckRule( rule );
        attr = ScmFactory.Attribute.createInstance( ws, conf );
        // attr attch class
        class1.attachAttr( attr.getId() );
    }
}