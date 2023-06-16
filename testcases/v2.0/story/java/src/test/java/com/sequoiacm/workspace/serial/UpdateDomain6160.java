package com.sequoiacm.workspace.serial;

import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.bizconf.ScmSdbDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
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
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * @Descreption SCM-6160:创建桶，触发创建新的集合空间
 * @Author yangjianbo
 * @CreateDate 2023/5/11
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version 1.0
 */
public class UpdateDomain6160 extends TestScmBase {
    private ScmSession session = null;
    private SiteWrapper rootSite = null;
    private String wsName = "ws6160";
    private String fileName = "file6160";
    private String bucketName = "bucket6160";
    private String csMeta = "_META";
    private int fileSize = 1024 * 1024;
    private String filePath = null;
    private File localPath = null;
    private ScmWorkspace ws;
    private String doMainOld = wsName + "domainold";
    private String doMainNew = wsName + "domainnew";
    private String doMainNotExit = wsName + "domainnotexit";
    private String testClPrefixName = "testCl_";
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

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        CollectionSpace collectionSpace = sdb
                .getCollectionSpace( wsName + csMeta );
        long i = 0;
        while ( true ) {
            try {
                collectionSpace.createCollection( testClPrefixName + i );
            } catch ( BaseException e ) {
                if ( e.getErrorCode() != SDBError.SDB_DMS_NOSPC
                        .getErrorCode() ) {
                    throw e;
                }
                break;
            }
            i++;
        }
        ws.updateMetaDomain( doMainNew );
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName + 1 );
        ScmFile file = bucket.createFile( fileName );
        file.setContent( filePath );
        file.save();
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
