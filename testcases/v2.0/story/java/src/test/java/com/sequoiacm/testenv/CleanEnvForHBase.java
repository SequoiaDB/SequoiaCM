package com.sequoiacm.testenv;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;

/**
 * @Description:清理hbase数据源的环境
 * @author fanyu
 * @Date:2018年2月26日
 * @version:1.0
 */
public class CleanEnvForHBase extends TestScmBase {
    Logger log = LoggerFactory.getLogger( CleanEnvForHBase.class );

    @BeforeClass(alwaysRun = true)
    private void setUp() {
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testRemainSession() throws Exception {
        List< SiteWrapper > siteList = ScmInfo.getAllSites();
        for ( SiteWrapper site : siteList ) {
            if ( site.getDataType().equals( DatasourceType.HBASE ) ) {
                String conf = ( String ) site.getDataDsConf()
                        .get( "hbase.zookeeper.quorum" );
                deleteTableInHbase( conf );
            } else {
                System.out.println( site.getSiteName()
                        + "'s datasourcetype is not hbase,it is "
                        + site.getDataType() );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
    }

    private void deleteTableInHbase( String confstr ) throws Exception {
        Configuration conf = HBaseConfiguration.create();
        conf.set( "hbase.zookeeper.quorum", confstr );
        Connection connect = null;
        try {
            connect = ConnectionFactory.createConnection( conf );
            Admin admin = connect.getAdmin();
            String regex = ".*";
            Pattern pattern = Pattern.compile( regex );
            admin.disableTables( pattern );
            admin.deleteTables( pattern );
            TableName[] tableNames = admin.listTableNames( pattern );
            if ( tableNames.length > 0 ) {
                throw new Exception( "delete table in hbase fail" );
            }
        } catch ( IOException e ) {
            log.warn( "delete table failed,msg = " + e.getMessage() );
        } finally {
            if ( connect != null ) {
                connect.close();
            }
        }
    }
}
