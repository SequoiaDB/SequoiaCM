package com.sequoiacm.testcommon;

import java.util.List;

import org.bson.BSONObject;

import com.sequoiacm.client.element.ScmWorkspaceInfo;

public class WsWrapper {
    // private static final Logger logger = Logger.getLogger(Ws.class);
    private ScmWorkspaceInfo wsInfo;

    public WsWrapper( ScmWorkspaceInfo wsInfo ) {
        this.wsInfo = wsInfo;
    }

    public String getName() {
        return wsInfo.getName();
    }

    public List< BSONObject > getDataLocation() {
        return wsInfo.getDataLocation();
    }

    public BSONObject getDataShardingType( int siteId ) {
        BSONObject dataShardingType = null;
        List< BSONObject > dataLocation = getDataLocation();
        for ( BSONObject info : dataLocation ) {
            int localSiteId = ( int ) info.get( "site_id" );
            if ( localSiteId == siteId ) {
                dataShardingType = ( BSONObject ) info
                        .get( "data_sharding_type" );
                break;
            }
        }
        return dataShardingType;
    }

    public String getDataShardingTypeForOtherDs( int siteId ) {
        String dataShardingType = null;
        List< BSONObject > dataLocation = getDataLocation();
        for ( BSONObject info : dataLocation ) {
            int localSiteId = ( int ) info.get( "site_id" );
            if ( localSiteId == siteId ) {
                if ( null == info.get( "data_sharding_type" ) ) {
                    break;
                }
                dataShardingType = info.get( "data_sharding_type" ).toString();
                break;
            }
        }
        return dataShardingType;
    }

    public String getContainerPrefix( int siteId ) {
        String containerPrefix = "";
        List< BSONObject > dataLocation = getDataLocation();
        for ( BSONObject info : dataLocation ) {
            int localSiteId = ( int ) info.get( "site_id" );
            if ( localSiteId == siteId ) {
                containerPrefix = ( String ) info.get( "container_prefix" );
                break;
            }
        }
        return containerPrefix;
    }

    @Override
    public String toString() {
        return "" + wsInfo + "\n";
    }
}
