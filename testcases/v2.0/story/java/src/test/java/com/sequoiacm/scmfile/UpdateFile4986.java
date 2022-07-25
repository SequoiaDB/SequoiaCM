package com.sequoiacm.scmfile;

import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmUpdateContentOption;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @descreption SCM-4986:指定不计算MD5，更新非桶内的SCM文件
 * @author YiPan
 * @date 2022/7/25
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class UpdateFile4986 extends TestScmBase {
    private String fileName = "file4986";
    private File localPath = null;
    private String updatePath = null;
    private String filePath = null;
    private String downloadPath = null;
    private int fileSize = 1024 * 10;
    private int updateSize = 1024 * 20;
    private ScmSession session;
    private ScmWorkspace ws;
    private ScmId fileId;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        updatePath = localPath + File.separator + "updateFile_" + updateSize
                + ".txt";
        downloadPath = localPath + File.separator + "downLoadFile_" + updateSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( updatePath, updateSize );

        WsWrapper wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( ScmInfo.getRootSite() );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        BSONObject query = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, query );
    }

    @Test
    private void test() throws ScmException, IOException {
        // 上传文件，指定计算md5
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setContent( filePath );
        fileId = file.save( new ScmUploadConf( false, true ) );
        String version1Md5 = file.getMd5();
        Assert.assertNotNull( version1Md5 );

        // 更新文件，指定不计算md5
        file = ScmFactory.File.getInstance( ws, fileId );
        file.updateContent( new FileInputStream( updatePath ),
                new ScmUpdateContentOption( false ) );

        // 校验更新结果
        Assert.assertNull( file.getMd5() );
        file.getContent( downloadPath );
        Assert.assertEquals( TestTools.getMD5( downloadPath ),
                TestTools.getMD5( updatePath ) );

        // 校验历史版本文件内容和MD5
        TestTools.LocalFile.removeFile( downloadPath );
        ScmFile historyFile = ScmFactory.File.getInstance( ws, fileId, 1, 0 );
        Assert.assertEquals( historyFile.getMd5(), version1Md5 );
        historyFile.getContent( downloadPath );
        Assert.assertEquals( TestTools.getMD5( downloadPath ),
                TestTools.getMD5( filePath ) );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        if ( runSuccess ) {
            try {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                session.close();
            }
        }
    }
}
