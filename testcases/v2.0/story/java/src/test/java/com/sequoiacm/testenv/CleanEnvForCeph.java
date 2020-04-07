/**
 *
 */
package com.sequoiacm.testenv;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.dsutils.CephS3Utils;
import com.sequoiacm.testcommon.dsutils.CephSwiftUtils;

/**
 * public testCase, execute before execute all testCases
 */
public class CleanEnvForCeph extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( CleanEnvForCeph.class );
    private static List< SiteWrapper > sites;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        sites = ScmInfo.getAllSites();

        boolean isCephEnv = false;
        for ( SiteWrapper site : sites ) {
            DatasourceType dsType = site.getDataType();
            if ( dsType.equals( DatasourceType.CEPH_S3 ) ||
                    dsType.equals( DatasourceType.CEPH_SWIFT ) ) {
                isCephEnv = true;
                break;
            }
        }

        if ( isCephEnv == false ) {
            throw new SkipException( "Not ceph env, skip." );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testCleanBucket() throws Exception {
        for ( SiteWrapper site : sites ) {
            DatasourceType dsType = site.getDataType();
            if ( dsType.equals( DatasourceType.CEPH_S3 ) ) {
                this.deleteAllBuckets( site );
            } else if ( dsType.equals( DatasourceType.CEPH_SWIFT ) ) {
                this.deleteAllContainers( site );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
    }

    private void deleteAllBuckets( SiteWrapper site ) throws Exception {
        AmazonS3 conn;
        String bucketName = "";
        String key = "";
        List< Bucket > buckets;
        try {
            conn = CephS3Utils.createConnect( site );
            buckets = conn.listBuckets();
            for ( int i = 0; i < buckets.size(); i++ ) {
                bucketName = buckets.get( i ).getName();
                ObjectListing objects = conn.listObjects( bucketName );
                List< S3ObjectSummary > summarys = objects.getObjectSummaries();
                for ( S3ObjectSummary summ : summarys ) {
                    key = summ.getKey();
                    conn.deleteObject( bucketName, key );
                }
                conn.deleteBucket( bucketName );
            }

            buckets = conn.listBuckets();
            if ( 0 != buckets.size() ) {
                throw new Exception(
                        "failed to delete all buckets, remain buckets = " +
                                buckets );
            }
        } catch ( Exception e ) {
            logger.error( "failed to delete all buckets, siteId = " +
                    site.getSiteId() + ", bucketName = " + bucketName
                    + ", key = " + key );
            throw e;
        }
    }

    private void deleteAllContainers( SiteWrapper site ) throws Exception {
        Account account = null;
        String containerName = null;
        String objectName = null;
        try {
            account = CephSwiftUtils.createAccount( site );

            Collection< Container > containers = account.list();
            for ( Container container : containers ) {
                containerName = container.getName();
                // System.out.println("containerName = " + containerName + ",
                // site = " + site.getSiteName());

                Collection< StoredObject > storedObjs = container.list();
                for ( StoredObject storedObj : storedObjs ) {
                    objectName = storedObj.getName();
                    storedObj.delete();
                }

                container.delete();
            }

            containers = account.list();
            if ( 0 != containers.size() ) {
                throw new Exception(
                        "failed to delete all containers, remain buckets = " +
                                containers );
            }
        } catch ( Exception e ) {
            logger.error( "failed to delete all containers, siteId = " +
                    site.getSiteId() + ", containerName = "
                    + containerName + ", objectName = " + objectName );
            throw e;
        }
    }

}
