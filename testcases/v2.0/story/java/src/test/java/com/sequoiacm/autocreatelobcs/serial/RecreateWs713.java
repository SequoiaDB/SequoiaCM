package com.sequoiacm.autocreatelobcs.serial;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.common.ScmType.ServerScope;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.Ssh;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-713 : 重建同名workspace并进行操作 SCM-313 : 写文件时lob集合空间不存在 SCM-314 :
 *           写文件时lob集合不存在
 * @Author linsuqiang
 * @Date 2017-08-01
 * @Version 1.00
 */

/*
 * 1、创建ws，并在ws写文件； 2、删除该ws，重新建立同名ws； 3、在该ws上写文件； 4、检查结果；
 * 
 * 备注： 1、713为问题单SEQUOIACM-63的补充用例 2、创建新的workspace时本即没有lob的cs、cl，因此覆盖313、314
 */

public class RecreateWs713 extends TestScmBase {
    private String author = "RecreateWs713";
    private SiteWrapper rootSite = null;
    private String wsName1 = "ws1_RecreateWs713";
    private WsWrapper wsp = null;
    private List< ScmId > fileIdList = new CopyOnWriteArrayList< ScmId >();
    private int fileSize = 1024 * 1024 * 1;
    private File localPath = null;
    private String filePath = null;
    private boolean runSuccess = false;
    private ScmSession session = null;
    private List< SiteWrapper > siteList = new ArrayList< SiteWrapper >();
    private List< String > domainNameList = new ArrayList< String >();

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );
            rootSite = ScmInfo.getRootSite();
            siteList = ScmInfo.getAllSites();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( rootSite );
            TestSdbTools.Workspace.delete( wsName1, session );
            domainNameList.add( "metaDomain1" );
            domainNameList.add( "dataDomain1" );
            domainNameList.add( "dataDomain2" );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        createws( wsName1 );
        List< BSONObject > infoList = ScmSystem.Configuration
                .reloadBizConf( ServerScope.ALL_SITE,
                        rootSite.getSiteId(), session );
        System.out.println( "infoList after reloadbizconf: \n" + infoList );
        write( session, wsName1 );
        TestSdbTools.Workspace.delete( wsName1, session );
        createws( wsName1 );
        List< BSONObject > infoList1 = ScmSystem.Configuration
                .reloadBizConf( ServerScope.ALL_SITE,
                        rootSite.getSiteId(), session );
        System.out.println( "infoList after reloadbizconf: \n" + infoList1 );
        write( session, wsName1 );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            TestSdbTools.Workspace.delete( wsName1, session );
            for ( SiteWrapper site : siteList ) {
                if ( site.getDataType().equals( DatasourceType.SEQUOIADB ) ) {
                    dropAllDomain( site, domainNameList, false );
                }
            }
            dropAllDomain( rootSite, domainNameList.subList( 0, 1 ), true );
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void write( ScmSession session, String wsName ) throws Exception {

        ScmWorkspace ws = ScmFactory.Workspace
                .getWorkspace( wsp.getName(), session );
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( author + "_" + UUID.randomUUID() );
        ScmId fileId = file.save();
        fileIdList.add( fileId );
    }

    private void createws( String wsName ) throws Exception {
        ScmSession session = null;
        String metaStr1 = "{site:\'" + rootSite.getSiteName() + "\',domain:\'"
                + createDomain( rootSite, domainNameList.get( 0 ), true ) +
                "\'}";
        String dataStr1 = createDataStr( domainNameList );
        try {
            session = TestScmTools.createSession( rootSite );
            createWs( session, wsName, metaStr1, dataStr1 );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    public void createWs( ScmSession session, String wsName, String metaStr,
            String dataStr )
            throws Exception {
        Ssh ssh = null;
        try {
            ssh = new Ssh( ScmInfo.getRootSite().getNode().getHost() );

            // get scm_install_dir
            String installPath = ssh.getScmInstallDir();

            // create workspace
            String cmd =
                    installPath + "/bin/scmadmin.sh createws -n " + wsName +
                            " -m \"" + metaStr + "\" -d \""
                            + dataStr + "\" --url \"" +
                            TestScmBase.gateWayList.get( 0 ) + "/" +
                            rootSite.getSiteName().toLowerCase() +
                            "\" --user " + TestScmBase.scmUserName +
                            " --password " + TestScmBase.scmUserName;
            ssh.exec( cmd );
            String resultMsg = ssh.getStdout();
            if ( !resultMsg.contains( "success" ) ) {
                throw new Exception(
                        "Failed to create ws[" + wsName + "], msg:\n" +
                                resultMsg );
            }
        } finally {
            if ( null != ssh ) {
                ssh.disconnect();
            }
        }
    }

    private String createDataStr( List< String > domainNameList )
            throws Exception {
        String dataStr = "[";
        for ( int i = 0; i < siteList.size() - 1; i++ ) {
            if ( siteList.get( i ).getDataType()
                    .equals( DatasourceType.SEQUOIADB ) ) {
                dataStr += "{site:\'" + siteList.get( i ).getSiteName() +
                        "\',domain:\'"
                        + createDomain( siteList.get( i ),
                        domainNameList.get( i % domainNameList.size() ), false )
                        +
                        "\',data_sharding_type:{collection_space:\'year\'," +
                        "collection:\'month\'}},";

            } else {
                dataStr += "{site:\'" + siteList.get( i ).getSiteName() + "'},";
            }
        }
        SiteWrapper lastSite = siteList.get( siteList.size() - 1 );
        if ( lastSite.getDataType() == DatasourceType.SEQUOIADB ) {
            dataStr += "{site:\'" + lastSite.getSiteName() + "\',domain:\'"
                    + createDomain( lastSite, domainNameList
                    .get( siteList.size() % domainNameList.size() ), false )
                    +
                    "\',data_sharding_type:{collection_space:\'year\'," +
                    "collection:\'month\'}}]";
        } else {
            dataStr += "{site:\'" + lastSite.getSiteName() + "\'}]";
        }
        return dataStr;
    }

    private List< String > getGroupNames( Sequoiadb db ) {
        List< String > groupNameList = db.getReplicaGroupNames();
        List< String > sysGroupname = new ArrayList< String >();
        int num = groupNameList.size();
        for ( int i = 0; i < num; i++ ) {
            if ( groupNameList.get( i ).contains( "SYS" ) ) {
                sysGroupname.add( groupNameList.get( i ) );
            }
        }
        groupNameList.removeAll( sysGroupname );
        return groupNameList;
    }

    private String createDomain( SiteWrapper site, String domainName,
            boolean flag ) throws Exception {
        Sequoiadb db = null;
        try {
            if ( !flag ) {
                db = new Sequoiadb( site.getDataDsUrl(),
                        TestScmBase.sdbUserName, TestScmBase.sdbPassword );
            } else {
                db = new Sequoiadb( site.getMetaDsUrl(),
                        TestScmBase.sdbUserName, TestScmBase.sdbPassword );
            }
            if ( db.isDomainExist( domainName ) ) {
                return domainName;
            }
            List< String > groupNameList = getGroupNames( db );
            if ( groupNameList == null || groupNameList.size() == 0 ) {
                throw new Exception(
                        "db does not exist group," + groupNameList );
            }
            BSONObject obj = new BasicBSONObject();
            obj.put( "Groups", groupNameList.toArray() );
            try {
                db.createDomain( domainName, obj );
            } catch ( BaseException e ) {
                e.printStackTrace();
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        } finally {
            if ( db != null ) {
                db.close();
            }
        }
        return domainName;
    }

    private void dropAllDomain( SiteWrapper site, List< String > domainNameList,
            boolean flag ) {
        Sequoiadb db = null;
        try {
            if ( !flag ) {
                db = new Sequoiadb( site.getDataDsUrl(),
                        TestScmBase.sdbUserName, TestScmBase.sdbPassword );
            } else {
                db = new Sequoiadb( site.getMetaDsUrl(),
                        TestScmBase.sdbUserName, TestScmBase.sdbPassword );
            }
            try {
                for ( String domainName : domainNameList ) {
                    System.out.println( "domainName1 = " + domainName + " : " +
                            site.toString() );
                    db.dropDomain( domainName );
                }
            } catch ( BaseException e ) {
                if ( e.getErrorCode() != -214 ) {
                    e.printStackTrace();
                }
            }

        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( db != null ) {
                db.close();
            }
        }
    }
}
