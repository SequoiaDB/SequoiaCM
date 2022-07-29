package com.sequoiacm.s3.version;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

import java.util.Date;

/**
 * @Description SCM-4835 :: 获取当前版本文件更新属性
 * @author wuyan
 * @Date 2022.07.14
 * @version 1.00
 */
public class ScmFile4835 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4835";
    private String fileName = "scmfile4835";
    private ScmId fileId = null;
    private ScmId batchId = null;
    private SiteWrapper site = null;
    private int fileSize = 1024 * 10;
    private byte[] filedata = new byte[ fileSize ];
    private int updateSize = 1024 * 8;
    private byte[] updatedata = new byte[ updateSize ];
    private ScmWorkspace ws = null;
    private ScmBucket scmBucket = null;
    private ScmSession session;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
        fileId = S3Utils.createFile( scmBucket, fileName, filedata, fileName );
        S3Utils.createFile( scmBucket, fileName, updatedata );
        batchId = createBatch();
    }

    @Test
    public void test() throws Exception {
        String newAuthor = "newscmfile4835";
        String newtitle = "jim4835";
        String mimeType = "text/plain";

        ScmFile file = scmBucket.getFile( fileName );
        file.setAuthor( newAuthor );
        file.setTitle( newtitle );
        file.setMimeType( mimeType );
        ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
        batch.attachFile( fileId );

        // 获取当前版本更新属性信息
        int currentVersion = 2;
        ScmFile file1 = scmBucket.getFile( fileName );
        Assert.assertEquals( file1.getAuthor(), newAuthor );
        Assert.assertEquals( file1.getTitle(), newtitle );
        Assert.assertEquals( file1.getMimeType(), mimeType );
        Assert.assertEquals( file1.getBatchId(), batchId );
        // 获取文件内容
        S3Utils.checkFileContent( file1, updatedata );

        // 获取历史版本属性信息
        int historyVersion = 1;
        ScmFile file2 = scmBucket.getFile( fileName, historyVersion, 0 );
        Assert.assertEquals( file2.getAuthor(), fileName );
        Assert.assertEquals( file2.getTitle(), "sequoiacm" );
        Assert.assertEquals( file2.getMimeType(), "" );
        Assert.assertEquals( file2.getBatchId(), batchId );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Batch.deleteInstance( ws, batchId );
                S3Utils.clearBucket( session, bucketName );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private ScmId createBatch() throws ScmException {
        String batchName = "batch4835";
        ScmBatch batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( batchName );
        batchId = batch.save();
        return batchId;
    }

}
