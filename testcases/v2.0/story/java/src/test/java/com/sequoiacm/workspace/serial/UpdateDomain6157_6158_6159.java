package com.sequoiacm.workspace.serial;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmSdbDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.common.ScmSiteCacheStrategy;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import com.sequoiadb.base.Sequoiadb;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

/**
 * @Descreption SCM-6157:多版本上传文件，修改工作区数据域，获取新旧版本文件
 *              SCM-6158:多版本上传文件，修改工作区数据域，物理删除文件 
 *              SCM-6159:上传文件，修改工作区数据域，列取新旧 domain 下的文件
 * @Author yangjianbo
 * @CreateDate 2023/5/11
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version 1.0
 */
public class UpdateDomain6157_6158_6159 extends TestScmBase {
    private ScmSession session = null;
    private SiteWrapper rootSite = null;
    private String wsName = "ws6157";
    private String author = "Author6157";
    private String fileName = "file6157";
    private int fileSize = 1024 * 1024;
    private String filePath = null;
    private File localPath = null;
    private ScmWorkspace ws;
    private String doMainOld = wsName + "domainold";
    private String doMainNew = wsName + "domainnew";
    private String doMainNotExit = wsName + "domainnotexit";
    private String csMeta = "_META";
    private Sequoiadb sdb = null;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );
        sdb = TestSdbTools.getSdb( rootSite.getDataDsUrl() );
        cleanEnv();
        prepare();
    }

    @Test(groups = { "twoSite", "fourSite", GroupTags.base })
    public void test() throws Exception {

        ScmId fileID = upLoadFileNextCycle( fileName, 0 );
        ScmId fileID1 = upLoadFileNextCycle( fileName + 1, 0 );
        TestSdbTools.checkCsInDomain( sdb, wsName + csMeta, doMainOld );

        ws.updateMetaDomain( doMainNew );
        ScmId fileID2 = upLoadFileNextCycle( fileName + 2, 1 );
        TestSdbTools.checkCsInDomain( sdb, wsName + csMeta + "_1", doMainNew );

        ScmFile instance = ScmFactory.File.getInstance( ws, fileID );
        instance.updateContent( filePath );

        // 6157
        long size = ScmFactory.File.getInstance( ws, fileID, 2, 0 ).getSize();
        Assert.assertEquals( size, fileSize );
        size = ScmFactory.File.getInstance( ws, fileID, 1, 0 ).getSize();
        Assert.assertEquals( size, fileSize );

        // 6158
        ScmFactory.File.deleteInstance( ws, fileID, true );
        try {
            ScmFactory.File.getInstance( ws, fileID, 2, 0 );
        } catch ( ScmException ex ) {
            if ( ex.getErrorCode() != ScmError.FILE_NOT_FOUND.getErrorCode() ) {
                throw ex;
            }
        }
        
        try {
            ScmFactory.File.getInstance( ws, fileID, 1, 0 );
        } catch ( ScmException ex ) {
            if ( ex.getErrorCode() != ScmError.FILE_NOT_FOUND.getErrorCode() ) {
                throw ex;
            }
        }
        
        // 6159
        size = ScmFactory.File.getInstance( ws, fileID1 ).getSize();
        Assert.assertEquals( size, fileSize );

        size = ScmFactory.File.getInstance( ws, fileID2 ).getSize();
        Assert.assertEquals( size, fileSize );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                cleanEnv();
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
            if ( sdb != null ) {
                sdb.close();
            }
        }
    }

    private ScmId upLoadFileNextCycle( String fileName, int nextCycle )
            throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setAuthor( author );
        file.setContent( filePath );
        Date currentDate = new Date();
        Calendar instance = Calendar.getInstance();
        instance.setTime( currentDate );
        instance.add( Calendar.YEAR, nextCycle );
        file.setCreateTime( instance.getTime() );
        return file.save();
    }

    private void cleanEnv() throws Exception {
        TestTools.LocalFile.removeFile( localPath );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        TestSdbTools.dropDomain( rootSite, doMainNew );
        TestSdbTools.dropDomain( rootSite, doMainOld );
        TestSdbTools.dropDomain( rootSite, doMainNotExit );
    }

    private void prepare()
            throws IOException, ScmException, InterruptedException {
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestSdbTools.createDomain( rootSite, doMainNew );
        TestSdbTools.createDomain( rootSite, doMainOld );
        ws = ScmWorkspaceUtil.createWS( session, getWsConf() );

        ScmWorkspaceUtil.wsSetPriority( session, wsName );
    }

    public ScmWorkspaceConf getWsConf() throws ScmException {
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setName( wsName );
        conf.setMetaLocation(
                new ScmSdbMetaLocation( rootSite.getSiteName(), doMainOld ) );
        ScmSdbDataLocation dataLocation = new ScmSdbDataLocation(
                rootSite.getSiteName(), doMainOld );
        dataLocation.setCsShardingType( ScmShardingType.MONTH );
        dataLocation.setClShardingType( ScmShardingType.MONTH );

        conf.addDataLocation( dataLocation );
        conf.setEnableDirectory( false );
        conf.setSiteCacheStrategy( ScmSiteCacheStrategy.ALWAYS );
        return conf;
    }
}
