package com.sequoiacm.scmfile;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-5344:ScmFactory.File.getInputStream驱动测试
 * @author YiPan
 * @date 2022/10/9
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class GetInputStream5344 extends TestScmBase {
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String version1Path = null;
    private String version2Path = null;
    private String version3path = null;
    private String fileName = "file5344";
    private WsWrapper wsp = null;
    private SiteWrapper rootSite;
    private SiteWrapper branchSite;
    private ScmSession sessionM;
    private ScmSession sessionB;
    private List< ScmId > fileIds = new ArrayList<>();
    private BSONObject query;
    private ScmWorkspace wsM;
    private ScmWorkspace wsB;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        version1Path = localPath + File.separator + "v1_" + fileSize + ".txt";
        version2Path = localPath + File.separator + "v2_" + fileSize + ".txt";
        version3path = localPath + File.separator + "v3_" + fileSize + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( version1Path, fileSize );
        TestTools.LocalFile.createFile( version2Path, fileSize );
        TestTools.LocalFile.createFile( version3path, fileSize );

        wsp = ScmInfo.getWs();
        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        sessionM = ScmSessionUtils.createSession( rootSite );
        sessionB = ScmSessionUtils.createSession( branchSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
        wsB = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionB );
        query = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, query );
        createVersionFile();
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // 获取最新版本校验md5
        InputStream inputStream = ScmFactory.File.getInputStream( wsM,
                fileIds.get( 0 ) );
        String md5 = TestTools.getMD5( inputStream );
        Assert.assertEquals( TestTools.getMD5( version3path ), md5 );
        inputStream.close();

        // 指定版本为-1获取最新版本校验md5
        inputStream = ScmFactory.File.getInputStream( wsM, fileIds.get( 0 ), -1,
                -1, CommonDefine.ReadFileFlag.SCM_READ_FILE_FORCE_NO_CACHE );
        md5 = TestTools.getMD5( inputStream );
        Assert.assertEquals( TestTools.getMD5( version3path ), md5 );
        inputStream.close();

        // 获取历史版本，跨站点读
        inputStream = ScmFactory.File.getInputStream( wsB, fileIds.get( 0 ), 1,
                0, CommonDefine.ReadFileFlag.SCM_READ_FILE_FORCE_NO_CACHE );
        md5 = TestTools.getMD5( inputStream );
        Assert.assertEquals( TestTools.getMD5( version1Path ), md5 );
        inputStream.close();

        SiteWrapper[] expSites = { rootSite };
        // 校验当前版本缓存站点
        ScmFileUtils.checkMetaAndData( wsp, fileIds, expSites, localPath,
                version3path );
        // 校验历史版本缓存站点
        ScmFileUtils.checkHistoryFileMetaAndData( wsp.getName(), fileIds,
                expSites, localPath, version1Path, 1, 0 );
        ScmFileUtils.checkHistoryFileMetaAndData( wsp.getName(), fileIds,
                expSites, localPath, version2Path, 2, 0 );

        // fileId指定为null
        try {
            ScmFactory.File.getInputStream( wsM, null );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        // ws指定为null
        try {
            ScmFactory.File.getInputStream( null, fileIds.get( 0 ), -1, -1,
                    CommonDefine.ReadFileFlag.SCM_READ_FILE_FORCE_NO_CACHE );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmFileUtils.cleanFile( wsp, query );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                sessionM.close();
                sessionB.close();
            }
        }
    }

    private void createVersionFile() throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( wsM );
        file.setFileName( fileName );
        file.setContent( version1Path );
        fileIds.add( file.save() );
        file.updateContent( version2Path );
        file.updateContent( version3path );
    }
}