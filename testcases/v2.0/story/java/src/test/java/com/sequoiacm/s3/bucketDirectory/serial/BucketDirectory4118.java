package com.sequoiacm.s3.bucketDirectory.serial;

import java.io.File;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.bizconf.*;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.dsutils.CephS3Utils;

/**
 * @description SCM-4118:创建工作区，指定不存在的bucketName
 * @author ZhangYanan
 * @createDate 2022.2.18
 * @updateUser ZhangYanan
 * @updateDate 2022.2.18
 * @updateRemark
 * @version v1.0
 */
public class BucketDirectory4118 extends TestScmBase {
    private boolean runSuccess = false;
    private String wsName = "ws4118";
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String bucketName = "bucket4118";
    private String fileName = "file4118";
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSiteByType( ScmType.DatasourceType.CEPH_S3 );
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.createFile( filePath, fileSize );
        session = ScmSessionUtils.createSession( site );
    }

    @Test(groups = { GroupTags.twoSite, GroupTags.fourSite })
    public void test() throws Exception {
        // 指定不存在bucketName创建工作区,赋予权限
        ScmCephS3DataLocation cephS3DataLocation = new ScmCephS3DataLocation(
                site.getSiteName(), bucketName, ScmShardingType.NONE );
        ws = CephS3Utils.createWS( session, wsName, cephS3DataLocation );
        CephS3Utils.wsSetPriority( session, wsName, ScmPrivilegeType.ALL );
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName );
            file.setAuthor( fileName );
            file.setContent( filePath );
            file.save();
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.DATA_NOT_EXIST.getErrorCode() ) {
                throw e;
            }
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmFactory.Workspace.deleteWorkspace( session, ws.getName() );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}