package com.sequoiacm.version.serial;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @description SCM-1662:迁移所有版本文件
 * @author wuyan
 * @createDate 2018.06.07
 * @updateUser ZhangYanan
 * @updateDate 2021.12.09
 * @updateRemark
 * @version v1.0
 */
public class TransferAllVersionFile1662 extends TestScmBase {
    private static WsWrapper wsp = null;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmId taskId = null;
    private List< String > fileIdList = new ArrayList< String >();
    private File localPath = null;
    private int fileNum = 10;
    private BSONObject condition = null;
    private String fileName = "fileVersion1662";
    private String authorName = "transfer1662";
    private int fileSize1 = 1024 * 100;
    private int fileSize2 = 1024 * 50;
    private int fileSize3 = 1024 * 30;
    private int fileSize4 = 1024 * 5;
    private String filePath1 = null;
    private String filePath2 = null;
    private String filePath3 = null;
    private String filePath4 = null;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        // ready file
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath1 = localPath + File.separator + "localFile_" + fileSize1
                + ".txt";
        filePath2 = localPath + File.separator + "localFile_" + fileSize2
                + ".txt";
        filePath3 = localPath + File.separator + "localFile_" + fileSize3
                + ".txt";
        filePath4 = localPath + File.separator + "localFile_" + fileSize4
                + ".txt";
        TestTools.LocalFile.createFile( filePath1, fileSize1 );
        TestTools.LocalFile.createFile( filePath2, fileSize2 );
        TestTools.LocalFile.createFile( filePath3, fileSize3 );
        TestTools.LocalFile.createFile( filePath4, fileSize4 );

        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();

        sessionA = TestScmTools.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        writeAndUpdateFile( wsA );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int currentVersion = 2;
        int historyVersion = 1;
        ScopeType scopeType = ScopeType.SCOPE_ALL;
        startTransferTaskByAllVerFile( wsA, sessionA, scopeType );

        checkTransferedFileSiteAndDataInfo( wsA, currentVersion,
                historyVersion );
        checkNoTransferFileInfo( wsM, currentVersion, historyVersion );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestSdbTools.Task.deleteMeta( taskId );
                for ( String fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( wsM, new ScmId( fileId ),
                            true );
                }
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionM != null ) {
                sessionM.close();
            }
        }
    }

    private void writeAndUpdateFile( ScmWorkspace ws ) throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            String subfileName = fileName + "_" + i;
            ScmId fileId = null;
            if ( i % 2 == 0 ) {
                fileId = VersionUtils.createFileByFile( ws, subfileName,
                        filePath1, authorName );
                VersionUtils.updateContentByFile( ws, subfileName, fileId,
                        filePath2 );

            } else {
                fileId = VersionUtils.createFileByFile( ws, subfileName,
                        filePath3, authorName );
                VersionUtils.updateContentByFile( ws, subfileName, fileId,
                        filePath4 );
            }
            fileIdList.add( fileId.get() );
        }
    }

    private void startTransferTaskByAllVerFile( ScmWorkspace ws,
            ScmSession session, ScopeType scopeType ) throws Exception {
        condition = ScmQueryBuilder.start().put( ScmAttributeName.File.SIZE )
                .greaterThanEquals( fileSize2 )
                .put( ScmAttributeName.File.FILE_ID ).in( fileIdList ).get();
        taskId = ScmSystem.Task.startTransferTask( ws, condition, scopeType,
                rootSite.getSiteName() );

        // wait task finish
        ScmTaskUtils.waitTaskFinish( session, taskId );
    }

    private void checkTransferedFileSiteAndDataInfo( ScmWorkspace ws,
            int currentVersion, int historyVersion ) throws Exception {
        // check the transfered file,check the sitelist and data
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScopeType.SCOPE_ALL, condition );
        int size = 0;
        SiteWrapper[] expSiteList = { rootSite, branSite };
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            ScmId fileId = file.getFileId();
            int version = file.getMajorVersion();
            if ( version == currentVersion ) {
                // check sitelist and transfered fileContent of the
                // currentVersion file
                VersionUtils.checkSite( ws, fileId, currentVersion,
                        expSiteList );
                VersionUtils.CheckFileContentByFile( ws, fileId, currentVersion,
                        filePath2, localPath );
            } else {
                // check sitelist and transfered fileContent of the
                // historyVersion file
                VersionUtils.checkSite( ws, fileId, historyVersion,
                        expSiteList );
                VersionUtils.CheckFileContentByFile( ws, fileId, historyVersion,
                        filePath1, localPath );
            }
            size++;
        }
        cursor.close();
        int expFileNum = 10;
        Assert.assertEquals( size, expFileNum );
    }

    private void checkNoTransferFileInfo( ScmWorkspace ws, int currentVersion,
            int historyVersion ) throws ScmException {
        // check the no transfer file by all version
        BSONObject condition = ScmQueryBuilder.start()
                .put( ScmAttributeName.File.SIZE ).lessThan( fileSize2 )
                .put( ScmAttributeName.File.FILE_ID ).in( fileIdList ).get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScopeType.SCOPE_ALL, condition );
        int size = 0;
        SiteWrapper[] expSiteList = { branSite };
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            ScmId fileId = file.getFileId();
            int majorVersion = file.getMajorVersion();
            if ( majorVersion == currentVersion ) {
                VersionUtils.checkSite( ws, fileId, currentVersion,
                        expSiteList );
            } else {
                VersionUtils.checkSite( ws, fileId, historyVersion,
                        expSiteList );
            }
            size++;
        }
        cursor.close();
        int expFileNum = 10;
        Assert.assertEquals( size, expFileNum );
    }
}