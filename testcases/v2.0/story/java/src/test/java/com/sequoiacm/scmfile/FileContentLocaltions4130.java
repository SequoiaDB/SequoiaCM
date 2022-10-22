package com.sequoiacm.scmfile;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmContentLocation;
import com.sequoiacm.client.element.ScmSdbLobLocation;
import com.sequoiacm.client.element.ScmSiteInfo;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmCephS3DataLocation;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.dsutils.CephS3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @description SCM-4130:指定数据源上传文件，使用ScmFile.getContentLocaltions()接口获取文件数据源信息
 * @author ZhangYanan
 * @createDate 2022.2.18
 * @updateUser ZhangYanan
 * @updateDate 2022.2.18
 * @updateRemark
 * @version v1.0
 */
public class FileContentLocaltions4130 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024;
    private WsWrapper wsp;
    private String fileName = "file_4130";
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
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.createFile( filePath, fileSize );

        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        fileId = ScmFileUtils.create( ws, fileName, filePath );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        List< ScmContentLocation > fileContentLocationsInfo = file
                .getContentLocations();

        ScmFileUtils.checkContentLocation( fileContentLocationsInfo, site,
                fileId, ws );
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
}