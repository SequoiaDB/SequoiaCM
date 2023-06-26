package com.sequoiacm.workspace;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;

/**
 * @descreption SCM-6390:删除包含ceph数据源工作区后重建工作区
 * @author YiPan
 * @date 2023/06/26
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces6390 extends TestScmBase {
    private ScmSession session = null;
    private SiteWrapper site = null;
    private String wsName = "ws6390";
    private String fileNameBase = "file6390_";
    private ScmWorkspace ws = null;
    private File localPath = null;
    private String filePath1 = null;
    private String filePath2 = null;
    private int fileSize6m = 1024 * 1024 * 6;
    private int fileSize100k = 1024 * 100;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSiteByType( ScmType.DatasourceType.CEPH_SWIFT );
        session = ScmSessionUtils.createSession( site );
        createLocalFile();
        ScmWorkspaceUtil.deleteWs( wsName, session );
        createWs();
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // 创建文件
        create6mFile();
        create100kFile();
        // 删除工作区
        ScmWorkspaceUtil.deleteWs( wsName, session );
        // 重建工作区
        createWs();

        // 校验db表
        checkTable();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            ScmWorkspaceUtil.deleteWs( wsName, session );
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void checkTable() {
        Sequoiadb sdb = null;
        try {
            sdb = TestSdbTools.getSdb( mainSdbUrl );
            DBCollection cl = sdb.getCollectionSpace( "SCMSYSTEM" )
                    .getCollection( "DATA_TABLE_NAME_HISTORY" );
            DBCursor query = cl.query();
            while ( query.hasNext() ) {
                if ( query.getNext().toString().contains( wsName ) ) {
                    Assert.fail( "工作区删除后重建，DATA_TABLE_NAME_HISTORY表记录未删除" );
                }
            }
        } finally {
            if ( sdb != null ) {
                sdb.close();
            }
        }
    }

    private void create6mFile() throws ScmException {
        for ( int i = 0; i < 10; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + "6M" + i );
            file.setContent( filePath1 );
            Calendar cal = Calendar.getInstance();
            cal.set( Calendar.MONTH, cal.get( Calendar.MONTH ) - 1 );
            cal.getTime();
            file.setCreateTime( cal.getTime() );
            file.save();
        }
    }

    private void create100kFile() throws ScmException {
        for ( int i = 0; i < 10; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + "100k" + i );
            file.setContent( filePath2 );
            file.save();
        }
    }

    private void createLocalFile() throws IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath1 = localPath + File.separator + "localFile_" + fileSize6m
                + ".txt";
        filePath2 = localPath + File.separator + "localFile_" + fileSize100k
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath1, fileSize6m );
        TestTools.LocalFile.createFile( filePath2, fileSize100k );
    }

    private void createWs() throws ScmException, InterruptedException {
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setDataLocations(
                ScmWorkspaceUtil.getDataLocationList( ScmInfo.getSiteNum() ) );
        conf.setMetaLocation(
                ScmWorkspaceUtil.getMetaLocation( ScmShardingType.MONTH ) );
        conf.setName( wsName );
        ws = ScmWorkspaceUtil.createWS( session, conf );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
    }

}