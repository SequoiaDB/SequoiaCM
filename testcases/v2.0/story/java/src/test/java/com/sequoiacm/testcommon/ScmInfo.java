package com.sequoiacm.testcommon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.bson.BSONObject;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmServiceInstance;
import com.sequoiacm.client.element.ScmSiteInfo;
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;

public class ScmInfo {
    private static final Logger logger = Logger.getLogger( ScmInfo.class );
    private static final String scheServiceName = "schedule-server";
    private static final String authServiceName = "auth-server";
    private static Random random = new Random();
    private static List< SiteWrapper > siteList = null;
    private static List< NodeWrapper > nodeList = null;
    private static List< WsWrapper > wsList = null;
    private static List< ScmServiceInstance > scheServerList = null;
    private static List< ScmServiceInstance > authServerList = null;

    public static void refresh( ScmSession session ) throws ScmException {
        siteList = new ArrayList<>();
        nodeList = getNodeList();
        wsList = getWsList( session );
        scheServerList = getServiceInstances( session, scheServiceName );
        authServerList = getServiceInstances( session, authServiceName );
        // site2node
        List< ScmSiteInfo > siteInfoList = getSiteList( session );
        for ( ScmSiteInfo siteInfo : siteInfoList ) {
            SiteWrapper site = new SiteWrapper( nodeList, siteInfo );
            siteList.add( site );
        }

        // print ScmSystem's info
        logger.info( "sites info \n" + siteList );
        logger.info( "workspaces info \n" + wsList );
    }

    public static List< SiteWrapper > getAllSites() {
        return siteList;
    }

    public static List< NodeWrapper > getAllNodes() {
        return nodeList;
    }

    public static List< WsWrapper > getAllWorkspaces() {
        return wsList;
    }

    public static List< ScmServiceInstance > getScheServerList() {
        return scheServerList;
    }

    public static List< ScmServiceInstance > getAuthServerList() {
        return authServerList;
    }

    public static int getSiteNum() {
        return siteList.size();
    }

    public static int getAllNodeNum() {
        return nodeList.size();
    }

    public static int getWsNum() {
        return wsList.size();
    }

    /**
     * get a random site
     */
    public static SiteWrapper getSite() {
        SiteWrapper site = siteList.get( random.nextInt( siteList.size() ) );
        return site;
    }

    public static SiteWrapper getRootSite() {
        return siteList.get( 0 );
    }

    /**
     * get a random branch site
     */
    public static SiteWrapper getBranchSite() {
        return getBranchSites( 1 ).get( 0 );
    }

    /**
     * get the specified number of branchSites
     */
    public static List< SiteWrapper > getBranchSites( int num ) {
        List< SiteWrapper > allBranchSites = getBranchSites();

        // check parameter
        int maxBranchSiteNum = allBranchSites.size();
        if ( num > maxBranchSiteNum ) {
            throw new IllegalArgumentException(
                    "error, num > maxBranchSiteNum" );
        }

        List< SiteWrapper > branchSites = new ArrayList<>();

        // get random number branch sites
        int randNum = random.nextInt( maxBranchSiteNum );
        branchSites.add( allBranchSites.get( randNum ) );

        int addNum = randNum;
        for ( int i = 1; i < num; i++ ) {
            addNum++;
            if ( addNum < maxBranchSiteNum ) {
                branchSites.add( allBranchSites.get( addNum ) );
            } else {
                branchSites
                        .add( allBranchSites.get( addNum - maxBranchSiteNum ) );
            }
        }

        return branchSites;
    }

    private static List< SiteWrapper > getBranchSites() {
        List< SiteWrapper > branchSites = new ArrayList<>();
        for ( int i = 1; i < siteList.size(); i++ ) { // i=0 is rootSite
            branchSites.add( siteList.get( i ) );
        }
        return branchSites;
    }

