package com.sequoiacm.scmfile;

import java.io.File;
import java.util.List;

import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmContentLocation;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @description SCM-4131:更新文件信息，使用ScmFile.getContentLocaltions()接口获取文件数据源信息
 * @author ZhangYanan
 * @createDate 2022.2.18
 * @updateUser ZhangYanan
 * @updateDate 2022.2.18
 * @updateRemark
 * @version v1.0
 */
public class FileContentLocaltions4131 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private File localPath = null;
    private String filePath1 = null;
    private String filePath2 = null;
    private int fileSize = 1024;
    private WsWrapper wsp;
    private String fileName = "file_4131";
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
        session = ScmSessionUtils.createSession( site );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        fileId = ScmFileUtils.create( ws, fileName, filePath1 );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );

        List< ScmContentLocation > fileContentLocationsInfo1 = file
                .getContentLocations();
        ScmFileUtils.checkContentLocation( fileContentLocationsInfo1, site,
                fileId, ws );

        // 修改文件内容
        file.updateContent( filePath2 );
        fileId = file.getDataId();

        List< ScmContentLocation > fileContentLocationsInfo2 = file
                .getContentLocations();
        ScmFileUtils.checkContentLocation( fileContentLocationsInfo2, site,
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