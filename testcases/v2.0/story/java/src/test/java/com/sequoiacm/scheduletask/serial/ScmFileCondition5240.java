package com.sequoiacm.scheduletask.serial;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileCondition;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmMoveTaskConfig;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Descreption SCM-5240:任务新的匹配类型ScmFileCondtion，驱动测试
 * @Author YiPan
 * @CreateDate 2022/9/27
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class ScmFileCondition5240 extends TestScmBase {
    private String fileName = "file5240_";
    private String fileAuthor = "author5240";
    private BSONObject queryCond;
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private long now;
    private ScmSession sessionM = null;
    private WsWrapper wsp;
    private ScmWorkspace wsM;
    private List< ScmId > fileIds = new ArrayList<>();
    private int fileSize = 1024 * 100;
    private int fileNum = 10;
    private File localPath = null;
    private String filePath = null;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws IOException, ScmException {
        now = System.currentTimeMillis();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        wsp = ScmInfo.getWs();
        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        createFile( wsM );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        ScmFileCondition scmFileCondition = new ScmFileCondition();
        scmFileCondition.setFileBeginningTime( new Date( now ) );
        scmFileCondition.setFileEndingTime( new Date( now + 100 ) );
        // a.获取FileBeginningTime
        Assert.assertEquals( scmFileCondition.getFileBeginningTime(),
                new Date( now ) );
        // b.获取FileEndingTime
        Assert.assertEquals( scmFileCondition.getFileEndingTime(),
                new Date( now + 100 ) );

        // c.设置FileBeginningTime>FileEndingTime
        try {
            scmFileCondition.setFileBeginningTime( new Date( now ) );
            scmFileCondition.setFileEndingTime( new Date( now - 100 ) );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        // d.设置FileEndingTime=null
        scmFileCondition.setFileEndingTime( null );
        ScmMoveTaskConfig conf = new ScmMoveTaskConfig();
        conf.setCondition( scmFileCondition );
        conf.setTargetSite( branchSite.getSiteName() );
        conf.setWorkspace( wsM );

        ScmId taskId = ScmSystem.Task.startMoveTask( conf );
        ScmTaskUtils.waitTaskFinish( sessionM, taskId );
        SiteWrapper[] expSites = { branchSite };
        ScmFileUtils.checkMetaAndData( wsp, fileIds, expSites, localPath,
                filePath );

        // SEQUOIACM-1077补充测试点，设置起止时间相同
        scmFileCondition.setFileBeginningTime( new Date( now ) );
        scmFileCondition.setFileEndingTime( new Date( now ) );
        Assert.assertEquals( scmFileCondition.getFileBeginningTime(),
                new Date( now ) );
        Assert.assertEquals( scmFileCondition.getFileEndingTime(),
                new Date( now ) );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                TestTools.LocalFile.removeFile( localPath );
                ScmFileUtils.cleanFile( wsp, queryCond );
            } finally {
                sessionM.close();
            }
        }
    }

    private void createFile( ScmWorkspace ws ) throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName + i );
            file.setAuthor( fileAuthor );
            file.setContent( filePath );
            file.setCreateTime( new Date( now + i ) );
            fileIds.add( file.save() );
        }
    }
}