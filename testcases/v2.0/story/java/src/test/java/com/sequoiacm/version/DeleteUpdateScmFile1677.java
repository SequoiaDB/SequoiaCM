package com.sequoiacm.version;

import java.io.File;
import java.io.IOException;

import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @description SCM-1677:文件存在多个版本，执行删除
 * @author wuyan
 * @createDate 2018.06.11
 * @updateUser ZhangYanan
 * @updateDate 2021.12.06
 * @updateRemark
 * @version v1.0
 */
public class DeleteUpdateScmFile1677 extends TestScmBase {
    private static WsWrapper wsp = null;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmId fileId = null;

    private String fileName = "file1677";
    private int fileSize = 1024 * 800;
    private File localPath = null;
    private String filePath = null;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();

        sessionA = ScmSessionUtils.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionM = ScmSessionUtils.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        fileId = ScmFileUtils.create( wsA, fileName, filePath );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int updateTimes = 200;
        updateContextFile( wsA, updateTimes );
        ScmFactory.File.deleteInstance( wsA, fileId, true );
        checkDeleteResult( wsM );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionM != null ) {
                sessionM.close();
            }
        }
    }

    private void updateContextFile( ScmWorkspace ws, int times )
            throws Exception {
        for ( int i = 0; i < times; i++ ) {
            byte[] updateData = new byte[ 1024 * i + 1024 ];
            VersionUtils.updateContentByStream( ws, fileId, updateData );
            int version = i + 2;
            VersionUtils.CheckFileContentByStream( ws, fileName, version,
                    updateData );
        }
    }

    private void checkDeleteResult( ScmWorkspace ws ) throws ScmException {
        BSONObject fileCond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        long currentCount = ScmFactory.File.countInstance( ws,
                ScopeType.SCOPE_CURRENT, fileCond );
        long historyCount = ScmFactory.File.countInstance( ws,
                ScopeType.SCOPE_HISTORY, fileCond );
        long expCount = 0;
        Assert.assertEquals( currentCount, expCount,
                " the currentVersion file must be delete" );
        Assert.assertEquals( historyCount, expCount,
                " the historyVersion file must be delete" );
    }
}