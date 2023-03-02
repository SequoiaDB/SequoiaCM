package com.sequoiacm.workspace.serial;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.element.ScmContentLocation;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.bizconf.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @descreption SCM-5595:修改工作区下sequoiadb数据源配置验证
 * @author ZhangYanan
 * @date 2022/12/22
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5595 extends TestScmBase {
    private ScmSession session = null;
    private SiteWrapper rootSite = null;
    private String wsName = "ws5595";
    private String domainName = "domain5595";
    private String fileName = "file5595";
    private ScmWorkspace rootSiteWs = null;
    private List< SiteWrapper > siteList = new ArrayList<>();
    private int fileSize = 1024 * 1024;
    private String filePath = null;
    private File localPath = null;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile2_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        rootSite = ScmInfo.getRootSite();
        siteList.add( rootSite );
        session = TestScmTools.createSession( rootSite );

        ScmWorkspaceUtil.deleteWs( wsName, session );

        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        rootSiteWs = ScmFactory.Workspace.getWorkspace( wsName, session );
        // 创建预期domain
        createDomain( rootSite, domainName );
    }

    @Test(groups = { "twoSite", "fourSite", GroupTags.base })
    public void test() throws Exception {
        List< ScmDataLocation > dataLocation = prepareExpWsDataLocation();
        rootSiteWs.updateDataLocation( dataLocation, true );
        ScmWorkspaceUtil.checkWsUpdate( session, wsName, dataLocation );
        // 通过文件上传下载删除操作验证工作区是否正常
        checkWsStatus();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                deleteDomain( rootSite, domainName );
            }
        } finally {
            ScmWorkspaceUtil.deleteWs( wsName, session );
            if ( session != null ) {
                session.close();
            }
        }
    }

    public List< ScmDataLocation > prepareExpWsDataLocation()
            throws ScmInvalidArgumentException {
        List< ScmDataLocation > scmDataLocationList = new ArrayList<>();

        String siteName = rootSite.getSiteName();
        String OldDomainName = TestSdbTools
                .getDomainNames( rootSite.getDataDsUrl() ).get( 0 );
        ScmSdbDataLocation scmSdbDataLocation = new ScmSdbDataLocation(
                siteName, OldDomainName );

        BSONObject csOptions = new BasicBSONObject();
        csOptions.put( "LobPageSize", 0 );

        BSONObject clOptions = new BasicBSONObject();
        clOptions.put( "ReplSize", -1 );

        scmSdbDataLocation.setDomainName( domainName );
        scmSdbDataLocation.setCsOptions( csOptions );
        scmSdbDataLocation.setClOptions( clOptions );
        scmDataLocationList.add( scmSdbDataLocation );

        return scmDataLocationList;
    }

    public void createDomain( SiteWrapper site, String domainName ) {
        Sequoiadb sdb = null;

        try {
            sdb = TestSdbTools.getSdb( site.getDataDsUrl() );
            List< String > groupNameList = getGroupNames( sdb );
            BSONObject obj = new BasicBSONObject();
            obj.put( "Groups", groupNameList.toArray() );
            if ( sdb.isDomainExist( domainName ) ) {
                sdb.dropDomain( domainName );
                sdb.createDomain( domainName, obj );
            } else {
                sdb.createDomain( domainName, obj );
            }
        } finally {
            if ( sdb != null ) {
                sdb.close();
            }
        }
    }

    public void deleteDomain( SiteWrapper site, String domainName ) {
        Sequoiadb sdb = null;
        try {
            sdb = TestSdbTools.getSdb( site.getDataDsUrl() );
            if ( sdb.isDomainExist( domainName ) ) {
                sdb.dropDomain( domainName );
            }
        } finally {
            if ( sdb != null ) {
                sdb.close();
            }
        }
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

    public void checkWsStatus() throws Exception {
        ScmFile file = ScmFactory.File.createInstance( rootSiteWs );
        file.setContent( filePath );
        file.setFileName( fileName );
        ScmId fileID = file.save();

        SiteWrapper[] expSite = { rootSite };
        ScmFileUtils.checkMetaAndData( wsName, fileID, expSite, localPath,
                filePath );

        List<ScmContentLocation> fileContentLocationsInfo1 = file
                .getContentLocations();
        ScmFileUtils.checkContentLocation( fileContentLocationsInfo1, rootSite,
                fileID, rootSiteWs );

        file.delete( true );
        try {
            ScmFactory.File.getInstance( rootSiteWs, fileID );
            Assert.fail( "预期失败，实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.FILE_NOT_FOUND.getErrorCode() ) {
                throw e;
            }
        }

    }
}