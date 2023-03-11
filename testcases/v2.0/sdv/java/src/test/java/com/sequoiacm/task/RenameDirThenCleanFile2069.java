package com.sequoiacm.task;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.sequoiacm.testresource.SkipTestException;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description: SCM-2069 :: 重命名文件夹，文件夹下文件执行清理操作
 * @author fanyu
 * @Date:2018年7月11日
 * @version:1.0
 */
public class RenameDirThenCleanFile2069 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site1 = null;
    private SiteWrapper site2 = null;
    private WsWrapper wsp = null;
    private ScmSession session1 = null;
    private ScmSession session2 = null;
    private ScmWorkspace ws1 = null;
    private ScmWorkspace ws2 = null;

    private String name = "RenameDirThenCleanFile2069";
    private int fileSize = 1024;
    private int fileNum = 50;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private String dirPath = "/2069_A/2069_B/2069_C/2069_D/";
    private String newDirPath = "/2069_A/2069_B/2069_C/2069_E/";
    private ScmDirectory dir = null;

    private ScmId taskId = null;

    private File localPath = null;
    private String filePath = null;
    private Calendar calendar = Calendar.getInstance();

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        List< SiteWrapper > siteList = ScmInfo
                .getBranchSites( ScmInfo.getAllSites().size() - 1 );
        if ( siteList.size() <= 1 ) {
            throw new SkipTestException(
                    "branch site's num is less than 2, Skip!" );
        }
        if ( siteList.get( 0 ).getSiteId() < siteList.get( 1 ).getSiteId() ) {
            site1 = siteList.get( 0 );
            site2 = siteList.get( 1 );
        } else {
            site1 = siteList.get( 1 );
            site2 = siteList.get( 0 );
        }

        wsp = ScmInfo.getWs();
        session1 = TestScmTools.createSession( site1 );
        session2 = TestScmTools.createSession( site2 );
        ws1 = ScmFactory.Workspace.getWorkspace( wsp.getName(), session1 );
        ws2 = ScmFactory.Workspace.getWorkspace( wsp.getName(), session2 );

        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( name ).get();
        ScmFileUtils.cleanFile( wsp, cond );

        BSONObject cond1 = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( name + "_2" ).get();
        ScmFileUtils.cleanFile( wsp, cond1 );

        deleteDir( ws1, dirPath );
        deleteDir( ws1, newDirPath );
        dir = createDir( ws1, dirPath );
        calendar.set( Calendar.HOUR, calendar.get( Calendar.HOUR ) - 3 );
        prepareFile( dir );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        // rename dir
        String newname = "2069_E";
        ScmDirectory dir = ScmFactory.Directory.getInstance( ws1, dirPath );
        dir.rename( newname );

        // create file in 2069_E
        prepareFile( dir );

        // clean
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( name ).and( ScmAttributeName.File.DIRECTORY_ID )
                .is( dir.getId() ).get();
        taskId = ScmSystem.Task.startCleanTask( ws1, cond );
        waitTaskStop();

        // check meta data
        // clean
        checkResult( fileIdList.subList( 0, fileNum / 2 ), true );
        checkResult( fileIdList.subList( fileNum, fileNum + fileNum / 2 ),
                true );
        // unclean
        checkResult( fileIdList.subList( fileNum / 2, fileNum ), false );
        checkResult( fileIdList.subList( fileNum + fileNum / 2, 2 * fileNum ),
                false );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( !runSuccess || TestScmBase.forceClear ) {
                if ( fileIdList != null ) {
                    for ( ScmId fileId : fileIdList ) {
                        System.out.println( "fileId = " + fileId.get() );
                    }
                }
            }
            for ( ScmId fileId : fileIdList ) {
                ScmFactory.File.deleteInstance( ws1, fileId, true );
            }
            deleteDir( ws1, dirPath );
            deleteDir( ws1, newDirPath );
            TestSdbTools.Task.deleteMeta( taskId );
            TestTools.LocalFile.removeFile( localPath );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session1 != null ) {
                session1.close();
            }
            if ( session2 != null ) {
                session2.close();
            }
        }
    }

    private void checkResult( List< ScmId > fileIdList, boolean flag )
            throws Exception {
        SiteWrapper[] expSiteList = null;
        ;
        if ( !flag ) {
            for ( ScmId fileId : fileIdList ) {

                ScmFile file = ScmFactory.File.getInstance( ws1, fileId );
                if ( file.getLocationList().size() > 2 ) {
                    expSiteList = new SiteWrapper[ 3 ];
                    expSiteList[ 0 ] = site1;
                    expSiteList[ 1 ] = site2;
                    expSiteList[ 2 ] = ScmInfo.getRootSite();
                } else {
                    expSiteList = new SiteWrapper[ 2 ];
                    expSiteList[ 0 ] = site1;
                    expSiteList[ 1 ] = site2;
                }
                ScmFileUtils.checkMetaAndData( wsp, fileId, expSiteList,
                        localPath, filePath );
            }
        } else {
            for ( ScmId fileId : fileIdList ) {
                ScmFile file = ScmFactory.File.getInstance( ws1, fileId );
                if ( file.getLocationList().size() > 1 ) {
                    expSiteList = new SiteWrapper[ 2 ];
                    expSiteList[ 0 ] = ScmInfo.getRootSite();
                    expSiteList[ 1 ] = site2;
                } else {
                    expSiteList = new SiteWrapper[ 1 ];
                    expSiteList[ 0 ] = site2;
                }
                ScmFileUtils.checkMetaAndData( wsp, fileId,
                        ( SiteWrapper[] ) expSiteList, localPath, filePath );
            }
        }
    }

    private void waitTaskStop() throws ScmException {
        Date stopTime = null;
        while ( stopTime == null ) {
            stopTime = ScmSystem.Task.getTask( session1, taskId ).getStopTime();
        }
    }

    private void prepareFile( ScmDirectory dir ) throws Exception {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmId fileId = null;
            if ( i < fileNum / 2 ) {
                fileId = createFile( filePath, name, dir );
            } else {
                fileId = createFile( filePath, name + "_2", dir );
            }
            readFile( fileId );
            fileIdList.add( fileId );
        }
    }

    private void readFile( ScmId fileId ) throws Exception {
        ScmFile file2 = ScmFactory.File.getInstance( ws2, fileId );
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        file2.getContent( downloadPath );
    }

    private ScmId createFile( String filePath, String name, ScmDirectory dir )
            throws ScmException, ParseException {
        ScmFile file = ScmFactory.File.createInstance( ws1 );
        file.setContent( filePath );
        file.setFileName( name + "_" + UUID.randomUUID() );
        file.setAuthor( name );
        file.setCreateTime( calendar.getTime() );
        file.setDirectory( dir );
        ScmId fileId = file.save();
        return fileId;
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
