package com.sequoiacm.s3.version;

import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-4787 :: 桶开启版本控制，获取文件执行非物理删除(指定历史版本v1查询文件)
 * @author wuyan
 * @Date 2022.07.12
 * @version 1.00
 */
public class ScmFile4787B extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4787b";
    private String fileName = "scmfile4787b";
    private ScmId fileId = null;
    private SiteWrapper site = null;
    private int fileSize = 1024 * 100;
    private byte[] filedata = new byte[ fileSize ];
    private int updateSize = 1024 * 20;
    private byte[] updatedata = new byte[ updateSize ];
    private ScmWorkspace ws = null;
    private ScmBucket scmBucket = null;
    private ScmSession session;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
        fileId = ScmFileUtils.createFile( scmBucket, fileName, filedata );
        ScmFileUtils.createFile( scmBucket, fileName, updatedata );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        int historyVersion = 1;
        ScmFile file = scmBucket.getFile( fileName, historyVersion, 0 );
        file.delete( false );

        // 检查删除文件移到历史版本，版本号为2
        int historyVersion1 = 2;
        ScmFile file1 = scmBucket.getFile( fileName, historyVersion1, 0 );
        S3Utils.checkFileContent( file1, updatedata );

        // 检查原历史版本文件，版本号为1
        int historyVersion2 = 1;
        ScmFile file2 = scmBucket.getFile( fileName, historyVersion2, 0 );
        S3Utils.checkFileContent( file2, filedata );

        // 新增一条删除标记版本号为3
        int deleteMarkerVersion = 3;
        checkFileVersion( deleteMarkerVersion, deleteMarkerVersion );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.clearBucket( session, bucketName );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void checkFileVersion( int deleteVersion, int expFileVersionNum )
            throws ScmException {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScmType.ScopeType.SCOPE_ALL, condition );
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            int version = file.getMajorVersion();
            if ( version == deleteVersion ) {
                Assert.assertTrue( file.isDeleteMarker() );
            } else {
                Assert.assertFalse( file.isDeleteMarker(),
                        "the file version =" + version );
            }
            size++;
        }
        cursor.close();

        Assert.assertEquals( size, expFileVersionNum );
    }
}
