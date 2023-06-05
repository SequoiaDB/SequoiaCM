package com.sequoiacm.workspace.serial;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.bizconf.ScmSdbDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.common.ScmSiteCacheStrategy;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Domain;
import com.sequoiadb.base.Sequoiadb;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * @Descreption SCM-6161:创建桶，验证集合空间取消桶集合 1000 限制
 * @Author yangjianbo
 * @CreateDate 2023/5/11
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version 1.0
 */
public class UpdateDomain6161 extends TestScmBase {
    private ScmSession session = null;
    private SiteWrapper rootSite = null;
    private String wsName = "ws6161";
    private String bucketName = "bucket6161";
    private String csMeta = "_META";
    private int fileSize = 1024 * 1024;
    private String filePath = null;
    private File localPath = null;
    private ScmWorkspace ws;
    private String doMainOld = wsName + "domainold";
    private String doMainNotExit = wsName + "domainnotexit";
    private Sequoiadb sdb = null;
    private boolean runSuccess = false;
    private int maxClNum = 4096;

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
        Domain domain = sdb.getDomain( doMainOld );
        long oldCsCount = getDomainCsCount( domain );
        int num = 1002;
        for ( int i = 0; i < num; i++ ) {
            ScmFactory.Bucket.createBucket( ws, bucketName + i );
        }
        Assert.assertEquals( getDomainCsCount( domain ), oldCsCount );

        for ( int i = num; i <= maxClNum; i++ ) {
            ScmFactory.Bucket.createBucket( ws, bucketName + i );
        }

        Assert.assertEquals( getDomainCsCount( domain ), oldCsCount + 1 );
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

    private long getDomainCsCount( Domain domain ) {
        DBCursor listCSInDomain = null;
        long csCount = 0;
        try {
            listCSInDomain = domain.listCSInDomain();
            while ( listCSInDomain.hasNext() ) {
                listCSInDomain.getNext();
                csCount++;
            }
        } finally {
            if ( null != listCSInDomain ) {
                listCSInDomain.close();
            }
        }
        return csCount;
    }

    private void cleanEnv() throws Exception {
        TestTools.LocalFile.removeFile( localPath );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        TestSdbTools.dropDomain( rootSite, doMainOld );
        TestSdbTools.dropDomain( rootSite, doMainNotExit );
    }

    private void prepare()
            throws IOException, ScmException, InterruptedException {
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
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
