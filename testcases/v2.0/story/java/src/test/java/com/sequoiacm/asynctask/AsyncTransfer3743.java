package com.sequoiacm.asynctask;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Descreption SCM-3743:网状结构异步单文件迁移，指定目标站点为源站点前一级站点
 * @Author YiPan
 * @CreateDate 2021/9/8
 * @Version 1.0
 */
public class AsyncTransfer3743 extends TestScmBase {
    private final static int fileSize = 1024;
    private final static String filename = "file3743";
    private SiteWrapper lastSite = null;
    private SiteWrapper frontSite = null;
    private SiteWrapper randomSite1 = null;
    private SiteWrapper randomSite2 = null;
    private WsWrapper wsp = null;
    private File localPath = null;
    private String filePath = null;
    private BSONObject queryCond = null;
    private List< SiteWrapper > sortBranchSites;
    private List< ScmSession > sessions = new ArrayList<>();
    private AtomicInteger runSuccessCount = new AtomicInteger( 0 );

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        wsp = ScmInfo.getWs();
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( filename ).get();
        sortBranchSites = ScmScheduleUtils.getSortBranchSites();
        // 最后一级站点迁移到前一级站点
        lastSite = sortBranchSites.get( sortBranchSites.size() - 1 );
        frontSite = sortBranchSites.get( sortBranchSites.size() - 2 );
        // 随机站点迁移到随机站点
        sortBranchSites.add( ScmInfo.getRootSite() );
        Collections.shuffle( sortBranchSites );
        randomSite1 = sortBranchSites.get( 0 );
        randomSite2 = sortBranchSites.get( 1 );
    }

    @DataProvider(name = "DataProvider")
    public Object[] FileSize() {
        Object[][] sites = { { lastSite, frontSite },
                { randomSite1, randomSite2 } };
        return sites;
    }

    @Test(groups = { "fourSite", "net" }, dataProvider = "DataProvider")
    public void test( SiteWrapper sourceSite, SiteWrapper targetSite )
            throws Exception {
        ScmFileUtils.cleanFile( wsp, queryCond );
        ScmSession sourceSiteSession = TestScmTools.createSession( sourceSite );
        sessions.add( sourceSiteSession );
        ScmWorkspace sourceSiteWs = ScmFactory.Workspace
                .getWorkspace( wsp.getName(), sourceSiteSession );
        // 创建文件
        ScmId fileId = ScmFileUtils.create( sourceSiteWs, filename, filePath );
        List< ScmId > fileIds = new ArrayList<>();
        fileIds.add( fileId );
        // 创建迁移任务
        ScmFactory.File.asyncTransfer( sourceSiteWs, fileId,
                targetSite.getSiteName() );
        SiteWrapper[] expSites = { sourceSite, targetSite };
        ScmTaskUtils.waitAsyncTaskFinished( sourceSiteWs, fileId,
                expSites.length );
        ScmScheduleUtils.checkScmFile( sourceSiteWs, fileIds, expSites );
        runSuccessCount.incrementAndGet();
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws ScmException {
        try {
            if ( runSuccessCount.get() == FileSize().length || forceClear ) {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            for ( ScmSession session : sessions ) {
                session.close();
            }
        }
    }

}