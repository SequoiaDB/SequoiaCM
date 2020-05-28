package com.sequoiacm.breakpointfile;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBreakpointFile;
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
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description:SCM-1944 :: 通过断点文件创建文件，添加/更新/删除自定义属性
 * @author fanyu
 * @Date:2018年7月11日
 * @version:1.0
 */
public class BraekFileThenCUDAttr1944 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    private String name = "BraekFileThenCUDAttr1944";
    private int fileSize = 1024;
    private ScmId fileId = null;

    private ScmClass class1 = null;
    private ScmAttribute attr = null;
    private ScmClassProperties properties = null;

    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        List< SiteWrapper > siteList = ScmInfo.getAllSites();
        for ( int i = 0; i < siteList.size(); i++ ) {
            if ( siteList.get( i ).getDataType()
                    .equals( DatasourceType.SEQUOIADB ) ) {
                site = siteList.get( i );
                break;
            }
            if ( i == siteList.size() - 1 ) {
                throw new SkipException( "NO Sequoiadb Datasourse, Skip!" );
            }
        }
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( name ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        attr = craeteAttr( name );
        class1 = createClass( name );
        classAttachAttr( class1, attr );
        properties = createProperties( class1, 1 );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        ScmBreakpointFile breakpointFile = createBreakpointFile( name,
                filePath );
        breakpointFile2ScmFile( breakpointFile, name, properties );
        check( fileId, properties );

        // update
        properties.addProperty( name, 10 );
        updateFile( fileId, properties );
        check( fileId, properties );

        // delete
        properties.deleteProperty( name );
        updateFile( fileId, properties );
        checkDel( fileId );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( !runSuccess || TestScmBase.forceClear ) {
                if ( fileId != null && class1 != null ) {
                    System.out.println( "fileId = " + fileId.get() );
                    System.out.println( "classId = " + class1.getId() );
                }
            }
            ScmFactory.File.deleteInstance( ws, fileId, true );
            ScmFactory.Class.deleteInstance( ws, class1.getId() );
            ScmFactory.Attribute.deleteInstance( ws, attr.getId() );
            TestTools.LocalFile.removeFile( localPath );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private ScmBreakpointFile createBreakpointFile( String name,
            String filePath ) throws ScmException {
        // create file
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, name );
        breakpointFile.upload( new File( filePath ) );
        return breakpointFile;
    }

    private void breakpointFile2ScmFile( ScmBreakpointFile breakpointFile,
            String name, ScmClassProperties properties ) throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( breakpointFile );
        file.setFileName( name );
        file.setAuthor( name );
        file.setClassProperties( properties );
        fileId = file.save();
    }

    private void updateFile( ScmId fileId, ScmClassProperties properties )
            throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        file.setClassProperties( properties );
    }

    private ScmClassProperties createProperties( ScmClass class1, int value ) {
        ScmClassProperties properties = new ScmClassProperties(
                class1.getId().get() );
        for ( int i = 0; i < class1.listAttrs().size(); i++ ) {
            properties.addProperty( class1.listAttrs().get( i ).getName(),
                    value );
        }
        return properties;
    }

    private ScmAttribute craeteAttr( String name ) throws ScmException {
        ScmAttributeConf conf = new ScmAttributeConf();
        conf.setName( name );
        conf.setDescription( name + "_desc" );
        conf.setDisplayName( name + "_display" );
        conf.setRequired( false );
        conf.setType( AttributeType.INTEGER );
        ScmAttribute attr = ScmFactory.Attribute.createInstance( ws, conf );
        return attr;
    }

    private ScmClass createClass( String name ) throws ScmException {
        ScmClass class1 = ScmFactory.Class.createInstance( ws, name,
                name + "_desc" );
        return class1;
    }

    private void classAttachAttr( ScmClass class1, ScmAttribute attr )
            throws ScmException {
        class1.attachAttr( attr.getId() );
    }

    private void check( ScmId fileId, ScmClassProperties properties )
            throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        Assert.assertEquals( file.getFileName(), name );
        Assert.assertEquals( file.getAuthor(), name );
        Assert.assertEquals( file.getClassProperties().keySet().size(), 1 );
        Assert.assertEquals( file.getClassProperties().getProperty( name ),
                properties.getProperty( name ) );
    }

    private void checkDel( ScmId fileId ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = file.getClassProperties();
        Assert.assertFalse( properties.contains( name ) );
    }
}
