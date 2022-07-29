package com.sequoiacm.s3.version.concurrent;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Description: SCM-4858 ::开启版本控制，并发删除和获取文件列表
 * @author wuyan
 * @Date 2022.07.22
 * @version 1.00
 */
public class ScmFile4858 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4858";
    private String fileName = "scmfile4858";
    private ScmId fileId = null;
    private int fileSize = 1024;
    private int updateSize = 1024 * 1024 * 2;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmBucket scmBucket = null;
    private List< ScmId > fileIds = new ArrayList<>();
    private int fileNums = 5;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        updatePath = localPath + File.separator + "localFile_" + updateSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( updatePath, updateSize );

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
    }

    @Test
    public void testCreateObject() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        List< String > expKeys = new ArrayList<>();
        for ( int i = 0; i < fileNums; i++ ) {
            String key = fileName + "_" + i;
            ScmId fileId = S3Utils.createFile( scmBucket, key, filePath );
            fileIds.add( fileId );
            expKeys.add( key );
            expKeys.add( key );
            DeleteFile deleteFile = new DeleteFile( key );
            es.addWorker( deleteFile );
        }

        ListFile listFile = new ListFile();
        es.addWorker( listFile );
        es.run();

        checkResult( expKeys );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.clearBucket( session, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class ListFile {
        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            BSONObject condition = ScmQueryBuilder
                    .start().or( fileIdBSON( 0 ), fileIdBSON( 1 ),
                            fileIdBSON( 2 ), fileIdBSON( 3 ), fileIdBSON( 4 ) )
                    .get();
            ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File
                    .listInstance( ws, ScmType.ScopeType.SCOPE_ALL, condition );
            int deleteMarkerVersion = 2;
            List< BSONObject > actFileNames = new ArrayList<>();
            while ( cursor.hasNext() ) {
                ScmFileBasicInfo file = cursor.getNext();
                int version = file.getMajorVersion();
                if ( file.isDeleteMarker() ) {
                    Assert.assertEquals( version, deleteMarkerVersion );
                } else {
                    Assert.assertEquals( version, 1 );
                }
                BSONObject fileInfo = new BasicBSONObject();
                fileInfo.put( file.getFileName(), version );
                actFileNames.add( fileInfo );
            }
            cursor.close();

        }
    }

    private class DeleteFile {
        private String fileName;

        private DeleteFile( String fileName ) {
            this.fileName = fileName;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( ScmInfo.getSite() );
                ScmBucket scmBucket = ScmFactory.Bucket.getBucket( session,
                        bucketName );
                scmBucket.deleteFile( fileName, false );
            } finally {
                session.close();
            }
        }
    }

    private void checkResult( List< String > expKeys ) throws Exception {
        BSONObject condition = ScmQueryBuilder.start()
                .or( fileIdBSON( 0 ), fileIdBSON( 1 ), fileIdBSON( 2 ),
                        fileIdBSON( 3 ), fileIdBSON( 4 ) )
                .get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScmType.ScopeType.SCOPE_ALL, condition );
        int size = 0;
        int deleteMarkerVersion = 2;
        List< String > actFileNames = new ArrayList<>();
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            int version = file.getMajorVersion();
            Assert.assertTrue( fileIds.contains( file.getFileId() ),
                    "--verson is " + version + "--fileId ="
                            + file.getFileId() );
            actFileNames.add( file.getFileName() );
            if ( file.isDeleteMarker() ) {
                Assert.assertEquals( version, deleteMarkerVersion );
            } else {
                Assert.assertEquals( version, 1 );
            }
            size++;
        }
        int fileVersionNum = 10;
        Assert.assertEquals( size, fileVersionNum, actFileNames.toString() );

        Collections.sort( actFileNames );
        Collections.sort( expKeys );
        Assert.assertEquals( actFileNames, expKeys );
    }

    private BSONObject fileIdBSON( int i ) throws ScmException {
        return ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileIds.get( i ).get() ).get();
    }
}
