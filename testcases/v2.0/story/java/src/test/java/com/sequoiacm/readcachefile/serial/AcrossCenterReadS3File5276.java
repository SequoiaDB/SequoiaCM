package com.sequoiacm.readcachefile.serial;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmSiteCacheStrategy;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Descreption SCM-5276:工作区配置不缓存策略，跨站点读取文件（S3桶文件多版本测试）
 * @Author YiPan
 * @CreateDate
 * @UpdateUser
 * @UpdateDate 2022/9/19
 * @UpdateRemark
 * @Version
 */
public class AcrossCenterReadS3File5276 extends TestScmBase {
    private final String fileNameBase = "file5276_";
    private final String bucketName = "bucket5276";
    private List< ScmId > singleVersionFileIds = new ArrayList<>();
    private List< ScmId > multiVersionFileIds = new ArrayList<>();
    private ScmSession sessionM;
    private ScmWorkspace wsM;
    private SiteWrapper branchSite;
    private AmazonS3 s3ClientM;
    private AmazonS3 s3ClientA;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String updatePath = null;
    private String filePath = null;
    private final int fileNum = 10;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        sessionM = TestScmTools.createSession( ScmInfo.getRootSite() );
        wsM = ScmFactory.Workspace.getWorkspace( TestScmBase.s3WorkSpaces,
                sessionM );
        wsM.updateSiteCacheStrategy( ScmSiteCacheStrategy.NEVER );
        branchSite = ScmInfo.getBranchSite();
        prepareS3client();
        prepareLocalFile();
        S3Utils.clearBucket( s3ClientM, bucketName );
        s3ClientM.createBucket( bucketName );
        S3Utils.updateBucketVersionConfig( bucketName,
                BucketVersioningConfiguration.ENABLED );
        createS3File( s3ClientM );
    }

    @Test(groups = { "twoSite", "fourSite", GroupTags.base })
    public void test() throws Exception {
        // 从分站点读主站点单版本文件
        readCurrentFile();

        // 从分站点读主站点多版本文件
        readHistoryFile();

        SiteWrapper[] expSites = { ScmInfo.getRootSite() };
        // 校验单版本文件最新版本数据
        ScmFileUtils.checkMetaAndData( TestScmBase.s3WorkSpaces,
                singleVersionFileIds, expSites, localPath, filePath );

        // 校验多版本文件最新版本数据
        ScmFileUtils.checkMetaAndData( TestScmBase.s3WorkSpaces,
                multiVersionFileIds, expSites, localPath, updatePath );

        // 校验多版本文件历史版本数据
        ScmFileUtils.checkHistoryFileMetaAndData( TestScmBase.s3WorkSpaces,
                multiVersionFileIds, expSites, localPath, filePath, 1, 0 );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                S3Utils.clearBucket( s3ClientM, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                wsM.updateSiteCacheStrategy( ScmSiteCacheStrategy.ALWAYS );
                sessionM.close();
                s3ClientA.shutdown();
                s3ClientM.shutdown();
            }
        }
    }

    private void prepareS3client() {
        ClientConfiguration config = new ClientConfiguration();
        config.setUseExpectContinue( false );
        config.setSocketTimeout( 300000 );
        // 请求指定优先站点为分站点1
        config.addHeader( "x-scm-preferred",
                ScmInfo.getRootSite().getSiteName() );
        s3ClientM = S3Utils.buildS3Client( TestScmBase.s3AccessKeyID,
                TestScmBase.s3SecretKey, S3Utils.getS3Url(), config );
        config.addHeader( "x-scm-preferred", branchSite.getSiteName() );
        s3ClientA = S3Utils.buildS3Client( TestScmBase.s3AccessKeyID,
                TestScmBase.s3SecretKey, S3Utils.getS3Url(), config );
    }

    private void prepareLocalFile() throws IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        updatePath = localPath + File.separator + "updateFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( updatePath, fileSize / 2 );
    }

    private void createS3File( AmazonS3 s3Client ) throws ScmException {
        for ( int i = 0; i < fileNum / 2; i++ ) {
            s3Client.putObject( bucketName, fileNameBase + i,
                    new File( filePath ) );
            ScmId scmId = S3Utils.queryS3Object( wsM, fileNameBase + i );
            singleVersionFileIds.add( scmId );
        }
        for ( int i = fileNum / 2; i < fileNum; i++ ) {
            s3Client.putObject( bucketName, fileNameBase + i,
                    new File( filePath ) );
            s3Client.putObject( bucketName, fileNameBase + i,
                    new File( updatePath ) );
            multiVersionFileIds
                    .add( S3Utils.queryS3Object( wsM, fileNameBase + i ) );
        }
    }

    private void readCurrentFile() throws IOException {
        for ( int i = 0; i < fileNum / 2; i++ ) {
            String downloadPath = localPath + File.separator + fileNameBase + i
                    + "single.txt";
            S3Object object = s3ClientA.getObject( bucketName,
                    fileNameBase + i );
            S3Utils.inputStream2File( object.getObjectContent(), downloadPath );
        }
    }

    private void readHistoryFile() throws IOException {
        for ( int i = fileNum / 2; i < fileNum; i++ ) {
            String downloadPath = localPath + File.separator + fileNameBase + i
                    + "single.txt";
            GetObjectRequest request = new GetObjectRequest( bucketName,
                    fileNameBase + i, "1.0" );
            S3Object object = s3ClientA.getObject( request );
            S3Utils.inputStream2File( object.getObjectContent(), downloadPath );
        }
    }
}