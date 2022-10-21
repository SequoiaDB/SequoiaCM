package com.sequoiacm.testcommon.dsutils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSftpDataLocation;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.log4j.Logger;

public class SftpUtils extends TestScmBase {
    private static SimpleDateFormat yearFm = new SimpleDateFormat( "yyyy" );
    private static SimpleDateFormat monthFm = new SimpleDateFormat( "MM" );
    private static SimpleDateFormat daythFm = new SimpleDateFormat( "dd" );

    public static String getSftpPostfix( ScmShardingType shardType ) {
        Date currTime = new Date();
        String currY = yearFm.format( currTime );
        String currM = monthFm.format( currTime );
        String currD = daythFm.format( currTime );
        String postfix = null;
        if ( shardType == ScmShardingType.NONE ) {
            postfix = "";
        } else if ( shardType == ScmShardingType.YEAR ) {
            postfix = currY;
        } else if ( shardType == ScmShardingType.QUARTER ) {
            int quarter = ( int ) Math.ceil( Double.parseDouble( currM ) / 3 );
            postfix = currY + "q" + quarter;
        } else if ( shardType == ScmShardingType.MONTH ) {
            postfix = currY + currM;
        } else if ( shardType == ScmShardingType.DAY ) {
            postfix = currY + currM + currD;
        }
        return postfix;
    }

    public static void createDirectory( String host, String dir )
            throws Exception {
        Ssh ssh = null;
        try {
            ssh = new Ssh( host );
            String cmd = "mkdir " + dir;

            // in case of time server not usable, retry in 1 min
            int times = 60;
            int intervalSec = 1;
            boolean restoreOk = false;
            Exception lastException = null;
            for ( int i = 0; i < times; ++i ) {
                try {
                    ssh.exec( cmd );
                    restoreOk = true;
                    break;
                } catch ( Exception e ) {
                    lastException = e;
                    Thread.sleep( intervalSec );
                }
            }

            if ( !restoreOk ) {
                throw lastException;
            }
        } finally {
            if ( null != ssh ) {
                ssh.disconnect();
            }
        }
    }

    public static void deleteDirectory( String host, String dir )
            throws Exception {
        Ssh ssh = null;
        try {
            ssh = new Ssh( host );
            String cmd = "rm -rf " + dir;

            // in case of time server not usable, retry in 1 min
            int times = 60;
            int intervalSec = 1;
            boolean restoreOk = false;
            Exception lastException = null;
            for ( int i = 0; i < times; ++i ) {
                try {
                    ssh.exec( cmd );
                    restoreOk = true;
                    break;
                } catch ( Exception e ) {
                    lastException = e;
                    Thread.sleep( intervalSec );
                }
            }

            if ( !restoreOk ) {
                throw lastException;
            }
        } finally {
            if ( null != ssh ) {
                ssh.disconnect();
            }
        }
    }

    public static ScmWorkspace createWS( ScmSession session, SiteWrapper site,
            String wsName ) throws ScmException, InterruptedException {
        ScmWorkspace ws;
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        List< ScmDataLocation > scmDataLocationList = new ArrayList<>();
        String domainName = TestSdbTools
                .getDomainNames( ScmInfo.getRootSite().getDataDsUrl() )
                .get( 0 );
        scmDataLocationList.add( new ScmSdbDataLocation(
                ScmInfo.getRootSite().getSiteName(), domainName ) );
        scmDataLocationList
                .add( new ScmSftpDataLocation( site.getSiteName() ) );

        conf.setDataLocations( scmDataLocationList );
        conf.setMetaLocation(
                ScmWorkspaceUtil.getMetaLocation( ScmShardingType.YEAR ) );
        conf.setName( wsName );
        conf.setEnableDirectory( true );
        ws = ScmWorkspaceUtil.createWS( session, conf );

        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        return ws;
    }
}
