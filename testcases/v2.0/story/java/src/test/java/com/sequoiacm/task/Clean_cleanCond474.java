package com.sequoiacm.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
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
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-474: 清理条件测试（所有属性）
 * @Author linsuqiang
 * @Date 2017-06-23
 * @Version 1.00
 */

/*
 * 1、在分中心A开始清理任务，清理条件匹配清理部分文件， 清理条件覆盖所有文件属性，且使用ScmQueryBuilder构造清理条件；
 * 2、检查执行结果正确性；
 */

public class Clean_cleanCond474 extends TestScmBase {
    private final int fileSize = 200 * 1024;
    private final int fileNum = 3;
    private final String authorName = "TestCleanCond474";
    private boolean runSuccess = false;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();

    private File localPath = null;
    private String filePath = null;

    private ScmSession sessionM = null;
    private ScmSession sessionA = null;

    private ScmId taskId = null;

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

            sessionA = TestScmTools.createSession( branceSite );
            sessionM = TestScmTools.createSession( rootSite );
            prepareFiles( sessionA );
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
            readAllFile( sessionM );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                    sessionA );
            BSONObject cond = buildCond( ws );
            taskId = ScmSystem.Task.startCleanTask( ws, cond );
            ScmTaskUtils.waitTaskFinish( sessionA, taskId );
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
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), sessionM );
                for ( int i = 0; i < fileNum; ++i ) {
                    ScmFactory.File.deleteInstance( ws, fileIdList.get( i ),
                            true );
                }
                TestSdbTools.Task.deleteMeta( taskId );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionM != null ) {
                sessionM.close();
            }
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private void prepareFiles( ScmSession session ) throws Exception {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                session );
        for ( int i = 0; i < fileNum; ++i ) {
            String str = "474_" + i;
            ScmFile scmfile = ScmFactory.File.createInstance( ws );
            scmfile.setFileName( str );
            scmfile.setAuthor( authorName );
            scmfile.setTitle( str );
            scmfile.setMimeType( str );
            scmfile.setContent( filePath );
            fileIdList.add( scmfile.save() );
        }
    }

    private void readAllFile( ScmSession session ) throws Exception {
        OutputStream fos = null;
        ScmInputStream sis = null;
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                    session );
            for ( ScmId fileId : fileIdList ) {
                ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                fos = new FileOutputStream( new File( downloadPath ) );
                sis = ScmFactory.File.createInputStream( scmfile );
                sis.read( fos );
            }
        } finally {
            if ( fos != null ) {
                fos.close();
            }
            if ( sis != null ) {
                sis.close();
            }
        }
    }

    private void checkResult() throws Exception {
        checkClean( fileIdList.get( 0 ), true );
        checkClean( fileIdList.get( 1 ), true );
        checkClean( fileIdList.get( 2 ), false );
    }

    private void checkClean( ScmId fileId, boolean isClean ) {
        List< ScmId > fileIdList = new ArrayList< ScmId >();
        fileIdList.add( fileId );
        try {
            if ( isClean ) {
                // check meta data
                SiteWrapper[] expSiteList = { rootSite };
                ScmFileUtils.checkMetaAndData( ws_T, fileIdList, expSiteList,
                        localPath, filePath );
            } else {
                // check meta data
                SiteWrapper[] expSiteList = { rootSite, branceSite };
                ScmFileUtils.checkMetaAndData( ws_T, fileIdList, expSiteList,
                        localPath, filePath );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private BSONObject buildCond( ScmWorkspace ws ) throws ScmException {
        BSONObject cond = null;
        Object[][] kvs = kvsArr( ws );
        ScmQueryBuilder builder = null;
        String bsStr = "{ \"";
        for ( Object[] kv : kvs ) {
            String key = ( String ) kv[ 0 ];
            Object value = kv[ 1 ];
            String subStr = null;
            if ( kv[ 1 ] instanceof String ) {
                subStr = key + "\" : { \"$lt\" : \"" + value + "\"}";
            } else {
                subStr = key + "\" : { \"$lt\" : " + value + "}";
            }
            if ( null == builder ) {
                builder = ScmQueryBuilder.start( key ).lessThan( value );
                bsStr = bsStr + subStr;
            } else {
                builder.put( key ).lessThan( value );
                bsStr = bsStr + " , \"" + subStr;
            }
        }
        cond = builder.and( ScmAttributeName.File.AUTHOR ).is( authorName )
                .get();
        return cond;
    }

    private Object[][] kvsArr( ScmWorkspace ws ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileIdList.get( 2 ) );
        return new Object[][] {
                new Object[] { ScmAttributeName.File.FILE_ID,
                        "ffffffffffffffffffffffff" }, // max
                // id
                // new Object[]{ScmAttributeName.File.FILE_NAME,
                // file.getFileName()},
                new Object[] { ScmAttributeName.File.FILE_NAME,
                        file.getFileName() },
                new Object[] { ScmAttributeName.File.TITLE, file.getTitle() },
                new Object[] { ScmAttributeName.File.MIME_TYPE,
                        file.getMimeType() },
                new Object[] { ScmAttributeName.File.SIZE, file.getSize() + 1 },
                new Object[] { ScmAttributeName.File.MAJOR_VERSION,
                        Integer.MAX_VALUE },
                new Object[] { ScmAttributeName.File.MINOR_VERSION,
                        Integer.MAX_VALUE },
                new Object[] { ScmAttributeName.File.USER,
                        file.getUser() + "A" },
                new Object[] { ScmAttributeName.File.CREATE_TIME,
                        file.getCreateTime().getTime() },
                new Object[] { ScmAttributeName.File.UPDATE_USER,
                        file.getUser() + "A" },
                new Object[] { ScmAttributeName.File.UPDATE_TIME,
                        file.getUpdateTime().getTime() } };
    }
}