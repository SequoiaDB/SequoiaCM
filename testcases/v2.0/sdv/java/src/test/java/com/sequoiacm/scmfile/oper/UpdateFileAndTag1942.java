package com.sequoiacm.scmfile.oper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
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
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @Description: SCM-1942 :: 更新文件后修改自定义标签
 * @author fanyu
 * @Date:2018年7月11日
 * @version:1.0
 */
public class UpdateFileAndTag1942 extends TestScmBase {
    private boolean runSuccess1 = false;
    private boolean runSuccess2 = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String name = "UpdateFileAndTag1942";
    private int fileSize = 1024;
    private int num = 2;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private File localPath = null;
    private String filePath1 = null;
    private String filePath2 = null;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath1 =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        filePath2 =
                localPath + File.separator + "localFile_" + fileSize + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath1, fileSize );
        TestTools.LocalFile.createFile( filePath2, fileSize / 2 );

        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( name ).get();
        ScmFileUtils.cleanFile( wsp, cond );

        // create file
        createAllFile( num );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testSingle() throws Exception {
        // update file content and tags
        // update single tag
        ScmTags tags1 = new ScmTags();
        tags1.addTag( name + "_0" );
        updateFile( fileIdList.get( 0 ), tags1, filePath2 );

        int currVersion = 2;
        int histVersion = 1;
        // check
        checkAttr( fileIdList.get( 0 ), name + "_0", fileSize / 2, currVersion,
                tags1 );
        VersionUtils
                .CheckFileContentByFile( ws, fileIdList.get( 0 ), currVersion,
                        filePath2, localPath );
        VersionUtils
                .CheckFileContentByFile( ws, fileIdList.get( 0 ), histVersion,
                        filePath2, localPath );
        SiteWrapper[] expSites = { site };
        ScmFileUtils.checkMeta( ws, fileIdList.get( 0 ), expSites );
        runSuccess1 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testMutil() throws Exception {
        // update file content and tags
        // update mutil tags
        ScmTags tags1 = new ScmTags();
        tags1.addTag( name + "_3_a" );
        tags1.addTag( name + "_3_b" );
        tags1.addTag( name + "_3_c" );
        updateFile( fileIdList.get( 1 ), tags1, filePath2 );

        int currVersion = 2;
        int histVersion = 1;
        // check
        checkAttr( fileIdList.get( 1 ), name + "_1", fileSize / 2, currVersion,
                tags1 );
        VersionUtils
                .CheckFileContentByFile( ws, fileIdList.get( 1 ), currVersion,
                        filePath2, localPath );
        VersionUtils
                .CheckFileContentByFile( ws, fileIdList.get( 1 ), histVersion,
                        filePath2, localPath );
        SiteWrapper[] expSites = { site };
        ScmFileUtils.checkMeta( ws, fileIdList.get( 1 ), expSites );
        runSuccess2 = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( !runSuccess1 || !runSuccess2 || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    System.out.println( "fileId = " + fileId.get() );
                }
            }
            for ( ScmId fileId : fileIdList ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
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

    private void createAllFile( int num ) throws ScmException {
        for ( int i = 0; i < num; i++ ) {
            ScmTags tags = new ScmTags();
            if ( i % 2 == 0 ) {
                tags.addTag( name + "_" + i );
            } else {
                tags.addTag( name + "_" + i + "_a" );
                tags.addTag( name + "_" + i + "_b" );
            }
            ScmId fileId = createFile( name + "_" + i, tags, filePath1 );
            fileIdList.add( fileId );
        }
    }

    private ScmId createFile( String filename, ScmTags tags, String filePath )
            throws ScmException {
        // upload file and set tags
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( filename );
        file.setAuthor( name );
        file.setTags( tags );
        file.setContent( filePath );
        ScmId fileId = file.save();
        return fileId;
    }

    private void updateFile( ScmId fileId, ScmTags tags, String filePath )
            throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        //file.setTags(tags);
        file.updateContent( filePath );
        //update tags
        ScmFile file1 = ScmFactory.File.getInstance( ws, fileId );
        file1.setTags( tags );
    }

    private void checkAttr( ScmId fileId, String fileName, int fileSize,
            int version, ScmTags tags ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId, version, 0 );
        Assert.assertEquals( file.getFileName(), fileName );
        Assert.assertEquals( file.getAuthor(), name );
        Assert.assertEquals( file.getMajorVersion(), version );
        Assert.assertEquals( file.getSize(), fileSize );
        ScmTags actTags = file.getTags();
        Assert.assertEquals( actTags.toSet().size(), tags.toSet().size() );
        Assert.assertEquals( actTags.toString(), tags.toString() );
    }
}