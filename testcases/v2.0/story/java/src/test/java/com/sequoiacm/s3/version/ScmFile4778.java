package com.sequoiacm.s3.version;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Description SCM-4778 ::更新桶状态为开启（disable->enabled），指定最新版本删除文件
 * @author wuyan
 * @Date 2022.07.12
 * @version 1.00
 */
public class ScmFile4778 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4778";
    private String fileName = "scmfile4778";
    private ScmId fileId = null;
    private SiteWrapper site = null;
    private int fileSize = 1024 * 20;
    private String filePath = null;
    private File localPath = null;
    private ScmWorkspace ws = null;
    private ScmBucket scmBucket = null;
    private ScmSession session;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        fileId = S3Utils.createFile( scmBucket, fileName, filePath );
        scmBucket.enableVersionControl();
    }

    @Test
    public void test() throws Exception {
        int currentVersion = -2;
        ScmFile file = scmBucket.getNullVersionFile(fileName);
        file.deleteVersion( currentVersion, 0 );

        try {
            scmBucket.getFile( fileName, currentVersion, 0 );
            Assert.fail( "get file with currentVersion should be fail!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), ScmError.FILE_NOT_FOUND.getErrorType(),
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }

        // 获取文件列表中无历史版本文件
        checkFileVersion();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
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

    private void checkFileVersion() throws ScmException {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScmType.ScopeType.SCOPE_ALL, condition );
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            int version = file.getMajorVersion();
            Assert.fail( "---list should be no file, the exist file version = "
                    + version );
            size++;
        }
        cursor.close();

        int expFileVersionNum = 0;
        Assert.assertEquals( size, expFileVersionNum );
    }
}
