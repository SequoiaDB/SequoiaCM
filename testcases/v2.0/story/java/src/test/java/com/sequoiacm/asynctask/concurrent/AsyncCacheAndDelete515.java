package com.sequoiacm.asynctask.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
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
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-515: 并发在分中心缓存文件、删除文件
 * @Author linsuqiang
 * @Date 2017-06-23
 * @Version 1.00
 */

/*
 * 1、并发在分中心A异步缓存单个文件、删除文件； 2、检查执行返回结果； 3、后台异步缓存任务执行完成后检查缓存后的文件正确性；
 */

public class AsyncCacheAndDelete515 extends TestScmBase {
    private final int fileSize = 1024 * 200;
    private final int fileNum = 50;
    private boolean runSuccess = false;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();

    private File localPath = null;
    private String filePath = null;

    private ScmSession sessionM = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsM = null;
    private String fileName = "AsyncCacheAndDelete515";

    private SiteWrapper rootSite = null;
    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            // ready file
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            rootSite = ScmInfo.getRootSite();
            branceSite = ScmInfo.getBranchSite();
            ws_T = ScmInfo.getWs();

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.FILE_NAME ).is( fileName )
                    .get();
            ScmFileUtils.cleanFile( ws_T, cond );

            sessionM = TestScmTools.createSession( rootSite );
            sessionA = TestScmTools.createSession( branceSite );
            wsM = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionM );
            prepareFiles();
        } catch ( Exception e ) {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionM != null ) {
                sessionM.close();
            }
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        try {
            ScmWorkspace wsA = ScmFactory.Workspace
                    .getWorkspace( ws_T.getName(), sessionA );
            for ( int i = 0; i < fileNum; ++i ) {
                ScmFactory.File.asyncCache( wsA, fileIdList.get( i ) );
                Thread.sleep( 200 );
                ScmFactory.File.deleteInstance( wsA, fileIdList.get( i ),
                        true );
            }

            checkResult();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException e ) {
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

    private void prepareFiles() throws Exception {
        for ( int i = 0; i < fileNum; ++i ) {
            ScmFile scmfile = ScmFactory.File.createInstance( wsM );
            scmfile.setContent( filePath );
            scmfile.setFileName( fileName + "_" + UUID.randomUUID() );
            fileIdList.add( scmfile.save() );
        }
    }

    private void checkResult() {
        try {
            for ( ScmId fileId : fileIdList ) {
                BSONObject cond = new BasicBSONObject( "id", fileId.get() );
                long cnt = ScmFactory.File.countInstance( wsM,
                        ScopeType.SCOPE_CURRENT, cond );
                Assert.assertEquals( cnt, 0 );
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }
}