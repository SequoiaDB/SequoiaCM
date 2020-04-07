package com.sequoiacm.scmfile.oper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
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
 * @Description: SCM-1943 :: 更新文件后删除自定义标签
 * @author fanyu
 * @Date:2018年7月11日
 * @version:1.0
 */
public class UpdateFileThenDelTag1943 extends TestScmBase {
    private boolean runSuccess1 = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String name = "UpdateFileThenDelTag1943";
    private int fileSize = 1024 * 1024;
    private ScmId fileId = null;
    private ScmTags tags = null;
    private File localPath = null;
    private int filePathNum = 3;
    private List< String > filePathList = new ArrayList< String >();

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        for ( int i = 1; i <= filePathNum; i++ ) {
            String filePath = localPath + File.separator + "localFile_" +
                    ( int ) ( fileSize / Math.pow( 2, i - 1 ) )
                    + ".txt";
            TestTools.LocalFile.createFile( filePath,
                    ( int ) ( fileSize / Math.pow( 2, i - 1 ) ) );
            filePathList.add( filePath );
        }
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( name ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        // create file
        tags = new ScmTags();
        tags.addTag( name + "-1" );
        tags.addTag( name + "-2" );
        tags.addTag( name + "-3" );
        tags.addTag( name + "-4" );
        tags.addTag( name + "-5" );
        fileId = createFile( name, tags, filePathList.get( 0 ) );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testSingle() throws Exception {
        // update file
        updateFile( fileId, tags, filePathList.get( 1 ) );
        Iterator< String > keySet = tags.toSet().iterator();
        // delete tags
        while ( keySet.hasNext() ) {
            String key = keySet.next();
            keySet.remove();
            tags.removeTag( key );
        }
        // update file
        updateFile( fileId, tags, filePathList.get( 2 ) );
        int currVersion = 3;
        // check
        for ( int i = 1; i <= currVersion; i++ ) {
            checkAttr( fileId, name,
                    ( int ) ( fileSize / Math.pow( 2, i - 1 ) ), i );
            VersionUtils.CheckFileContentByFile( ws, fileId, i,
                    filePathList.get( i - 1 ), localPath );
            SiteWrapper[] expSites = { site };
            ScmFileUtils.checkMeta( ws, fileId, expSites );
        }
        runSuccess1 = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( !runSuccess1 || TestScmBase.forceClear ) {
                System.out.println( "fileId = " + fileId.get() );
            }
            ScmFactory.File.deleteInstance( ws, fileId, true );
            TestTools.LocalFile.removeFile( localPath );
        } finally {
            if ( session != null ) {
                session.close();
            }
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
        file.updateContent( filePath );
        // update tags
        ScmFile file1 = ScmFactory.File.getInstance( ws, fileId );
        file1.setTags( tags );
    }

    private void checkAttr( ScmId fileId, String fileName, int fileSize,
            int version ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId, version, 0 );
        Assert.assertEquals( file.getFileName(), fileName );
        Assert.assertEquals( file.getAuthor(), name );
        Assert.assertEquals( file.getMajorVersion(), version );
        Assert.assertEquals( file.getSize(), fileSize );
        ScmTags actTags = file.getTags();
        Assert.assertEquals( actTags.toSet().size(), 0 );
    }
}