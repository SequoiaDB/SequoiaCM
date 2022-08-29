package com.sequoiacm.batch.serial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmBatchInfo;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:Transfer file of the batch , specify batch condition
 * testlink-case:SCM-2082
 *
 * @author wuyan
 * @Date 2018.07.17
 * @version 1.00
 */

public class TransferFileByBatchCond2082 extends TestScmBase {
    private static WsWrapper wsp = null;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmId taskId = null;
    private BSONObject condition = null;

    private String fileName1 = "file_batch_2082a";
    private String fileName2 = "file_batch_2082b";
    private ScmId fileId1 = null;
    private ScmId fileId2 = null;
    private ScmId batchId1 = null;
    private ScmId batchId2 = null;
    private String batchName1 = "batch_2082a";
    private String batchName2 = "batch_2082b";
    private byte[] writeData1 = new byte[ 1024 * 10 ];
    private byte[] writeData2 = new byte[ 1024 * 5 ];
    private byte[] updateData1 = new byte[ 1024 * 2 ];
    private byte[] updateData2 = new byte[ 1024 * 5 ];
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();

        sessionA = TestScmTools.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );

        // clean batch
        BSONObject tagBson = new BasicBSONObject( "tags", "tag2082" );
        ScmCursor< ScmBatchInfo > cursor = ScmFactory.Batch.listInstance( wsM,
                new BasicBSONObject( "tags", tagBson ) );
        while ( cursor.hasNext() ) {
            ScmBatchInfo info = cursor.getNext();
            ScmId batchId = info.getId();
            ScmFactory.Batch.deleteInstance( wsM, batchId );
        }
        cursor.close();

        // create file and update content
        fileId1 = VersionUtils.createFileByStream( wsA, fileName1, writeData1 );
        fileId2 = VersionUtils.createFileByStream( wsA, fileName2, writeData2 );
        VersionUtils.updateContentByStream( wsA, fileId1, updateData1 );
        VersionUtils.updateContentByStream( wsA, fileId2, updateData2 );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        batchId1 = createBatchAndAttachFile( wsA, batchName1, fileId1 );
        batchId2 = createBatchAndAttachFile( wsA, batchName2, fileId2 );
        startTransferTaskByBatch( wsA, sessionA, batchId1 );

        checkTransferedFileSiteAndDataInfo( wsA, fileName1 );
        checkNoTransferFileInfo( wsM, batchId2 );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                TestSdbTools.Task.deleteMeta( taskId );
                ScmFactory.Batch.deleteInstance( wsA, batchId1 );
                ScmFactory.Batch.deleteInstance( wsA, batchId2 );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() + e.getStackTrace() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionM != null ) {
                sessionM.close();
            }
        }
    }

    private void startTransferTaskByBatch( ScmWorkspace ws, ScmSession session,
            ScmId batchId ) throws Exception {
        condition = ScmQueryBuilder.start()
                .put( ScmAttributeName.File.BATCH_ID ).is( batchId.toString() )
                .get();
        taskId = ScmSystem.Task.startTransferTask( ws, condition,
                ScopeType.SCOPE_CURRENT, rootSite.getSiteName() );

        // wait task finish
        ScmTaskUtils.waitTaskFinish( session, taskId );
    }

    private ScmId createBatchAndAttachFile( ScmWorkspace ws, String batchName,
            ScmId fileId ) throws ScmException {
        ScmBatch batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( batchName );
        ScmId batchId = batch.save();
        batch.attachFile( fileId );

        // add tags
        ScmTags tags = new ScmTags();
        tags.addTag( "tag2082" );
        batch.setTags( tags );
        return batchId;
    }

    private void checkTransferedFileSiteAndDataInfo( ScmWorkspace ws,
            String fileName ) throws Exception {
        int currentVersion = 2;
        // check the transfered file,check the sitelist and data ,only
        // transfered current file
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScopeType.SCOPE_CURRENT, condition );
        int size = 0;
        SiteWrapper[] expSiteList = { rootSite, branSite };
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            ScmId fileId = file.getFileId();
            int majorVersion = file.getMajorVersion();
            if ( majorVersion == currentVersion ) {
                // check sitelist and transfered fileContent of the
                // currentVersion file
                VersionUtils.checkSite( ws, fileId, currentVersion,
                        expSiteList );
                VersionUtils.CheckFileContentByStream( ws, fileName,
                        majorVersion, updateData1 );
            } else {
                Assert.fail( "the file version is error!" + majorVersion );
            }
            size++;
        }
        cursor.close();
        int expFileNum = 1;
        Assert.assertEquals( size, expFileNum );
    }

    private void checkNoTransferFileInfo( ScmWorkspace ws, ScmId batchId )
            throws ScmException {
        int currentVersion = 2;
        int historyVersion = 1;
        // check the no transfer file by batch2
        BSONObject condition = ScmQueryBuilder.start()
                .put( ScmAttributeName.File.BATCH_ID ).is( batchId2.toString() )
                .get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScopeType.SCOPE_CURRENT, condition );
        int size = 0;
        SiteWrapper[] expSiteList = { branSite };
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            ScmId fileId = file.getFileId();
            int majorVersion = file.getMajorVersion();
            if ( majorVersion == currentVersion ) {
                VersionUtils.checkSite( ws, fileId, majorVersion, expSiteList );
            } else {
                Assert.fail( "the file version is error!" + majorVersion );
            }
            size++;
        }
        cursor.close();
        int expFileNum = 1;
        Assert.assertEquals( size, expFileNum );

        // check the no transfer file by history version file
        List< String > fileIdList = new ArrayList<>( 2 );
        fileIdList.add( fileId1.get() );
        fileIdList.add( fileId2.get() );
        BSONObject condition1 = ScmQueryBuilder.start()
                .put( ScmAttributeName.File.FILE_ID ).in( fileIdList ).get();
        ScmCursor< ScmFileBasicInfo > cursor1 = ScmFactory.File
                .listInstance( ws, ScopeType.SCOPE_HISTORY, condition1 );
        int size1 = 0;
        while ( cursor1.hasNext() ) {
            ScmFileBasicInfo file1 = cursor1.getNext();
            ScmId fileId1 = file1.getFileId();
            int majorVersion1 = file1.getMajorVersion();
            if ( majorVersion1 == historyVersion ) {
                VersionUtils.checkSite( ws, fileId1, majorVersion1,
                        expSiteList );
            } else {
                Assert.fail( "the file version is error!" + majorVersion1 );
            }
            size1++;
        }
        cursor1.close();
        int expFileNum1 = 2;
        Assert.assertEquals( size1, expFileNum1 );
    }

}