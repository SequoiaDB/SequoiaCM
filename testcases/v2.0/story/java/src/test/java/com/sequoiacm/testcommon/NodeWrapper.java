package com.sequoiacm.testcommon;

import org.bson.BSONObject;

public class NodeWrapper {
    // private static final Logger logger = Logger.getLogger(Site.class);
    private BSONObject nodeInfo;

    public NodeWrapper( BSONObject nodeInfo ) {
        this.nodeInfo = nodeInfo;
    }

    public String getName() {
        return ( String ) nodeInfo.get( "name" );
    }

    public int getId() {
        return ( int ) nodeInfo.get( "id" );
    }

    public int getSiteId() {
        return ( int ) nodeInfo.get( "site_id" );
    }

    public String getHost() {
        return ( String ) nodeInfo.get( "host_name" );
    }

    public int getPort() {
        return ( int ) nodeInfo.get( "port" );
    }

    public String getUrl() {
        return this.getHost() + ":" + this.getPort();
    }

    public int getRestPort() {
        // get default value
        return ( int ) nodeInfo.get( "port" );
    }

    @Override
    public String toString() {
        return "" + nodeInfo;
    }

}
