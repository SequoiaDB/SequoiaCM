package com.sequoiacm.testcommon.dsutils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.log4j.Logger;
import org.bson.BSONObject;

import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.WsWrapper;

public class HdfsUtils extends TestScmBase {
    private static final Logger logger = Logger.getLogger( HdfsUtils.class );
    private static SimpleDateFormat yearFm = new SimpleDateFormat( "yyyy" );
    private static SimpleDateFormat monthFm = new SimpleDateFormat( "MM" );
    private static String urlKey = "fs.defaultFS";

    public static FileSystem getFs( SiteWrapper site ) {
        Configuration conf = new Configuration();
        conf.addResource( "core-site.xml" );
        conf.addResource( "hdfs-site.xml" );
        FileSystem fs = null;
        try {
            fs = FileSystem.get( new URI( conf.get( urlKey ) ), conf,
                    site.getDataUser() );
        } catch ( IOException | InterruptedException | URISyntaxException e ) {
            e.printStackTrace();
            logger.error( "get fileSystem fail,MSG = " + e.getMessage() );
        }
        return fs;
    }

    public static void download( SiteWrapper site, WsWrapper ws, ScmId fileId,
            String filePath ) throws IOException {
        FileSystem fs = null;
        try {
            fs = getFs( site );
            String path = getPath( site, ws ) + "/" + fileId.get();
            Path srcPath = new Path( path );
            if ( fs.exists( srcPath ) ) {
                FSDataInputStream in = fs.open( srcPath );
                FileOutputStream out;
                out = new FileOutputStream( filePath );
                IOUtils.copyBytes( in, out, 4096, true );
                logger.info( "srcPath " + srcPath.toString() + " success" );
            } else {
                logger.error( "the record does not exist,path = "
                        + srcPath.toString() );
            }
        } catch ( FileNotFoundException e ) {
            e.printStackTrace();
        } finally {
            if ( fs != null ) {
                fs.close();
            }
        }
    }

    public static void upload( SiteWrapper site, WsWrapper ws, ScmId fileId,
            String filePath ) throws IOException {
        FileSystem fs = null;
        Path destdir = null;
        Path destPath = null;
        try {
            fs = getFs( site );
            String path = getPath( site, ws );
            destdir = new Path( path );
            if ( !fs.exists( destdir ) ) {
                mkdir( destdir, site );
            }
            destPath = new Path( path + "/" + fileId.get() );
            FSDataOutputStream out = fs.create( destPath, true );
            FileInputStream in = new FileInputStream( filePath );
            IOUtils.copyBytes( in, out, 4096, true );
            logger.info( "upload " + destPath.toString() + " success" );
        } catch ( IOException e ) {
            e.printStackTrace();
            logger.error( "mkdir fail,MSG = " + destPath.toString() );
        } finally {
            if ( fs != null ) {
                fs.close();
            }
        }
    }

    public static void mkdir( Path path, SiteWrapper site ) throws IOException {
        FileSystem fs = null;
        boolean mkdirsFlag = false;
        try {
            fs = getFs( site );
            mkdirsFlag = fs.mkdirs( path );
            if ( !mkdirsFlag ) {
                logger.error( "mkdir fail,MSG = " + path.toString() );
            }
        } catch ( IOException e ) {
            e.printStackTrace();
            logger.error( "mkdir fail,MSG = " + path.toString() );
        } finally {
            if ( fs != null ) {
                fs.close();
            }
        }
    }

    public static void deletePath( SiteWrapper site, String path )
            throws IOException {
        FileSystem fs = getFs( site );
        boolean deleteFlag = false;
        try {
            deleteFlag = fs.deleteOnExit( new Path( path ) );
            if ( !deleteFlag ) {
                logger.error( "delete path fail,MSG = " + path );
            }
        } catch ( IllegalArgumentException | IOException e ) {
            logger.error( "delete path fail,MSG = " + path );
            e.printStackTrace();
        } finally {
            if ( fs != null ) {
                fs.close();
            }
        }
    }

    public static void delete( SiteWrapper site, WsWrapper ws, ScmId fileId )
            throws IOException {
        String path = null;
        try {
            path = getPath( site, ws ) + "/" + fileId.get();
            deletePath( site, path );
        } catch ( IOException e ) {
            e.printStackTrace();
            logger.error( "delete fail,MSG = " + path.toString() );
        }
    }

    private static String getPath( SiteWrapper site, WsWrapper ws ) {
        // ws.getDataLocation();
        Date currTime = new Date();
        String currY = yearFm.format( currTime );
        String currM = monthFm.format( currTime );
        String shardType = ws.getDataShardingTypeForOtherDs( site.getSiteId() );
        if ( shardType == null ) {
            shardType = "month";
        }
        String path = getRootPath( site, ws ) + "/" + ws.getName();
        if ( shardType.equals( "none" ) ) {
            return path;
        } else if ( shardType.equals( "year" ) ) {
            path = path + "/" + currY;
        } else if ( shardType.equals( "quarter" ) ) {
            int quarter = ( int ) Math.ceil( Double.parseDouble( currM ) / 3 );
            path = path + "/" + currY + "Q" + quarter;
        } else if ( shardType.equals( "month" ) ) {
            path = path + "/" + currY + currM;
        }
        return path;
    }

    public static String getRootPath( SiteWrapper site, WsWrapper ws ) {
        String rootPath = "/scm";
        List< BSONObject > dataLocation = ws.getDataLocation();
        for ( BSONObject info : dataLocation ) {
            int localSiteId = ( int ) info.get( "site_id" );
            if ( localSiteId == site.getSiteId() ) {
                if ( null == info.get( "hdfs_file_root_path" ) ) {
                    break;
                }
                rootPath = info.get( "hdfs_file_root_path" ).toString();
                break;
            }
        }
        return rootPath;
    }
}
