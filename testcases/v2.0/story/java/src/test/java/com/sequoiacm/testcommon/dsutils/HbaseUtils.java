package com.sequoiacm.testcommon.dsutils;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.log4j.Logger;

import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;

public class HbaseUtils extends TestScmBase {
    private static final Logger logger = Logger.getLogger( HbaseUtils.class );

    public static Connection getConnection( SiteWrapper site )
            throws IOException {
        // get hbase's host and port
        String dsUrl = ( String ) site.getDataDsConf()
                .get( "hbase.zookeeper.quorum" );
        String[] obj = dsUrl.split( ":" ); // e.g: suse113-2:2181
        String host = obj[ 0 ];
        String port = obj[ 1 ];
        // connect hbase
        Configuration config = HBaseConfiguration.create();
        config.set( "hbase.zookeeper.quorum", host );
        config.set( "hbase.zookeeper.property.clientPort", port );
        return ConnectionFactory.createConnection( config );
    }

    public static void createNS( SiteWrapper site, String nsName )
            throws IOException {
        Admin admin = null;
        try {
            admin = getConnection( site ).getAdmin();
            if ( !isExistNS( site, nsName ) ) {
                admin.createNamespace(
                        NamespaceDescriptor.create( nsName ).build() );
            }
            logger.info( "create namespace success,ns=" + nsName );
        } finally {
            if ( admin != null ) {
                admin.close();
            }
        }
    }

    public static void deleteNS( SiteWrapper site, String nsName )
            throws IOException {
        Admin admin = null;
        try {
            admin = getConnection( site ).getAdmin();
            if ( isExistNS( site, nsName ) ) {
                admin.deleteNamespace( nsName );
            }
            logger.info( "delete namespace success,ns=" + nsName );
        } finally {
            if ( admin != null ) {
                admin.close();
            }
        }
    }

    private static boolean isExistNS( SiteWrapper site, String nsName )
            throws IOException {
        boolean isExist = false;
        Admin admin = null;
        try {
            admin = getConnection( site ).getAdmin();
            admin.getNamespaceDescriptor( nsName );
            isExist = true;
        } catch ( org.apache.hadoop.hbase.NamespaceNotFoundException e ) {
            logger.info( "msg = " + e.getMessage() );
        } finally {
            if ( admin != null ) {
                admin.close();
            }
        }
        return isExist;
    }

    public static boolean isInNS( SiteWrapper site, String nsName,
            String tablename ) throws IOException {
        boolean isIn = false;
        Admin admin = null;
        try {
            admin = getConnection( site ).getAdmin();
            HTableDescriptor[] tabledescs = admin
                    .listTableDescriptorsByNamespace( nsName );
            for ( HTableDescriptor descriptor : tabledescs ) {
                if ( descriptor.getNameAsString().equals( tablename ) ) {
                    isIn = true;
                    break;
                }
            }
        } finally {
            if ( admin != null ) {
                admin.close();
            }
        }
        return isIn;
    }
}
