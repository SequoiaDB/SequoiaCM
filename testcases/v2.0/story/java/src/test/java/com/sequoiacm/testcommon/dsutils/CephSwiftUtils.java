package com.sequoiacm.testcommon.dsutils;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.client.factory.AccountFactory;
import org.javaswift.joss.client.factory.AuthenticationMethod;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;

import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;

public class CephSwiftUtils extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( CephSwiftUtils.class );

    public static Account createAccount(
            SiteWrapper site ) /* throws ScmCryptoException */ {
        Account account = null;
        String siteDsUrl = site.getDataDsUrl();
        String siteDsUser = site.getDataUser();
        // String siteDsPasswd = site.getDataPasswd();
        String siteDsPasswd = TestScmBase.CEPHSwift_s3SecretKey;
        try {
            AccountConfig config = new AccountConfig();
            config.setAuthUrl( siteDsUrl );
            config.setUsername( siteDsUser );
            config.setPassword( siteDsPasswd );
            config.setAuthenticationMethod( AuthenticationMethod.BASIC );

            AccountFactory service = new AccountFactory( config );
            account = service.createAccount();
        } catch ( Exception e ) {
            logger.error( "failed to connect ceph-swift, siteId = "
                    + site.getSiteId() );
            throw e;
        }
        return account;
    }

    public static void createObject( SiteWrapper site, WsWrapper ws,
            ScmId fileId, String filePath ) throws Exception {
        Account account = null;
        String objectName = fileId.get();
        try {
            account = createAccount( site );
            String containerName = getContainerName( site, ws );
            Container container = account.getContainer( containerName );
            if ( !container.exists() ) {
                container.create();
            }
            StoredObject storeObject = container.getObject( objectName );
            byte[] data = TestTools.getBuffer( filePath );
            storeObject.uploadObject( data );
        } catch ( Exception e ) {
            logger.error( "failed to write file in ceph-swift, fileId = "
                    + objectName );
            throw e;
        }
    }

    public static StoredObject getObjectSegment( SiteWrapper site, WsWrapper ws,
            ScmId fileId, int part ) throws Exception {
        Account account = null;
        StoredObject objSegment = null;
        String objectName = fileId.get();
        try {
            account = createAccount( site );
            String containerName = getContainerName( site, ws );
            Container container = account.getContainer( containerName );

            objSegment = container.getObjectSegment( objectName, part );
        } catch ( Exception e ) {
            logger.error(
                    "failed to get object segment in ceph-swift, fileId = "
                            + objectName + ", part = " + part );
            throw e;
        }
        return objSegment;
    }

    public static Map< String, Object > getObjMetadata( SiteWrapper site,
            WsWrapper ws, ScmId fileId, String filePath, String downloadPath )
            throws Exception {
        Account account = null;
        String objectName = fileId.get();
        Map< String, Object > metadata = null;
        long fileSize = new File( filePath ).length();
        try {
            account = createAccount( site );
            String containerName = getContainerName( site, ws );
            Container container = account.getContainer( containerName );

            StoredObject storeObject = container.getObject( objectName );
            metadata = storeObject.getMetadata();

            // check object length
            Long objSize = ( Long ) metadata.get( "File-Size" );
            if ( fileSize != objSize ) {
                throw new Exception( "objLength is error, objSize = " + objSize
                        + "fileSize = " + fileSize + ", fileId = " + fileId );
            }

            // check object content
            storeObject.downloadObject( new File( downloadPath ) );
            String expMd5 = TestTools.getMD5( filePath );
            String actMd5 = TestTools.getMD5( downloadPath );
            if ( expMd5.equals( actMd5 ) ) {
                throw new Exception( "objContent is error, actMd5 = " + actMd5
                        + "expMd5 = " + expMd5 + ", fileId = " + fileId );
            }
        } catch ( Exception e ) {
            logger.error(
                    "failed to get object in ceph-swift, fileId = " + fileId );
            throw e;
        }
        return metadata;
    }

    public static void deleteObject( SiteWrapper site, WsWrapper ws,
            ScmId fileId ) throws Exception {
        Account account = null;
        String objectName = fileId.get();
        try {
            account = createAccount( site );
            String containerName = getContainerName( site, ws );
            Container container = account.getContainer( containerName );

            StoredObject storeObject = container.getObject( objectName );
            String countStr = ( String ) storeObject
                    .getMetadata( "Segment-Count" );

            storeObject.delete();
            if ( countStr != null ) {
                int count = Integer.valueOf( countStr );
                for ( int i = 1; i <= count; i++ ) {
                    StoredObject storeObj = container
                            .getObjectSegment( objectName, i );
                    storeObj.delete();
                }
            }
        } catch ( Exception e ) {
            logger.error( "failed to delete object in ceph-swift, fileId = "
                    + fileId );
            throw e;
        }
    }

    public static void deleteContainer( SiteWrapper site, String wsName )
            throws Exception {
        Account account = null;
        String containerName = null;
        String objectName = null;
        try {
            account = createAccount( site );
            containerName = getContainerName( site, wsName );

            Container container = account.getContainer( containerName );
            if ( container.exists() ) {
                Collection< StoredObject > storedObjs = container.list();
                for ( StoredObject storedObj : storedObjs ) {
                    objectName = storedObj.getName();
                    storedObj.delete();
                }

                container.delete();
            }
        } catch ( Exception e ) {
            logger.error( "failed to delete all containers, siteId = "
                    + site.getSiteId() + ", containerName = " + containerName
                    + ", objectName = " + objectName );
            throw e;
        }
    }

    private static String getContainerName( SiteWrapper site, WsWrapper ws )
            throws ScmException {
        String containerName = "";

        // get bucketName prefix
        String containerPrefix = ws.getContainerPrefix( site.getSiteId() );
        if ( null == containerPrefix ) {
            containerPrefix = ws.getName() + "_scmfile";
        }

        // get bucketName postFix
        String dstType = TestSdbTools.getDataShardingTypeForOtherDs(
                site.getSiteId(), ws.getName() );
        if ( null == dstType ) {
            dstType = "month";
        }
        String postFix = TestSdbTools.getCsClPostfix( dstType );

        // get bucketName
        if ( !dstType.equals( "none" ) ) {
            containerPrefix += "_";
        }
        containerName = containerPrefix + postFix;

        return containerName;
    }

    private static String getContainerName( SiteWrapper site, String wsName )
            throws ScmException {
        String containerName = "";

        // get bucketName prefix
        Object containerPrefix = TestSdbTools
                .getContainerPrefix( site.getSiteId(), wsName );
        if ( null == containerPrefix ) {
            containerPrefix = wsName + "_scmfile";
        } else {
            containerPrefix = containerPrefix.toString();
        }

        // get bucketName postFix
        String dstType = TestSdbTools
                .getDataShardingTypeForOtherDs( site.getSiteId(), wsName );
        if ( null == dstType ) {
            dstType = "month";
        }
        String postFix = TestSdbTools.getCsClPostfix( dstType );

        // get bucketName
        if ( !dstType.equals( "none" ) ) {
            containerPrefix += "_";
        }
        containerName = containerPrefix + postFix;

        return containerName;
    }

}
