package com.sequoiacm.workspace.serial;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmSdbDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.common.ScmSiteCacheStrategy;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import com.sequoiadb.base.Sequoiadb;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;


/**
 * @Descreption SCM-6168:管理员用户使用驱动更新工作区元数据域
 * @Author yangjianbo
 * @CreateDate 2023/5/11
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version 1.0
 */
public class UpdateDomain6168 extends TestScmBase {
    private ScmSession session = null;
    private SiteWrapper rootSite = null;
    private String wsName = "ws6168";
    private ScmWorkspace ws;
    private String filePath = null;
    private int fileSize = 1024 * 1024;
    private String author = "Author6168";
    private String fileName = "file6168";
    private String doMainNew = wsName + "domainnew";
    private String doMainOld = wsName + "domainold";
    private String doMainNotExit = wsName + "domainnotexit";
    private String csMeta = "_META";
    private File localPath = null;
    private boolean runSuccess = false;
    private Sequoiadb sdb = null;

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

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        ws.updateMetaDomain( doMainNew );
        upLoadFileNextCycle( fileName + 1, 1 );
        TestSdbTools.checkCsInDomain( sdb, wsName + csMeta + "_1", doMainNew );
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
            throws ScmException, InterruptedException, IOException {
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestSdbTools.createDomain( rootSite, doMainNew );
        TestSdbTools.createDomain( rootSite, doMainOld );
        ws = ScmWorkspaceUtil.createWS( session, getWsConf() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName, ScmPrivilegeType.ALL );
    }

    public ScmWorkspaceConf getWsConf() throws ScmException {
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setName( wsName );
        conf.setMetaLocation(
                new ScmSdbMetaLocation( rootSite.getSiteName(), doMainOld ) );
        ScmSdbDataLocation dataLocation = new ScmSdbDataLocation(
                rootSite.getSiteName(), doMainOld );
        dataLocation.setCsShardingType( ScmShardingType.YEAR );
        dataLocation.setClShardingType( ScmShardingType.YEAR );

        conf.addDataLocation( dataLocation );
        conf.setEnableDirectory( false );
        conf.setSiteCacheStrategy( ScmSiteCacheStrategy.ALWAYS );
        conf.setBatchShardingType( ScmShardingType.YEAR );
        return conf;
    }
    
}
