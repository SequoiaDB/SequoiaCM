package com.sequoiacm.asynctask;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @Descreption SCM-3744:网状默认异步单文件迁移任务，源站点为ws最后一级站点
 * @Author YiPan
 * @CreateDate 2021/9/8
 * @Version 1.0
 */
public class AsyncTransfer3744 extends TestScmBase {
    private final static int fileSize = 1024;
    private final static String filename = "file3744";
    private ScmSession branchSiteSession = null;
    private ScmWorkspace branchSiteWs = null;
    private SiteWrapper branchSite = null;
    private WsWrapper wsp = null;
    private List< ScmId > fileIds = new ArrayList<>();
    private File localPath = null;
    private String filePath = null;
    private BSONObject queryCond = null;
    private boolean runSuccess = false;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        List< SiteWrapper > sortBranchSites = ScmScheduleUtils
                .getSortBranchSites();
        // 最后一级分站点
        branchSite = sortBranchSites.get( sortBranchSites.size() - 1 );
        wsp = ScmInfo.getWs();
        branchSiteSession = ScmSessionUtils.createSession( branchSite );
        branchSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSiteSession );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( filename ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
    }

    @Test(groups = { "twoSite", "fourSite", "net", GroupTags.base })
    public void test() throws Exception {
        // 最后一级站点创建文件
        ScmId fileId = ScmFileUtils.create( branchSiteWs, filename, filePath );
        fileIds.add( fileId );

        // 创建迁移任务
        try {
            ScmFactory.File.asyncTransfer( branchSiteWs, fileId );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNSUPPORTED ) {
                throw e;
            }
        }
        SiteWrapper[] expSites = { branchSite };
        ScmTaskUtils.waitAsyncTaskFinished( branchSiteWs, fileId,
                expSites.length );
        ScmScheduleUtils.checkScmFile( branchSiteWs, fileIds, expSites );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            branchSiteSession.close();
        }
    }

}