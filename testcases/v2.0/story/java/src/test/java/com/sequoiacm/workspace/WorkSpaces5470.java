package com.sequoiacm.workspace;

import java.util.ArrayList;
import java.util.List;

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
 * @descreption SCM-5070:修改数据源分区规则验证
 * @author ZhangYanan
 * @date 2022/11/29
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5470 extends TestScmBase {
    private ScmSession session = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper site = null;
    private String wsName = "ws5070";
    private ScmWorkspace rootSiteWs = null;
    private List< SiteWrapper > siteList = new ArrayList<>();
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        rootSite = ScmInfo.getRootSite();
        session = TestScmTools.createSession( rootSite );
        siteList = ScmInfo.getAllSites();
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        rootSiteWs = ScmFactory.Workspace.getWorkspace( wsName, session );
        List< ScmDataLocation > dataLocation = ScmWorkspaceUtil
                .prepareWsDataLocation( siteList, ScmShardingType.YEAR );
        rootSiteWs.updateDataLocation( dataLocation );
    }

    @Test(groups = { "fourSite", GroupTags.base })
    public void test() throws Exception {
        List< ScmDataLocation > dataLocation = ScmWorkspaceUtil
                .prepareWsDataLocation( siteList, ScmShardingType.NONE );
        rootSiteWs.updateDataLocation( dataLocation, true );

        checkWsUpdate( session, wsName );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
        } finally {
            ScmWorkspaceUtil.deleteWs( wsName, session );
            if ( session != null ) {
                session.close();
            }
        }
    }

    public void checkWsUpdate( ScmSession session, String wsName )
            throws ScmException {
        ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace( wsName,
                session );
        List< ScmDataLocation > actDataLocations = workspace.getDataLocations();
        List< ScmDataLocation > expDataLocations = prepareExpWsDataLocation();
        for ( ScmDataLocation actDataLocation : actDataLocations ) {
            for ( ScmDataLocation expDataLocation : expDataLocations ) {
                if ( actDataLocation.getSiteName()
                        .equals( expDataLocation.getSiteName() ) ) {
                    Assert.assertEquals( actDataLocation, expDataLocation );
                }
            }
        }
    }

    public List< ScmDataLocation > prepareExpWsDataLocation()
            throws ScmInvalidArgumentException {
        List< ScmDataLocation > scmDataLocationList = new ArrayList<>();
        for ( SiteWrapper site : siteList ) {
            String dataType = site.getDataType().toString();
            String siteName = site.getSiteName();
            switch ( dataType ) {
            case "sequoiadb":
                String domainName = TestSdbTools
                        .getDomainNames( site.getDataDsUrl() ).get( 0 );
                ScmSdbDataLocation scmSdbDataLocation = new ScmSdbDataLocation(
                        siteName, domainName );
                scmSdbDataLocation.setCsShardingType( ScmShardingType.NONE );
                scmSdbDataLocation.setClShardingType( ScmShardingType.YEAR );
                scmDataLocationList.add( scmSdbDataLocation );
                break;
            case "hbase":
                ScmHbaseDataLocation scmHbaseDataLocation = new ScmHbaseDataLocation(
                        siteName );
                scmHbaseDataLocation.setShardingType( ScmShardingType.NONE );
                scmDataLocationList.add( scmHbaseDataLocation );
                break;
            case "hdfs":
                ScmHdfsDataLocation scmHdfsDataLocation = new ScmHdfsDataLocation(
                        siteName );
                scmHdfsDataLocation.setShardingType( ScmShardingType.NONE );
                scmDataLocationList.add( scmHdfsDataLocation );
                break;
            case "ceph_s3":
                ScmCephS3DataLocation scmCephS3DataLocation = new ScmCephS3DataLocation(
                        siteName );
                scmCephS3DataLocation.setShardingType( ScmShardingType.NONE );
                scmDataLocationList.add( scmCephS3DataLocation );
                break;
            case "ceph_swift":
                ScmCephSwiftDataLocation scmCephSwiftDataLocation = new ScmCephSwiftDataLocation(
                        siteName );
                scmCephSwiftDataLocation
                        .setShardingType( ScmShardingType.NONE );
                scmDataLocationList.add( scmCephSwiftDataLocation );
                break;
            case "sftp":
                ScmSftpDataLocation scmSftpDataLocation = new ScmSftpDataLocation(
                        siteName );
                scmSftpDataLocation.setShardingType( ScmShardingType.NONE );
                scmDataLocationList.add( scmSftpDataLocation );
                break;
            default:
                Assert.fail( "dataSourceType not match: " + dataType );
            }
        }
        return scmDataLocationList;
    }
}