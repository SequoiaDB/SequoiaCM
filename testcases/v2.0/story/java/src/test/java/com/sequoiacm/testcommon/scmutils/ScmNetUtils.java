package com.sequoiacm.testcommon.scmutils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bson.BSONObject;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description init.java
 * @author luweikang
 * @date 2018年7月24日
 */
public class ScmNetUtils extends TestScmBase {
    private static String SITE_ID = "site_id";

    /**
     * return the root site;
     * 
     * @return
     * @throws ScmException
     */
    public static SiteWrapper getRootSite( WsWrapper wsWrapper )
            throws ScmException {
        return ScmInfo.getRootSite();
    }

    /**
     * return the last level site;
     * 
     * @return
     * @throws ScmException
     */
    public static SiteWrapper getLastSite( WsWrapper wsWrapper )
            throws ScmException {
        ScmNetUtils net = new ScmNetUtils();
        List< SiteWrapper > siteList = net.init( wsWrapper );
        return siteList.get( siteList.size() - 1 );
    }

    /**
     * return a random site, but not include last site
     * 
     * @return
     * @throws ScmException
     */
    public static SiteWrapper getNonLastSite( WsWrapper wsWrapper )
            throws ScmException {
        ScmNetUtils net = new ScmNetUtils();
        List< SiteWrapper > siteList = net.init( wsWrapper );
        int siteNum = new Random().nextInt( siteList.size() - 1 );
        SiteWrapper site = siteList.get( siteNum );
        return site;
    }

    /**
     * return a list of two site,the level of list.get(0) < list.get(1);
     * 
     * @return
     * @throws ScmException
     */
    public static List< SiteWrapper > getCleanSites( WsWrapper wsWrapper )
            throws ScmException {
        ScmNetUtils net = new ScmNetUtils();
        List< SiteWrapper > siteList = net.init( wsWrapper );
        List< SiteWrapper > taskSiteList = new ArrayList< SiteWrapper >();
        int secondSite = new Random().nextInt( siteList.size() - 1 ) + 1;
        int firstSite = new Random().nextInt( secondSite );
        taskSiteList.add( siteList.get( firstSite ) );
        taskSiteList.add( siteList.get( secondSite ) );
        return taskSiteList;
    }

    /**
     * retuen sort an array of sites
     * 
     * @return
     * @throws ScmException
     */
    public static List< SiteWrapper > getAllSite( WsWrapper wsWrapper )
            throws ScmException {
        ScmNetUtils net = new ScmNetUtils();
        List< SiteWrapper > siteList = net.init( wsWrapper );
        return siteList;
    }

    /**
     * return a list of two site,the level is random, list.get(0) sourceSite,
     * list.get(1) targetSite use: transfer or asyncCache
     * 
     * @return
     * @throws ScmException
     */
    public static List< SiteWrapper > getRandomSites( WsWrapper wsWrapper )
            throws ScmException {
        ScmNetUtils net = new ScmNetUtils();
        List< SiteWrapper > siteList = net.init( wsWrapper );
        int source = new Random().nextInt( siteList.size() );
        int target = new Random().nextInt( siteList.size() );
        while ( source == target ) {
            target = new Random().nextInt( siteList.size() );
        }
        List< SiteWrapper > tranSiteList = new ArrayList< SiteWrapper >();
        tranSiteList.add( siteList.get( source ) );
        tranSiteList.add( siteList.get( target ) );
        return tranSiteList;
    }

    /**
     * return tow site, site adjacent use: asyncTransfer
     * 
     * @return
     * @throws ScmException
     */
    public static List< SiteWrapper > getSortSites( WsWrapper wsWrapper )
            throws ScmException {
        ScmNetUtils net = new ScmNetUtils();
        List< SiteWrapper > siteList = net.init( wsWrapper );
        int source = new Random().nextInt( siteList.size() - 1 );
        int target = source + 1;
        List< SiteWrapper > asyncSiteList = new ArrayList< SiteWrapper >();
        asyncSiteList.add( siteList.get( source ) );
        asyncSiteList.add( siteList.get( target ) );
        return asyncSiteList;
    }

    private List< SiteWrapper > init( WsWrapper wsWrapper )
            throws ScmException {
        List< BSONObject > dataLocationList = wsWrapper.getDataLocation();
        List< SiteWrapper > siteList = new ArrayList< SiteWrapper >();
        List< SiteWrapper > allSiteList = ScmInfo.getAllSites();
        for ( BSONObject dataLocation : dataLocationList ) {
            for ( SiteWrapper siteWrapper : allSiteList ) {
                if ( ( int ) dataLocation.get( SITE_ID ) == siteWrapper
                        .getSiteId() ) {
                    siteList.add( siteWrapper );
                }
            }
        }
        return siteList;
    }

}
