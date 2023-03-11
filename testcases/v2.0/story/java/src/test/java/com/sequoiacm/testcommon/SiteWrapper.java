package com.sequoiacm.testcommon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bson.BSONObject;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.element.ScmSiteInfo;
import com.sequoiacm.testresource.SkipTestException;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;

public class SiteWrapper {
    // private static final Logger logger = Logger.getLogger(Site.class);
    private Random random = new Random();

    private ScmSiteInfo siteInfo;
    private List< NodeWrapper > nodes;
    private String serviceName;

    public SiteWrapper( List< NodeWrapper > allNodeList,
            ScmSiteInfo siteInfo ) {
        this.siteInfo = siteInfo;
        nodes = this.getNodesOfSite( allNodeList, siteInfo.getId() );
    }

    public int getNodeNum() {
        return nodes.size();
    }

    public int getSiteId() {
        return siteInfo.getId();
    }

    public String getSiteServiceName() {
        this.serviceName = getSiteName().toLowerCase();
        return this.serviceName;
    }

    public void setSiteServiceName( String serviceName ) {
        this.serviceName = serviceName;
    }

    public String getSiteName() {
        return siteInfo.getName();
    }

    public DatasourceType getDataType() {
        return siteInfo.getDataType();
    }

    public String getMetaUser() {
        return siteInfo.getMetaUser();
    }

    public String getMetaPasswd() {
        String passwd = "sequoiadb";
        return passwd;
    }

    public String getDataUser() {
        return siteInfo.getDataUser();
    }

    public String getDataPasswd() {
        Sequoiadb sdb = null;
        DBCursor cursor = null;
        String conf = null;
        try {
            sdb = TestSdbTools.getSdb( TestScmBase.mainSdbUrl );
            DBCollection cl = sdb.getCollectionSpace( TestSdbTools.SCM_CS )
                    .getCollection( TestSdbTools.SCM_CL_SITE );
            String matcher = "{ \"id\" : " + this.getSiteId()
                    + ", \"data.configuration\" : {$exists:1} }";
            cursor = cl.query( matcher, null, null, null );
            while ( cursor.hasNext() ) {
                BSONObject data = ( BSONObject ) cursor.getNext().get( "data" );
                if ( data != null ) {
                    conf = ( String ) data.get( "password" );
                }
            }
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
            if ( sdb != null ) {
                sdb.close();
            }
        }
        return conf;
    }

    public String getMetaDsUrl() {
        List< String > urls = this.getMetaDsUrls();
        return urls.get( random.nextInt( urls.size() ) );
    }

    public String getDataDsUrl() {
        List< String > urls = this.getDataDsUrls();
        String dataDsUrl = urls.get( random.nextInt( urls.size() ) );
        return dataDsUrl;
    }

    public String getCephPrimaryDataDsUrl() throws Exception {
        if ( this.getDataType() != DatasourceType.CEPH_S3 ) {
            throw new SkipTestException( "必须为ceph S3站点！" );
        }
        List< String > urls = this.getDataDsUrls();
        String dataDsUrl = urls.get( 0 );
        return dataDsUrl;
    }

    public String getCephStandbyDataDsUrl() throws Exception {
        if ( this.getDataType() != DatasourceType.CEPH_S3 ) {
            throw new SkipTestException( "必须为ceph S3站点！" );
        }
        List< String > urls = this.getDataDsUrls();
        if ( urls.size() <= 1 ) {
            throw new SkipTestException( "该站点未配置备库用户" );
        }
        String dataDsUrl = urls.get( 1 );
        return dataDsUrl;
    }

    // tempoary method by db
    public BSONObject getDataDsConf() {
        Sequoiadb sdb = null;
        DBCursor cursor = null;
        BSONObject conf = null;
        try {
            sdb = TestSdbTools.getSdb( TestScmBase.mainSdbUrl );
            DBCollection cl = sdb.getCollectionSpace( TestSdbTools.SCM_CS )
                    .getCollection( TestSdbTools.SCM_CL_SITE );
            String matcher = "{ \"id\" : " + this.getSiteId()
                    + ", \"data.configuration\" : {$exists:1} }";
            cursor = cl.query( matcher, null, null, null );
            while ( cursor.hasNext() ) {
                BSONObject data = ( BSONObject ) cursor.getNext().get( "data" );
                if ( data != null ) {
                    conf = ( BSONObject ) data.get( "configuration" );
                }
            }
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
            if ( sdb != null ) {
                sdb.close();
            }
        }
        return conf;
    }

    public List< String > getMetaDsUrls() {
        if ( siteInfo.isRootSite() ) {
            return siteInfo.getMetaUrl();
        } else {
            List< String > metaUrls = new ArrayList();
            metaUrls.add( TestScmBase.mainSdbUrl );
            return metaUrls;
        }
    }

    public List< String > getDataDsUrls() {
        return siteInfo.getDataUrl();
    }

    public NodeWrapper getNode() {
        return this.getNodes( 1 ).get( 0 );
    }

    /**
     * get the specified number of nodes
     */
    public List< NodeWrapper > getNodes( int num ) {
        // check parameter
        int maxNodeNum = nodes.size();
        if ( num > maxNodeNum ) {
            throw new IllegalArgumentException(
                    "error, num > maxBranchSiteNum" );
        }

        List< NodeWrapper > nodeList = new ArrayList<>();

        // get random number nodes
        int randNum = random.nextInt( maxNodeNum );
        nodeList.add( nodes.get( randNum ) );

        int addNum = randNum;
        for ( int i = 1; i < num; i++ ) {
            addNum++;
            if ( addNum < maxNodeNum ) {
                nodeList.add( nodes.get( addNum ) );
            } else {
                nodeList.add( nodes.get( addNum - maxNodeNum ) );
            }
        }
        return nodeList;
    }

    /**
     * get all the nodes of the current site
     */
    public List< NodeWrapper > getNodes() {
        return this.getNodes( nodes.size() );
    }

    /**
     * get node info
     */
    private List< NodeWrapper > getNodesOfSite( List< NodeWrapper > nodeList,
            int siteId ) {
        List< NodeWrapper > nodesOfSite = new ArrayList<>();
        for ( NodeWrapper node : nodeList ) {
            int id = ( int ) node.getSiteId();
            if ( id == siteId ) {
                nodesOfSite.add( node );
            }
        }
        return nodesOfSite;
    }

    @Override
    public String toString() {
        return siteInfo + "\nnodes " + nodes + "\n";
    }
}
