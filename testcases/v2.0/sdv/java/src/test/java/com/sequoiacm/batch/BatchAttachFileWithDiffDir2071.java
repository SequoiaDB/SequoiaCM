package com.sequoiacm.batch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description: SCM-2071 :: 批次中添加不同目录下的文件
 * @author fanyu
 * @Date:2018年7月11日
 * @version:1.0
 */
public class BatchAttachFileWithDiffDir2071 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    private String name = "BatchAttachFileWithDiffDir2071";
    private int fileSize = 1;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private String dirPath1 = "/2071_A/2071_B/2071_C/2071_D/2071_E/";
    private String dirPath2 = "/2071_E/2071_F/2071_G/2071_H/2071_I/";

    private ScmId batchId = null;

    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";

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

        prepareFile( dirPath1 );
        prepareFile( dirPath2 );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        // create batch
        ScmBatch batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( name );
        batchId = batch.save();

        // attch file
        for ( ScmId fileId : fileIdList ) {
            batch.attachFile( fileId );
        }

        // get Batch
        ScmBatch batch1 = ScmFactory.Batch.getInstance( ws, batchId );
        Assert.assertEquals( batch1.getName(), name );
        Assert.assertEquals( batch1.listFiles().size(), fileIdList.size() );

        // get file
        int index = ( int ) Math.random() * 10;
        ScmFile file = ScmFactory.File
                .getInstance( ws, fileIdList.get( index ) );
        String downloadPath = TestTools.LocalFile
                .initDownloadPath( localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
        file.getContent( downloadPath );
        // check content
        Assert.assertEquals( TestTools.getMD5( filePath ),
                TestTools.getMD5( downloadPath ) );
        // check path
        String filePath = null;
        if ( index < 4 ) {
            filePath = getSubPaths( dirPath1 ).get( index ) + "/";
        } else {
            filePath = getSubPaths( dirPath1 ).get( index / 5 ) + "/";
        }
        Assert.assertEquals( file.getDirectory().getPath(), filePath );
        Assert.assertEquals( file.getBatchId(), batchId );
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
            ScmFactory.Batch.deleteInstance( ws, batchId );
            deleteDir( ws, dirPath1 );
            deleteDir( ws, dirPath2 );
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

    private void prepareFile( String dirPath ) throws Exception {
        List< String > pathList = getSubPaths( dirPath );
        for ( int i = 0; i < pathList.size(); i++ ) {
            ScmDirectory dir = ScmFactory.Directory
                    .getInstance( ws, pathList.get( i ) );
            ScmId fileId = createFile( filePath, name, dir );
            fileIdList.add( fileId );
        }
    }

    private ScmId createFile( String filePath, String name, ScmDirectory dir )
            throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( name + "_" + UUID.randomUUID() );
        file.setAuthor( name );
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
        return ScmFactory.Directory
                .getInstance( ws, pathList.get( pathList.size() - 1 ) );
    }

    private void deleteDir( ScmWorkspace ws, String dirPath ) {
        List< String > pathList = getSubPaths( dirPath );
        for ( int i = pathList.size() - 1; i >= 0; i-- ) {
            try {
                ScmFactory.Directory.deleteInstance( ws, pathList.get( i ) );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DIR_NOT_FOUND &&
                        e.getError() != ScmError.DIR_NOT_EMPTY ) {
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
