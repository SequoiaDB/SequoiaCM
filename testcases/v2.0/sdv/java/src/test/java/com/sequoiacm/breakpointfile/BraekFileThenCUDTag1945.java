package com.sequoiacm.breakpointfile;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.sequoiacm.testresource.SkipTestException;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.exception.ScmException;
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
public class BraekFileThenCUDTag1945 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String name = "BraekFileThenCUDTag1945";
    private int fileSize = 1024;
    private ScmId fileId = null;
    private ScmTags tags = null;
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
                throw new SkipTestException( "NO Sequoiadb Datasourse, Skip!" );
            }
        }
        site = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( name ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        creatTags( name, false );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        // breakpointFile transfer to ScmFile
        ScmBreakpointFile breakpointFile = createBreakpointFile( name,
                filePath );
        breakpointFile2ScmFile( breakpointFile, name, tags );
        check( fileId, tags );

        // update
        creatTags( name, false );
        updateFile( fileId, tags );
        check( fileId, tags );

        // delete
        for ( String value : tags.toSet() ) {
            tags.removeTag( value );
        }
        updateFile( fileId, tags );
        checkDel( fileId );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( !runSuccess || TestScmBase.forceClear ) {
                if ( fileId != null ) {
                    System.out.println( "fileId = " + fileId.get() );
                }
            }
            ScmFactory.File.deleteInstance( ws, fileId, true );
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
            String name, ScmTags tags ) throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( breakpointFile );
        file.setFileName( name );
        file.setAuthor( name );
        file.setTags( tags );
        fileId = file.save();
    }

    private void updateFile( ScmId fileId, ScmTags tag ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        file.setTags( tag );
    }

    private ScmTags creatTags( String name, boolean mutil )
            throws ScmException {
        tags = new ScmTags();
        if ( !mutil ) {
            tags.addTag( name + "_" + UUID.randomUUID() );
        } else {
            Set< String > set = new HashSet<>();
            set.add( name + "_" + UUID.randomUUID() );
            set.add( name + "_" + UUID.randomUUID() );
            set.add( name + "_" + UUID.randomUUID() );
            tags.addTags( set );
        }
        return tags;
    }

    private void check( ScmId fileId, ScmTags tag ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        Assert.assertEquals( file.getFileName(), name );
        Assert.assertEquals( file.getAuthor(), name );
        Assert.assertEquals( file.getTags().toSet().size(), 1 );
        Assert.assertEquals( file.getTags().toSet().containsAll( tag.toSet() ),
                true, "fileTags = " + file.getTags().toString() + ",expTags = "
                        + tag.toString() );
    }

    private void checkDel( ScmId fileId ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmTags tag = file.getTags();
        Assert.assertEquals( tag.toSet().size(), 0, tag.toSet().toString() );
    }
}
