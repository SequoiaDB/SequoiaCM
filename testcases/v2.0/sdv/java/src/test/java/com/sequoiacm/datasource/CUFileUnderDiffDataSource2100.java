package com.sequoiacm.datasource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testresource.SkipTestException;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
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
 * @Description: SCM-2100 SCM-2101 :: 创建文件和更新文件站点数据源分别为hbase和hdfs
 * @author fanyu
 * @Date:2018年7月11日
 * @version:1.0
 */
public class CUFileUnderDiffDataSource2100 extends TestScmBase {
    private boolean runSuccess1 = false;
    private boolean runSuccess2 = false;
    private SiteWrapper hbaseSite = null;
    private SiteWrapper hdfsSite = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmSession session1 = null;
    private ScmWorkspace ws = null;
    private ScmWorkspace ws1 = null;

    private String name = "CUFileUnderDiffDataSource2100";
    private int fileSize = 1024;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private File localPath = null;
    private String filePath1 = null;
    private String filePath2 = null;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath1 = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        filePath2 = localPath + File.separator + "localFile_" + fileSize / 2
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath1, fileSize );
        TestTools.LocalFile.createFile( filePath2, fileSize / 2 );

        List< SiteWrapper > siteList = ScmInfo.getAllSites();
        for ( int i = 0; i < siteList.size(); i++ ) {
            if ( siteList.get( i ).getDataType()
                    .equals( DatasourceType.HBASE ) ) {
                hbaseSite = siteList.get( i );
            }
            if ( siteList.get( i ).getDataType()
                    .equals( DatasourceType.HDFS ) ) {
                hdfsSite = siteList.get( i );
            }
        }
        if ( hbaseSite == null || hdfsSite == null ) {
            throw new SkipTestException( "NO hbase/hdfs Datasourse, Skip!" );
        }

        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( hbaseSite );
        session1 = TestScmTools.createSession( hdfsSite );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        ws1 = ScmFactory.Workspace.getWorkspace( wsp.getName(), session1 );
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( name ).get();
        ScmFileUtils.cleanFile( wsp, cond );
    }

    @Test(groups = { GroupTags.fourSite })
    private void testCreateInHbase() throws Exception {
        ScmId fileId = createFile( ws, name, filePath1 );
        updateFile( ws1, fileId, filePath2 );
        int currentVerion = 2;
        checkResult( fileId, ws1, currentVerion );
        runSuccess1 = true;
    }

    @Test(groups = { GroupTags.fourSite })
    private void testCreateInHdfs() throws Exception {
        ScmId fileId = createFile( ws1, name, filePath1 );
        updateFile( ws, fileId, filePath2 );
        int currentVerion = 2;
        checkResult( fileId, ws, currentVerion );
        runSuccess2 = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
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
        } finally {
            if ( session != null ) {
                session.close();
            }
            if ( session1 != null ) {
                session1.close();
            }
        }
    }

    private ScmId createFile( ScmWorkspace ws, String name, String filePath )
            throws ScmException {
        // upload file and set tags
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( name + "_" + UUID.randomUUID() );
        file.setAuthor( name );
        file.setContent( filePath );
        ScmId fileId = file.save();
        fileIdList.add( fileId );
        return fileId;
    }

    private void updateFile( ScmWorkspace ws, ScmId fileId, String filePath )
            throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        // file.setTags(tags);
        file.updateContent( filePath );
    }

    private void checkResult( ScmId fileId, ScmWorkspace ws, int currentVerion )
            throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        Assert.assertEquals( file.getAuthor(), name );
        Assert.assertEquals( file.getMajorVersion(), currentVerion );
        Assert.assertEquals( file.getSize(), fileSize / 2 );
        Assert.assertEquals( file.getDirectory().getPath(), "/" );
        // check content
        VersionUtils.CheckFileContentByFile( ws, fileId, currentVerion,
                filePath2, localPath );
    }
}