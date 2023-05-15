package com.sequoiacm.scmfile.oper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmClass;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmClassProperties;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description: SCM-2072 :: 指定目录、自定义属性和标签检索文件
 * @author fanyu
 * @Date:2018年7月11日
 * @version:1.0
 */
public class ListFileWithDirTagAttr2072 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    private String name = "ListFileWithDirTagAttr2072";
    private int fileSize = 1;
    private int fileNum = 5;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();

    private ScmTags tags = null;
    private ScmClass class1 = null;
    private ScmAttribute attr = null;
    private ScmClassProperties properties = null;

    private String dirPath1 = "/2072_A/2072_B/2072_C/2072_D/2072_E/";
    private String dirPath2 = "/2072_E/2072_F/2072_G/2072_H/2072_I/";

    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( name ).get();
        ScmFileUtils.cleanFile( wsp, cond );

        deleteDir( ws, dirPath1 );
        deleteDir( ws, dirPath2 );

        createDir( ws, dirPath1 );
        createDir( ws, dirPath2 );

        ScmTags scmTags = new ScmTags();
        scmTags.addTag( name );
        attr = createAttr( name );
        class1 = ScmFactory.Class.createInstance( ws, name, name + "_desc" );
        class1.attachAttr( attr.getId() );
        properties = createProperties( class1, 5 );
        prepareFile( name, dirPath1, scmTags, properties );
        prepareFile( name, dirPath2, scmTags, properties );
    }

    // SEQUOIACM-1312暂时屏蔽
    @Test(groups = { "oneSite", "twoSite", "fourSite" }, enabled = false)
    private void test() throws Exception {
        ScmDirectory dir = ScmFactory.Directory.getInstance( ws, dirPath1 );
        BSONObject queryCond = ScmQueryBuilder
                .start( ScmAttributeName.File.PROPERTIES + "." + name ).is( 5 )
                .and( "tags" ).in( name )
                .and( ScmAttributeName.File.DIRECTORY_ID ).is( dir.getId() )
                .get();
        System.out.println( "cond = " + queryCond.toString() );
        System.out.println(
                "fileIdList = " + fileIdList.subList( 20, 25 ).toString() );
        int count = 0;
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScopeType.SCOPE_CURRENT, queryCond );
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo fileInfo = cursor.getNext();
            Assert.assertEquals( fileInfo.getMinorVersion(), 0 );
            Assert.assertEquals( fileInfo.getMajorVersion(), 1 );
            count++;
        }
        Assert.assertEquals( count, fileNum );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( !runSuccess || TestScmBase.forceClear ) {
                if ( fileIdList != null ) {
                    System.out
                            .println( "fileIdList = " + fileIdList.toString() );
                }
            }
            for ( ScmId fileId : fileIdList ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
            deleteDir( ws, dirPath1 );
            deleteDir( ws, dirPath2 );
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

    private void prepareFile( String name, String dirPath, ScmTags tags,
            ScmClassProperties properties ) throws Exception {
        List< String > pathList = getSubPaths( dirPath );
        for ( int i = 0; i < pathList.size(); i++ ) {
            for ( int j = 0; j < fileNum; j++ ) {
                ScmDirectory dir = ScmFactory.Directory.getInstance( ws,
                        pathList.get( i ) );
                ScmId fileId = createFile( name, dir, tags, properties );
                fileIdList.add( fileId );
            }
        }
    }

    private ScmId createFile( String name, ScmDirectory dir, ScmTags tags,
            ScmClassProperties properties ) throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( name + "_" + UUID.randomUUID() );
        file.setAuthor( name );
        file.setDirectory( dir );
        file.setTags( tags );
        file.setClassProperties( properties );
        ScmId fileId = file.save();
        return fileId;
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

    private ScmAttribute createAttr( String name ) throws ScmException {
        ScmAttributeConf conf = new ScmAttributeConf();
        conf.setName( name );
        conf.setDescription( name + "_desc" );
        conf.setDisplayName( name + "_display" );
        conf.setRequired( false );
        conf.setType( AttributeType.INTEGER );
        ScmAttribute attr = ScmFactory.Attribute.createInstance( ws, conf );
        return attr;
    }

    private ScmDirectory createDir( ScmWorkspace ws, String dirPath )
            throws ScmException {
        List< String > pathList = getSubPaths( dirPath );
        for ( String path : pathList ) {
            try {
                ScmFactory.Directory.createInstance( ws, path );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DIR_EXIST ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
        return ScmFactory.Directory.getInstance( ws,
                pathList.get( pathList.size() - 1 ) );
    }

    private void deleteDir( ScmWorkspace ws, String dirPath ) {
        List< String > pathList = getSubPaths( dirPath );
        for ( int i = pathList.size() - 1; i >= 0; i-- ) {
            try {
                ScmFactory.Directory.deleteInstance( ws, pathList.get( i ) );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DIR_NOT_FOUND
                        && e.getError() != ScmError.DIR_NOT_EMPTY ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
    }

    private List< String > getSubPaths( String path ) {
        String ele = "/";
        String[] arry = path.split( "/" );
        List< String > pathList = new ArrayList< String >();
        for ( int i = 1; i < arry.length; i++ ) {
            ele = ele + arry[ i ];
            pathList.add( ele );
            ele = ele + "/";
        }
        return pathList;
    }
}
