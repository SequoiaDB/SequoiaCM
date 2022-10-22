package com.sequoiacm.scmfile.concurrent;

import java.io.File;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmContentLocation;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @description SCM-4134:更新文件时，使用ScmFile.getContentLocaltions()接口获取文件数据源信息
 * @author ZhangYanan
 * @createDate 2022.2.18
 * @updateUser ZhangYanan
 * @updateDate 2022.2.18
 * @updateRemark
 * @version v1.0
 */
public class FileContentLocaltions4134 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private File localPath = null;
    private String filePath1 = null;
    private String filePath2 = null;
    private int fileSize = 1024;
    private WsWrapper wsp;
    private boolean isDeletefilethreadSuccess = false;
    private String fileName = "file_4134";
    private ScmWorkspace ws;
    private ScmId fileId = null;
    private BSONObject queryCond;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath1 = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.createFile( filePath1, fileSize );

        filePath2 = localPath + File.separator + "localFile_" + fileSize * 2
                + ".txt";
        TestTools.LocalFile.createFile( filePath2, fileSize * 2 );

        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        fileId = ScmFileUtils.create( ws, fileName, filePath1 );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        es.addWorker( new ThreadGetContentLocations() );
        es.addWorker( new ThreadUpdateFile() );
        es.run();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class ThreadGetContentLocations extends ResultStore {
        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                List< ScmContentLocation > fileContentLocationsInfo1 = file
                        .getContentLocations();
                if ( isDeletefilethreadSuccess ) {
                    fileContentLocationsInfo1 = file.getContentLocations();
                    fileId = file.getDataId();
                }
                ScmFileUtils.checkContentLocation( fileContentLocationsInfo1, site,
                        fileId, ws );
            }finally {
                session.close();
            }
        }
    }

    private class ThreadUpdateFile extends ResultStore {
        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            file.updateContent( filePath2 );
            isDeletefilethreadSuccess = true;
        }
    }
}