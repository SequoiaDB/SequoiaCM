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
import org.apache.hadoop.hdfs.server.namenode.ha.proto.HAZKInfoProtos;
import org.apache.hadoop.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
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

    /**
     * @descreption HDFS数据源获取FileSystem
     * @param site
     * @return FileSystem
     */
    public static FileSystem getFs( SiteWrapper site )
            throws IOException, InterruptedException, KeeperException {
        String url = chooseUrl( TestScmBase.hdfsURI );
        Configuration conf = new Configuration();
        conf.set( urlKey, url );
        conf.set( "fs.hdfs.impl",
                "org.apache.hadoop.hdfs.DistributedFileSystem" );
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

    /**
     * @descreption HDFS数据源指定fileId下载对象
     * @param site
     * @param ws
     * @param fileId
     * @param filePath
     * @return
     */
    public static void download( SiteWrapper site, WsWrapper ws, ScmId fileId,
            String filePath )
            throws IOException, InterruptedException, KeeperException {
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

    /**
     * @descreption HDFS数据源指定fileId上传对象
     * @param site
     * @param ws
     * @param fileId
     * @param filePath
     * @return
     */
    public static void upload( SiteWrapper site, WsWrapper ws, ScmId fileId,
            String filePath )
            throws IOException, InterruptedException, KeeperException {
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

    /**
     * @descreption HDFS数据源创建目录
     * @param path
     * @param site
     * @return
     */
    public static void mkdir( Path path, SiteWrapper site )
            throws IOException, InterruptedException, KeeperException {
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

    /**
     * @descreption HDFS数据源删除目录
     * @param path
     * @param site
     * @return
     */
    public static void deletePath( SiteWrapper site, String path )
            throws IOException, InterruptedException, KeeperException {
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

    /**
     * @descreption HDFS数据源指定fileId删除对象
     * @param site
     * @param ws
     * @param fileId
     * @return
     */
    public static void delete( SiteWrapper site, WsWrapper ws, ScmId fileId )
            throws IOException, InterruptedException, KeeperException {
        String path = null;
        try {
            path = getPath( site, ws ) + "/" + fileId.get();
            deletePath( site, path );
        } catch ( IOException e ) {
            e.printStackTrace();
            logger.error( "delete fail,MSG = " + path.toString() );
        }
    }

    /**
     * @descreption HDFS数据源获取工作区对应Path
     * @param site
     * @param ws
     * @return String
     */
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

    /**
     * @descreption HDFS数据源获取工作区对应路径RootPath
     * @param site
     * @param ws
     * @return String
     */
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

    private static String chooseUrl( String url )
            throws IOException, InterruptedException, KeeperException {
        if ( url.contains( "hdfs:" ) ) {
            return url;
        } else {
            String activeNode = findHdfsActiveNode( url );
            return "hdfs://" + activeNode;

        }
    }

    private static ZooKeeper getZooClient( String address ) throws IOException {
        int SESSION_TIMEOUT = 60000;
        ZooKeeper zk = new ZooKeeper( address, SESSION_TIMEOUT, new Watcher() {
            @Override
            public void process( WatchedEvent event ) {
                logger.info( "event:" + event.getType() );
            }
        } );
        return zk;
    }

    private static String findHdfsActiveNode( String address )
            throws IOException, KeeperException, InterruptedException {
        String hadoopHaPath = "/hadoop-ha";
        ZooKeeper zk = getZooClient( address );
        try {
            List< String > nodes = zk.getChildren( hadoopHaPath, false );
            if ( nodes.size() <= 0 ) {
                throw new IOException( "hadoop-ha path is empty" );
            } else {
                String nameService = nodes.get( 0 );
                String nameServicePath = hadoopHaPath + "/" + nameService;
                List< String > nameNodes = zk.getChildren( nameServicePath,
                        false );
                if ( nameNodes.size() <= 0 ) {
                    throw new IOException( "nameServicePath path is empty" );
                } else {
                    for ( String nameNode : nameNodes ) {
                        String nameNodePath = nameServicePath + "/" + nameNode;
                        byte[] data = zk.getData( nameNodePath, false, null );
                        HAZKInfoProtos.ActiveNodeInfo activeNodeInfo = HAZKInfoProtos.ActiveNodeInfo
                                .parseFrom( data );
                        String hostname = activeNodeInfo.getHostname();
                        int port = activeNodeInfo.getPort();
                        return hostname + ":" + port;
                    }
                }
            }
        } finally {
            zk.close();
        }
        return null;
    }
}
