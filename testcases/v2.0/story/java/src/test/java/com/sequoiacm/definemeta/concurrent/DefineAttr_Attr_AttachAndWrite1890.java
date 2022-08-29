package com.sequoiacm.definemeta.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmClass;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.element.metadata.ScmIntegerRule;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiadb.exception.BaseException;

/**
 * @Description: SCM-1890 :: 模型关联属性和其他业务操作并发
 * @author fanyu
 * @Date:2018年7月6日
 * @version:1.0
 */
public class DefineAttr_Attr_AttachAndWrite1890 extends TestScmBase {
    private boolean runSuccess = false;
    private String name = "AttachAndWrite1890";
    private String desc = "AttachAndWrite1890";
    private ScmClass class1 = null;
    private List< ScmAttribute > attrList = new ArrayList< ScmAttribute >();
    private int num = 10;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
            class1 = ScmFactory.Class.createInstance( ws, name, desc );
            for ( int i = 0; i < num; i++ ) {
                attrList.add( craeteAttr( name + "_" + i ) );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { GroupTags.base })
    private void test() throws Exception {
        Attach aThread = new Attach();
        Write wThread = new Write();
        aThread.start( 10 );
        wThread.start( 10 );
        boolean dflag = aThread.isSuccess();
        boolean uflag = wThread.isSuccess();
        Assert.assertEquals( dflag, true, aThread.getErrorMsg() );
        Assert.assertEquals( uflag, true, wThread.getErrorMsg() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Class.deleteInstance( ws, class1.getId() );
                for ( int i = 0; i < num; i++ ) {
                    ScmFactory.Attribute.deleteInstance( ws,
                            attrList.get( i ).getId() );
                }
            }
            if ( !runSuccess && attrList.size() != 0 ) {
                System.out.println( "class = " + class1.toString() );
                ScmFactory.Class.deleteInstance( ws, class1.getId() );
                for ( int i = 0; i < num; i++ ) {
                    ScmFactory.Attribute.deleteInstance( ws,
                            attrList.get( i ).getId() );
                }
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private ScmAttribute craeteAttr( String name ) throws ScmException {
        ScmAttributeConf conf = new ScmAttributeConf();
        conf.setName( name );
        conf.setDescription( desc );
        conf.setDisplayName( name + "_display" );
        conf.setRequired( true );
        conf.setType( AttributeType.INTEGER );

        ScmIntegerRule rule = new ScmIntegerRule();
        rule.setMinimum( 0 );
        rule.setMaximum( 10 );
        conf.setCheckRule( rule );
        ScmAttribute attr = ScmFactory.Attribute.createInstance( ws, conf );
        return attr;
    }

    private class Attach extends TestThreadBase {
        @Override
        public void exec() {
            try {
                for ( int i = 0; i < num; i++ ) {
                    class1.attachAttr( attrList.get( i ).getId() );
                }
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.METADATA_ATTR_ALREADY_IN_CLASS ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
    }

    private class Write extends TestThreadBase {
        @Override
        public void exec() throws ScmException {
            ScmId fileId = null;
            try {
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setFileName( name + "_" + UUID.randomUUID() );
                fileId = file.save();
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            } finally {
                if ( fileId != null ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
            }
        }
    }
}
