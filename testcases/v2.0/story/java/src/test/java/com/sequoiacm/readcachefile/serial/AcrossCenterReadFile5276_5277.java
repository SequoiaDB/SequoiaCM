package com.sequoiacm.readcachefile.serial;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmSiteCacheStrategy;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @Descreption SCM-5276:工作区配置不缓存策略，跨站点读取文件（SCM文件多版本测试）
 *              SCM-5277:工作区配置不缓存策略，不同网络模型下跨站点读
 * @Author YiPan
 * @CreateDate
 * @UpdateUser
 * @UpdateDate 2022/9/19
 * @UpdateRemark
 * @Version
 */
public class AcrossCenterReadFile5276_5277 extends TestScmBase {
    private final String fileNameBase = "file5276_";
    private final String fileAuthor = "author5276";
    private List< ScmId > singleVersionFileIds = new ArrayList<>();
    private List< ScmId > multiVersionFileIds = new ArrayList<>();
    private ScmSession sessionM;
    private ScmSession sessionA;
    private ScmWorkspace wsM;
    private ScmWorkspace wsA;
    private BSONObject cond;
    private WsWrapper wsp;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String updatePath = null;
    private String filePath = null;
    private final int fileNum = 10;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        sessionM = TestScmTools.createSession( ScmInfo.getRootSite() );
        wsp = ScmInfo.getWs();
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
        wsM.updateSiteCacheStrategy( ScmSiteCacheStrategy.NEVER );

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
        sessionA = TestScmTools.createSession( ScmInfo.getBranchSite() );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        ScmFileUtils.cleanFile( wsp.getName(), cond );
        createFile( wsM );
    }

    @Test(groups = { "twoSite", "fourSite", GroupTags.base })
    public void test() throws Exception {
        // 从分站点读主站点单版本文件
        readCurrentFile();

        // 从分站点读主站点多版本文件
        readHistoryFile();

        SiteWrapper[] expSites = { ScmInfo.getRootSite() };
        // 校验单版本文件最新版本数据
        ScmFileUtils.checkMetaAndData( wsp.getName(), singleVersionFileIds,
                expSites, localPath, filePath );

        // 校验多版本文件最新版本数据
        ScmFileUtils.checkMetaAndData( wsp.getName(), multiVersionFileIds,
                expSites, localPath, updatePath );

        // 校验多版本文件历史版本数据
        ScmFileUtils.checkHistoryFileMetaAndData( wsp.getName(),
                multiVersionFileIds, expSites, localPath, filePath, 1, 0 );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmFileUtils.cleanFile( wsp.getName(), cond );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                wsM.updateSiteCacheStrategy( ScmSiteCacheStrategy.ALWAYS );
                sessionM.close();
                sessionA.close();
            }
        }
    }

    private void createFile( ScmWorkspace ws ) throws ScmException {
        for ( int i = 0; i < fileNum / 2; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setAuthor( fileAuthor );
            file.setContent( filePath );
            singleVersionFileIds.add( file.save() );
        }
        for ( int i = fileNum / 2; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setAuthor( fileAuthor );
            file.setContent( filePath );
            multiVersionFileIds.add( file.save() );
            file.updateContent( updatePath );
        }
    }

    private void readCurrentFile() throws ScmException {
        for ( int i = 0; i < singleVersionFileIds.size(); i++ ) {
            String downloadPath = localPath + File.separator + fileNameBase + i
                    + "single.txt";
            ScmFile file = ScmFactory.File.getInstance( wsA,
                    singleVersionFileIds.get( i ) );
            file.getContent( downloadPath );
        }
    }

    private void readHistoryFile() throws ScmException {
        for ( int i = 0; i < multiVersionFileIds.size(); i++ ) {
            String downloadPath = localPath + File.separator + fileNameBase + i
                    + "multi.txt";
            ScmFile file = ScmFactory.File.getInstance( wsA,
                    multiVersionFileIds.get( i ), 1, 0 );
            file.getContent( downloadPath );
        }
    }
}