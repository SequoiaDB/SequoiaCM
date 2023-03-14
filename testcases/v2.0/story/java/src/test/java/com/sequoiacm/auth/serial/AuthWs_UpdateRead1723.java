package com.sequoiacm.auth.serial;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @Description:SCM-1723:有单个工作区权限(Update)，对“当前版本权限列表.xlsx”中的各个接口进行覆盖测试,
 * @author fanyu
 * @Date:2018年6月6日
 * @version:1.0
 */
public class AuthWs_UpdateRead1723 extends TestScmBase {
    private SiteWrapper rootsite;
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession sessionM;
    private ScmSession sessionA;
    private ScmSession sessionUR;
    private ScmWorkspace wsM;
    private ScmWorkspace wsA;
    private ScmWorkspace wsUR;
    private String author = "AuthWs_UpdateRead1723";
    private String username = "AuthWs_Update1723";
    private String rolename = "1723_UR";
    private String passwd = "1723";
    private ScmUser user;
    private ScmRole role;
    private ScmResource rs;
    private int fileSize = 1024 * 200;
    private File localPath = null;
    private String filePath = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private List< ScmId > taskIdList = new ArrayList< ScmId >();

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        try {
            localPath = new File( TestScmBase.dataDirectory + File.separator
                    + TestTools.getClassName() );
            filePath = localPath + File.separator + "localFile_" + fileSize
                    + ".txt";
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            rootsite = ScmInfo.getRootSite();
            site = ScmInfo.getBranchSite();
            wsp = ScmInfo.getWs();
            sessionM = ScmSessionUtils.createSession( rootsite );
            sessionA = ScmSessionUtils.createSession( site );
            wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
            wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
            cleanEnv();
            prepare();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite", "star" })
    private void startest() throws Exception {
        nettest();
        testCancelTransferTask();
    }

    @Test(groups = { "twoSite", "fourSite", "net" })
    private void nettest() throws Exception {
        testUpdateBatch();
        testBatchAttachFile();
        testBatchDetachFile();
        testUpdateFile();
        testAsynCaheFile();
        testAsynCaheFileByVersion();
        testAsynTransfer();
        testAsynTransferByVersion();
        testUpdateSche();
        testTransferTask();
        testCleanTask();
        testCleanTaskByScope();
        testCancelCleanTask();
        testTransferTaskByScope();
        testTransferTaskByTarget();
    }

    private void testUpdateBatch() throws ScmException {
        String batchName = "AuthWs_Update1723";
        String newbatchName = "AuthWs_Update1723_new";
        ScmId batchId = null;
        try {
            ScmBatch expBatch = ScmFactory.Batch.createInstance( wsA );
            expBatch.setName( batchName );
            batchId = expBatch.save();
            ScmBatch actBatch = ScmFactory.Batch.getInstance( wsUR,
                    expBatch.getId() );
            // update
            actBatch.setName( newbatchName );

            ScmBatch finaBatch = ScmFactory.Batch.getInstance( wsUR,
                    expBatch.getId() );
            Assert.assertEquals( actBatch.getName(), finaBatch.getName() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( batchId != null ) {
                ScmFactory.Batch.deleteInstance( wsA, batchId );
            }
        }
    }

    private void testBatchAttachFile() throws ScmException {
        String batchName = "AuthWs_Update1723" + "_" + UUID.randomUUID();
        String fileName = "AuthWs_Update1723_file" + "_" + UUID.randomUUID();
        ScmId batchId = null;
        ScmId fileId = null;
        try {
            ScmFile file = ScmFactory.File.createInstance( wsA );
            file.setFileName( fileName );
            fileId = file.save();
            ScmBatch expBatch = ScmFactory.Batch.createInstance( wsA );
            expBatch.setName( batchName );
            batchId = expBatch.save();

            ScmBatch expBatch1 = ScmFactory.Batch.getInstance( wsUR, batchId );
            expBatch1.attachFile( fileId );

            ScmBatch actBatch = ScmFactory.Batch.getInstance( wsA,
                    expBatch.getId() );
            Assert.assertEquals( actBatch.getName(), expBatch1.getName() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( batchId != null ) {
                ScmFactory.Batch.deleteInstance( wsA, batchId );
            }
        }
    }

    private void testBatchDetachFile() throws ScmException {
        String batchName = "AuthWs_Update1723" + "_" + UUID.randomUUID();
        String fileName = "AuthWs_Update1723_file" + "_" + UUID.randomUUID();
        ScmId batchId = null;
        ScmId fileId = null;
        try {
            ScmFile file = ScmFactory.File.createInstance( wsA );
            file.setFileName( fileName );
            fileId = file.save();
            ScmBatch expBatch = ScmFactory.Batch.createInstance( wsA );
            expBatch.setName( batchName );
            batchId = expBatch.save();
            expBatch.attachFile( fileId );

            ScmBatch expBatch1 = ScmFactory.Batch.getInstance( wsUR, batchId );
            expBatch1.detachFile( fileId );

            ScmBatch actBatch = ScmFactory.Batch.getInstance( wsA,
                    expBatch.getId() );
            Assert.assertEquals( actBatch.getName(), expBatch1.getName() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( batchId != null ) {
                ScmFactory.Batch.deleteInstance( wsA, batchId );
            }
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
            }
        }
    }

    private void testUpdateFile() throws ScmException {
        String fileName = "AuthWs_Update1723_file" + "_" + UUID.randomUUID();
        String newfileName = "AuthWs_Update1723_file_new" + "_"
                + UUID.randomUUID();
        ScmId fileId = null;
        try {
            ScmFile expfile = ScmFactory.File.createInstance( wsA );
            expfile.setFileName( fileName );
            fileId = expfile.save();

            ScmFile actfile = ScmFactory.File.getInstance( wsUR, fileId );
            actfile.setFileName( newfileName );

            ScmFile expFile1 = ScmFactory.File.getInstance( wsUR, fileId );
            Assert.assertEquals( actfile.getFileName(),
                    expFile1.getFileName() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
            }
        }
    }

    private void testAsynCaheFile() throws Exception {
        String name = author + "_" + UUID.randomUUID();
        ScmId fileId = null;
        try {
            fileId = createFile( wsM, name );
            ScmFactory.File.asyncCache( wsUR, fileId );
            SiteWrapper[] expSiteList = { rootsite, site };
            ScmTaskUtils.waitAsyncTaskFinished( wsM, fileId,
                    expSiteList.length );
            ScmFileUtils.checkMetaAndData( wsp, fileId, expSiteList, localPath,
                    filePath );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void testAsynCaheFileByVersion() throws Exception {
        String name = author + "_" + UUID.randomUUID();
        ScmId fileId = null;
        try {
            fileId = createFile( wsM, name );
            ScmFactory.File.asyncCache( wsUR, fileId, 1, 0 );
            SiteWrapper[] expSiteList = { rootsite, site };
            ScmTaskUtils.waitAsyncTaskFinished( wsM, fileId,
                    expSiteList.length );
            ScmFileUtils.checkMetaAndData( wsp, fileId, expSiteList, localPath,
                    filePath );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void testAsynTransfer() throws Exception {
        String name = author + "_" + UUID.randomUUID();
        ScmId fileId = null;
        try {
            fileId = createFile( wsA, name );
            ScmFactory.File.asyncTransfer( wsUR, fileId,
                    rootsite.getSiteName() );
            SiteWrapper[] expSiteList = { ScmInfo.getRootSite(), site };
            ScmTaskUtils.waitAsyncTaskFinished( wsM, fileId,
                    expSiteList.length );
            ScmFileUtils.checkMetaAndData( wsp, fileId, expSiteList, localPath,
                    filePath );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void testAsynTransferByVersion() throws Exception {
        String name = author + "_" + UUID.randomUUID();
        ScmId fileId = null;
        try {
            fileId = createFile( wsA, name );

            ScmFactory.File.asyncTransfer( wsUR, fileId, 1, 0,
                    rootsite.getSiteName() );

            SiteWrapper[] expSiteList = { ScmInfo.getRootSite(), site };
            ScmTaskUtils.waitAsyncTaskFinished( wsM, fileId,
                    expSiteList.length );
            ScmFileUtils.checkMetaAndData( wsp, fileId, expSiteList, localPath,
                    filePath );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void testUpdateSche() throws Exception {
        String scheName = "AuthWs_Read1723" + "_" + UUID.randomUUID();
        SiteWrapper rootSite = ScmInfo.getRootSite();
        SiteWrapper branchSite = ScmInfo.getBranchSite();
        String maxStayTime = "0d";
        BSONObject queryCond = null;
        ScmSchedule expSche = null;
        try {
            queryCond = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                    .is( scheName ).get();
            ScmScheduleContent content = new ScmScheduleCopyFileContent(
                    branchSite.getSiteName(), rootSite.getSiteName(),
                    maxStayTime, queryCond );
            String crond = "* * * * * ? 2029";
            expSche = ScmSystem.Schedule.create( sessionA, wsp.getName(),
                    ScheduleType.COPY_FILE, scheName, null, content, crond );

            ScmSchedule actSche = ScmSystem.Schedule.get( sessionUR,
                    expSche.getId() );

            actSche.updateContent( content );
            actSche.updateCron( crond );
            actSche.updateDesc( "" );
            actSche.updateName( scheName );
            Assert.assertEquals( actSche.getName(), expSche.getName() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( expSche != null ) {
                ScmSystem.Schedule.delete( sessionA, expSche.getId() );
            }
        }
    }

    private void testCleanTask() throws Exception {
        String name = author + "_" + UUID.randomUUID();
        ScmId taskId = null;
        try {
            List< ScmId > fileIdList1 = prepareCleanFile( wsM, name, 1 );
            BSONObject condition = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( name ).get();
            taskId = ScmSystem.Task.startCleanTask( wsUR, condition );
            taskIdList.add( taskId );
            ScmTaskUtils.waitTaskFinish( sessionA, taskId );
            SiteWrapper[] expSiteList = { rootsite };
            ScmFileUtils.checkMetaAndData( wsp, fileIdList1, expSiteList,
                    localPath, filePath );
        } catch ( AssertionError e ) {
            throw new Exception( "testCleanTask taskId = " + taskId.get(), e );
        }
    }

    private void testCancelCleanTask() throws Exception {
        String name = author + "_" + UUID.randomUUID();
        ScmTask task = null;
        try {
            int fileNum = 10;
            prepareCleanFile( wsM, name, fileNum );
            BSONObject condition = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( name ).get();
            ScmId taskId = ScmSystem.Task.startCleanTask( wsUR, condition );
            taskIdList.add( taskId );
            ScmSystem.Task.stopTask( sessionUR, taskId );
            waitTaskStop( taskId );
            task = ScmSystem.Task.getTask( sessionUR, taskId );
            Assert.assertEquals( task
                    .getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_CANCEL
                    || task.getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_FINISH,
                    true );
        } catch ( AssertionError e ) {
            throw new Exception(
                    "testCancelCleanTask task = " + task.toString(), e );
        }
    }

    private void testCleanTaskByScope() throws Exception {
        String name = author + "_" + UUID.randomUUID();
        ScmTask task = null;
        try {
            int fileNum = 10;
            prepareCleanFile( wsM, name, fileNum );
            BSONObject condition = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( name ).get();
            ScmId taskId = ScmSystem.Task.startCleanTask( wsUR, condition,
                    ScopeType.SCOPE_CURRENT );
            taskIdList.add( taskId );
            ScmSystem.Task.stopTask( sessionUR, taskId );
            waitTaskStop( taskId );
            task = ScmSystem.Task.getTask( sessionUR, taskId );
            Assert.assertEquals( task
                    .getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_CANCEL
                    || task.getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_FINISH,
                    true );
        } catch ( AssertionError e ) {
            throw new Exception(
                    "testCleanTaskByScope task = " + task.toString(), e );
        }
    }

    private void testTransferTask() throws Exception {
        String name = author + "_" + UUID.randomUUID();
        ScmId taskId = null;
        try {
            List< ScmId > fileIdList = prepareTransferFile( wsA, name, 1 );
            BSONObject condition = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( name ).get();
            taskId = ScmSystem.Task.startTransferTask( wsUR, condition,
                    ScopeType.SCOPE_CURRENT, rootsite.getSiteName() );
            taskIdList.add( taskId );
            ScmTaskUtils.waitTaskFinish( sessionM, taskId );
            SiteWrapper[] siteArr = { rootsite, site };
            ScmFileUtils.checkMetaAndData( wsp, fileIdList, siteArr, localPath,
                    filePath );
        } catch ( AssertionError e ) {
            throw new Exception( "testTransferTask taskId = " + taskId.get(),
                    e );
        }
    }

//    private void testTransferTaskNet() throws Exception {
//        String fileName = author + "_" + UUID.randomUUID();
//        ScmId fileId;
//        ScmId taskId = null;
//        try {
//            fileId = ScmFileUtils.create( wsA, fileName, filePath );
//            fileIdList.add( fileId );
//
//            BSONObject condition = ScmQueryBuilder
//                    .start( ScmAttributeName.File.FILE_NAME ).is( fileName )
//                    .get();
//            taskId = ScmSystem.Task.startTransferTask( wsUR, condition,
//                    ScopeType.SCOPE_CURRENT, rootsite.getSiteName() );
//            taskIdList.add( taskId );
//
//            ScmTaskUtils.waitTaskFinish( sessionA, taskId );
//            SiteWrapper[] siteArr = { rootsite, site };
//            ScmFileUtils.checkMetaAndData( wsp, fileId, siteArr, localPath,
//                    filePath );
//        } catch ( AssertionError e ) {
//            throw new Exception( "testTransferTask taskId = " + taskId, e );
//        }
//    }

    private void testCancelTransferTask() throws Exception {
        String name = author + "_" + UUID.randomUUID();
        ScmTask task = null;
        try {
            prepareTransferFile( wsA, name, 10 );
            BSONObject condition = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( name ).get();
            ScmId taskId = ScmSystem.Task.startTransferTask( wsUR, condition );
            taskIdList.add( taskId );
            ScmSystem.Task.stopTask( sessionUR, taskId );
            waitTaskStop( taskId );
            task = ScmSystem.Task.getTask( sessionUR, taskId );
            Assert.assertEquals( task
                    .getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_CANCEL
                    || task.getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_FINISH,
                    true );
        } catch ( AssertionError e ) {
            throw new Exception(
                    "testCancelTransferTask task = " + task.toString(), e );
        }
    }

    private void testTransferTaskByScope() throws Exception {
        String name = author + "_" + UUID.randomUUID();
        ScmId taskId = null;
        try {
            List< ScmId > fileIdList = prepareTransferFile( wsA, name, 2 );
            BSONObject condition = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( name ).get();
            taskId = ScmSystem.Task.startTransferTask( wsUR, condition,
                    ScopeType.SCOPE_CURRENT, rootsite.getSiteName() );
            taskIdList.add( taskId );
            ScmTaskUtils.waitTaskFinish( sessionM, taskId );
            SiteWrapper[] siteArr = { rootsite, site };
            ScmFileUtils.checkMetaAndData( wsp, fileIdList, siteArr, localPath,
                    filePath );
        } catch ( AssertionError e ) {
            throw new Exception(
                    "testTransferTaskByScope taskId = " + taskId.get(), e );
        }
    }

    private void testTransferTaskByTarget() throws Exception {
        String name = author + "_" + UUID.randomUUID();
        ScmId taskId = null;
        try {
            List< ScmId > fileIdList = prepareTransferFile( wsA, name, 2 );

            BSONObject condition = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( name ).get();
            taskId = ScmSystem.Task.startTransferTask( wsUR, condition,
                    ScopeType.SCOPE_CURRENT, rootsite.getSiteName() );
            taskIdList.add( taskId );

            ScmTaskUtils.waitTaskFinish( sessionM, taskId );
            SiteWrapper[] siteArr = { rootsite, site };
            ScmFileUtils.checkMetaAndData( wsp, fileIdList, siteArr, localPath,
                    filePath );
        } catch ( AssertionError e ) {
            throw new Exception(
                    "testTransferTaskByTarget taskId = " + taskId.get(), e );
        }
    }

    private void grantPriAndAttachRole( ScmSession session, ScmResource rs,
            ScmUser user, ScmRole role, ScmPrivilegeType privileges ) {
        try {
            ScmUserModifier modifier = new ScmUserModifier();
            ScmFactory.Role.grantPrivilege( sessionA, role, rs, privileges );
            modifier.addRole( role );
            ScmFactory.User.alterUser( sessionA, user, modifier );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            ScmFactory.Role.revokePrivilege( sessionA, role, rs,
                    ScmPrivilegeType.UPDATE );
            ScmFactory.Role.revokePrivilege( sessionA, role, rs,
                    ScmPrivilegeType.READ );
            ScmFactory.Role.deleteRole( sessionA, role );
            ScmFactory.User.deleteUser( sessionA, user );
            for ( ScmId fileId : fileIdList ) {
                ScmFactory.File.deleteInstance( wsM, fileId, true );
            }
            for ( ScmId taskId : taskIdList ) {
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionM != null ) {
                sessionM.close();
            }
        }
    }

    private void waitTaskStop( ScmId taskId ) throws ScmException {
        Date stopTime = null;
        while ( stopTime == null ) {
            stopTime = ScmSystem.Task.getTask( sessionA, taskId ).getStopTime();
        }
    }

    private List< ScmId > prepareCleanFile( ScmWorkspace ws, String author,
            int fileNum ) throws Exception {
        ScmId fileId = null;
        List< ScmId > fileIdList = new ArrayList< ScmId >();
        try {
            for ( int i = 0; i < fileNum; i++ ) {
                fileId = createFile( ws, author );
                readFile( ws, fileId );
                fileIdList.add( fileId );
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        return fileIdList;
    }

    private List< ScmId > prepareTransferFile( ScmWorkspace ws, String author,
            int fileNum ) {
        List< ScmId > fileIdList = new ArrayList< ScmId >();
        try {
            for ( int i = 0; i < fileNum; i++ ) {
                ScmId fileId = createFile( ws, author );
                fileIdList.add( fileId );
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        return fileIdList;
    }

    private ScmId createFile( ScmWorkspace ws, String author )
            throws ScmException {
        // create file
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( author + "_" + UUID.randomUUID() );
        file.setAuthor( author );
        file.setContent( filePath );
        ScmId fileId = file.save();
        fileIdList.add( fileId );
        return fileId;
    }

    private void readFile( ScmWorkspace ws, ScmId fileId ) throws Exception {
        // read file
        ScmFile file = ScmFactory.File.getInstance( wsA, fileId );
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        file.getContent( downloadPath );
    }

    private void cleanEnv() {
        try {
            ScmFactory.Role.deleteRole( sessionA, rolename );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        try {
            ScmFactory.User.deleteUser( sessionA, username );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    private void prepare() throws Exception {
        try {
            user = ScmFactory.User.createUser( sessionA, username,
                    ScmUserPasswordType.LOCAL, passwd );
            role = ScmFactory.Role.createRole( sessionA, rolename, null );

            rs = ScmResourceFactory.createWorkspaceResource( wsp.getName() );
            grantPriAndAttachRole( sessionA, rs, user, role,
                    ScmPrivilegeType.UPDATE );
            grantPriAndAttachRole( sessionA, rs, user, role,
                    ScmPrivilegeType.READ );

            ScmAuthUtils.checkPriority( site, username, passwd, role, wsp );
            sessionUR = ScmSessionUtils.createSession( site, username, passwd );
            wsUR = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    sessionUR );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }
}
