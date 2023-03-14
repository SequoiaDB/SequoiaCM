package com.sequoiacm.testcommon.dsutils;

import java.io.IOException;
import java.util.regex.Pattern;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.WsWrapper;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.log4j.Logger;

import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;

public class HbaseUtils extends TestScmBase {
    private static final Logger logger = Logger.getLogger( HbaseUtils.class );

    /**
     * @descreption 获取Hbase数据源连接
     * @param site
     * @return Connection
     */
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

    /**
     * @descreption Hbase数据源创建NS
     * @param site
     * @param nsName
     * @return
     */
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

    /**
     * @descreption Hbase数据源删除NS
     * @param site
     * @param nsName
     * @return
     */
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

    /**
     * @descreption Hbase数据源判断NS是否存在
     * @param site
     * @param nsName
     * @return boolean
     */
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

    /**
     * @descreption Hbase数据源判断表名是否存在于NS
     * @param site
     * @param nsName
     * @param tableName
     * @return boolean
     */
    public static boolean isInNS( SiteWrapper site, String nsName,
            String tableName ) throws IOException {
        boolean isIn = false;
        Admin admin = null;
        try {
            admin = getConnection( site ).getAdmin();
            HTableDescriptor[] tabledescs = admin
                    .listTableDescriptorsByNamespace( nsName );
            for ( HTableDescriptor descriptor : tabledescs ) {
                if ( descriptor.getNameAsString().equals( tableName ) ) {
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

    /**
     * @descreption Hbase数据源获取工作区对应表名
     * @param site
     * @param ws
     * @return boolean
     */
    public static String getDataTableNameInHbase( SiteWrapper site,
            WsWrapper ws ) throws ScmException {
        return getDataTableNameInHbase( site.getSiteId(), ws.getName() );
    }

    /**
     * @descreption Hbase数据源获取工作区对应表名
     * @param siteId
     * @param wsName
     * @return boolean
     */
    public static String getDataTableNameInHbase(int siteId, String wsName )
            throws ScmException {
        String prefix = wsName + "_SCMFILE";

        String dataShardingType = TestSdbTools.getDataShardingTypeForOtherDs( siteId,
                wsName );
        if ( null == dataShardingType ) {
            dataShardingType = "month";
        }
        String postfix = TestSdbTools.getCsClPostfix( dataShardingType );

        if ( !dataShardingType.equals( "none" ) ) {
            prefix += "_";
        }

        return prefix + postfix;
    }

    public static void deleteTableInHbase( SiteWrapper site ) throws Exception {
        Connection connect = null;
        try {
            connect = HbaseUtils.getConnection( site );
            Admin admin = connect.getAdmin();
            String regex = ".*";
            Pattern pattern = Pattern.compile( regex );
            admin.disableTables( pattern );
            admin.deleteTables( pattern );
            TableName[] tableNames = admin.listTableNames( pattern );
            if ( tableNames.length > 0 ) {
                throw new Exception( "delete table in hbase fail" );
            }
        } finally {
            if ( connect != null ) {
                connect.close();
            }
        }
    }
}