    /**
     * get all site info if there is a new site to use this function, otherwise,
     * recommended to use getAllSites()
     */
    public static List< ScmSiteInfo > getSiteList( ScmSession session )
            throws ScmException {
        List< ScmSiteInfo > siteInfoList = new ArrayList<>();
        ScmCursor< ScmSiteInfo > cursor = null;
        try {
            cursor = ScmFactory.Site.listSite( session );
            while ( cursor.hasNext() ) {
                ScmSiteInfo info = cursor.getNext();
                siteInfoList.add( info );
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            throw e;
        } finally {
            if ( null != cursor ) {
                cursor.close();
            }
        }
        return sortSiteList( siteInfoList );
    }

    /**
     * sort info
     *
     * @return List<ScmSiteInfo> [rootSiteInfo, branchSiteInfo1,
     *         branchSiteInfo2, ......]
     */
    private static List< ScmSiteInfo > sortSiteList(
            List< ScmSiteInfo > siteInfoList ) {
        List< ScmSiteInfo > infoList = new ArrayList<>();
        List< ScmSiteInfo > tempList = new ArrayList<>();
        for ( ScmSiteInfo info : siteInfoList ) {
            if ( info.isRootSite() ) {
                infoList.add( info );
            } else {
                tempList.add( info );
            }
        }
        for ( ScmSiteInfo info : tempList ) {
            infoList.add( info );
        }
        return infoList;
    }

    /**
     * get all server info for all site sort: {site_id:1, port:1} if there is a
     * new node to use this function, otherwise, recommended to use
     * getAllNodes()
     */
    public static List< NodeWrapper > getNodeList() {
        Sequoiadb db = null;
        DBCursor cursor = null;
        List< BSONObject > nodeInfoList = new ArrayList<>();
        try {
            db = new Sequoiadb( TestScmBase.mainSdbUrl, TestScmBase.sdbUserName,
                    TestScmBase.sdbPassword );
            CollectionSpace csDB = db.getCollectionSpace( "SCMSYSTEM" );
            DBCollection clDB = csDB.getCollection( "CONTENTSERVER" );

            cursor = clDB.query( null, "{\"_id\" : {\"$include\" : 0} }",
                    "{\"site_id\" : 1, \"port\" : 1}", null );
            while ( cursor.hasNext() ) {
                BSONObject info = cursor.getNext();
                nodeInfoList.add( info );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            throw e;
        } finally {
            if ( null != cursor ) {
                cursor.close();
            }
            if ( null != db ) {
                db.close();
            }
        }

        List< NodeWrapper > allNodes = new ArrayList<>();
        for ( BSONObject nodeInfo : nodeInfoList ) {
            NodeWrapper node = new NodeWrapper( nodeInfo );
            allNodes.add( node );
        }

        return allNodes;
    }

    public static WsWrapper getWs() {
        return getWss( 1 ).get( 0 );
    }

    public static List< WsWrapper > getWss( int num ) {
        // check parameter
        int maxWsNum = wsList.size();
        if ( num > maxWsNum ) {
            throw new IllegalArgumentException(
                    "error, num > maxBranchSiteNum" );
        }

        List< WsWrapper > wss = new ArrayList<>();

        // get random number nodes
        int randNum = random.nextInt( maxWsNum );
        wss.add( wsList.get( randNum ) );

        int addNum = randNum;
        for ( int i = 1; i < num; i++ ) {
            addNum++;
            if ( addNum < maxWsNum ) {
                wss.add( wsList.get( addNum ) );
            } else {
                wss.add( wsList.get( addNum - maxWsNum ) );
            }
        }

        return wss;
    }

    /**
     * get all workspace if there is a new ws to use this function, otherwise,
     * recommended to use getAllWorkspaces()
     */
    public static List< WsWrapper > getWsList( ScmSession session )
            throws ScmException {
        ScmCursor< ScmWorkspaceInfo > cursor = null;
        List< ScmWorkspaceInfo > wsInfoList = new ArrayList<>();
        try {
            cursor = ScmFactory.Workspace.listWorkspace( session );
            while ( cursor.hasNext() ) {
                ScmWorkspaceInfo info = cursor.getNext();
                wsInfoList.add( info );
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            throw e;
        } finally {
            if ( null != cursor ) {
                cursor.close();
            }
        }

        List< WsWrapper > wss = new ArrayList<>();
        for ( ScmWorkspaceInfo wsInfo : wsInfoList ) {
            WsWrapper ws = new WsWrapper( wsInfo );
            wss.add( ws );
        }

        return wss;
    }

    /**
     * get all serviceInstance by servicename if there is a new ws to use this
     * function, otherwise, recommended to use getScheduleServers()
     */
    public static List< ScmServiceInstance > getServiceInstances(
            ScmSession session, String serviceName ) throws ScmException {
        return ScmSystem.ServiceCenter.getServiceInstanceList( session,
                serviceName );
    }
}
