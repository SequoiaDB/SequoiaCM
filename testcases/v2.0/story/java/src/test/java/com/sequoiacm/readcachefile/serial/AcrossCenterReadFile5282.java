package com.sequoiacm.readcachefile.serial;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.ScmSiteCacheStrategy;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Descreption SCM-5282:工作区配置尽可能缓存策略，使用seek读跨站点读文件
 * @Author YiPan
 * @CreateDate
 * @UpdateUser
 * @UpdateDate 2022/9/19
 * @UpdateRemark
 * @Version
 */
public class AcrossCenterReadFile5282 extends TestScmBase {
    private final String fileNameBase = "file5282_";
    private final String fileAuthor = "author5282";
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
    private SiteWrapper branchSite;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        sessionM = TestScmTools.createSession( ScmInfo.getRootSite() );
        wsp = ScmInfo.getWs();
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
        wsM.updateSiteCacheStrategy( ScmSiteCacheStrategy.ALWAYS );

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
        branchSite = ScmInfo.getBranchSite();
        sessionA = TestScmTools.createSession( branchSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        createFile( wsM );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // 从分站点读主站点单版本文件,指定seek读
        readCurrentFile();

        // 从分站点读主站点多版本文件,指定seek读
        readHistoryFile();

        SiteWrapper[] expSites = { ScmInfo.getRootSite(), branchSite };
        // 校验单版本文件最新版本数据，被缓存到主分两个站点
        ScmFileUtils.checkMetaAndData( wsp, singleVersionFileIds, expSites,
                localPath, filePath );

        SiteWrapper[] UnCacheExpSites = { ScmInfo.getRootSite() };
        // 校验多版本文件最新版本数据，未被缓存
        ScmFileUtils.checkMetaAndData( wsp, multiVersionFileIds,
                UnCacheExpSites, localPath, updatePath );

        // 校验多版本文件历史版本数据，被缓存到主分两个站点
        ScmFileUtils.checkHistoryFileMetaAndData( wsp.getName(),
                multiVersionFileIds, expSites, localPath, filePath, 1, 0 );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmFileUtils.cleanFile( wsp, cond );
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

    private void readCurrentFile() throws ScmException, IOException {
        for ( int i = 0; i < singleVersionFileIds.size(); i++ ) {
            String downloadPath = localPath + File.separator + fileNameBase + i
                    + "single.txt";
            ScmFile file = ScmFactory.File.getInstance( wsA,
                    singleVersionFileIds.get( i ) );
            FileOutputStream fos = new FileOutputStream( downloadPath );
            ScmInputStream in = ScmFactory.File.createInputStream(
                    ScmType.InputStreamType.SEEKABLE, file );
            try {
                in.seek( CommonDefine.SeekType.SCM_FILE_SEEK_SET, 0 );
                in.read( fos );
            } finally {
                in.close();
                fos.close();
            }
        }
    }

    private void readHistoryFile() throws ScmException, IOException {
        for ( int i = 0; i < multiVersionFileIds.size(); i++ ) {
            String downloadPath = localPath + File.separator + fileNameBase + i
                    + "multi.txt";
            ScmFile file = ScmFactory.File.getInstance( wsA,
                    multiVersionFileIds.get( i ), 1, 0 );
            FileOutputStream fos = new FileOutputStream( downloadPath );
            ScmInputStream in = ScmFactory.File.createInputStream(
                    ScmType.InputStreamType.SEEKABLE, file );
            try {
                in.seek( CommonDefine.SeekType.SCM_FILE_SEEK_SET, 0 );
                in.read( fos );
            } finally {
                in.close();
                fos.close();
            }
        }
    }
}