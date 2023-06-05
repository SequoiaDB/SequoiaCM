package com.sequoiacm.testcommon;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.testcommon.dsutils.HbaseUtils;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;
import org.testng.Assert;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.common.ScmType.ServerScope;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.dsutils.CephS3Utils;
import com.sequoiacm.testcommon.dsutils.CephSwiftUtils;
import com.sequoiacm.testcommon.dsutils.HdfsUtils;
import com.sequoiadb.base.*;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.ConfigOptions;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.DBLob;
import com.sequoiadb.base.Sequoiadb;

public class TestSdbTools {
    // SCMSYSTEM
    public static final String SCM_CS = "SCMSYSTEM";
    public static final String SCM_CL_SITE = "SITE";
    public static final String SCM_CL_USER = "USER";
    public static final String SCM_CL_SESSION = "SESSIONS";
    public static final String SCM_CL_TASK = "TASK";
    public static final String SCM_CL_CONTENTSEVER = "CONTENTSERVER";
    public static final String SCM_CL_WORKSPACE = "WORKSPACE";
    // META
    public static final String WK_CS_FILE = "_META";
    public static final String WK_CL_FILE = "FILE";
    public static final String WK_CL_FILE_HISTORY = "FILE_HISTORY";
    public static final String WK_CL_TRANSACTION_LOG = "TRANSACTION_LOG";
    public static final String WK_CL_CLASS_DEFINE = "CLASS_DEFINE";
    public static final String WK_CL_CLASS_ATTR_DEFINE = "CLASS_ATTR_DEFINE";
    public static final String WK_CL_CLASS_ATTR_REL = "CLASS_ATTR_REL";
    private static final Logger logger = Logger.getLogger( TestSdbTools.class );
    // current time
    private static SimpleDateFormat yearFm = new SimpleDateFormat( "yyyy" );
    private static SimpleDateFormat monthFm = new SimpleDateFormat( "MM" );
    private static SimpleDateFormat daythFm = new SimpleDateFormat( "dd" );

    /**
     * @descreption 获取sdb连接
     * @param sdbUrl
     * @return
     * @throws
     */
    public static Sequoiadb getSdb( String sdbUrl ) {
        Sequoiadb sdb = new Sequoiadb( sdbUrl, TestScmBase.sdbUserName,
                TestScmBase.sdbPassword );
        sdb.setSessionAttr( new BasicBSONObject( "PreferedInstance", "M" ) );
        return sdb;
    }

    /**
     * @descreption 连接db获取DomainName
     * @param sdbUrl
     * @return
     * @throws
     */
    public static List< String > getDomainNames( String sdbUrl ) {
        Sequoiadb sdb = null;
        DBCursor cursor = null;
        List< String > domainNames = new ArrayList<>();
        try {
            sdb = getSdb( sdbUrl );
            cursor = sdb.listDomains( null, null, null, null );
            while ( cursor.hasNext() ) {
                String name = ( String ) cursor.getNext().get( "Name" );
                domainNames.add( name );
            }
        } finally {
            if ( null != cursor ) {
                cursor.close();
            }
            if ( null != sdb ) {
                sdb.close();
            }
        }
        return domainNames;
    }

    /**
     * @descreption 统计db集群下集合内文件数量
     * @param urls
     * @param user
     * @param password
     * @param clName
     * @param csName
     * @param match
     * @return
     * @throws Exception
     */
    public static long count( String urls, String user, String password,
            String csName, String clName, Object match ) throws Exception {
        Sequoiadb db = null;
        long num = 0;
        try {
            db = new Sequoiadb( urls, user, password, new ConfigOptions() );
            DBCollection cl = db.getCollectionSpace( csName )
                    .getCollection( clName );
            if ( match instanceof BasicBSONObject ) {
                num = cl.getCount( ( BasicBSONObject ) match );
            } else if ( match instanceof String ) {
                num = cl.getCount( ( String ) match );
            } else {
                throw new Exception( "invalid type!!!" );
            }
        } finally {
            if ( db != null ) {
                db.close();
            }
        }
        return num;
    }

    /**
     * @descreption 指定db下集合插入数据
     * @param urls
     * @param user
     * @param password
     * @param csName
     * @param records
     * @return
     * @throws Exception
     */
    public static void insert( String urls, String user, String password,
            String csName, String clName, Object records ) throws Exception {
        Sequoiadb db = null;
        try {
            db = new Sequoiadb( urls, user, password, new ConfigOptions() );
            DBCollection cl = db.getCollectionSpace( csName )
                    .getCollection( clName );
            if ( records instanceof BasicBSONObject ) {
                cl.insert( ( BasicBSONObject ) records );
            } else if ( records instanceof ArrayList ) {
                cl.insert( ( ArrayList< BSONObject > ) records );
            } else if ( records instanceof String ) {
                cl.insert( ( String ) records );
            } else {
                throw new Exception( "invalid type!!!" );
            }
        } finally {
            if ( db != null ) {
                db.close();
            }
        }
    }

    /**
     * @descreption 指定db下集合修改数据
     * @param urls
     * @param user
     * @param password
     * @param csName
     * @param clName
     * @param matcher
     * @param modify
     * @return
     * @throws Exception
     */
    public static void update( String urls, String user, String password,
            String csName, String clName, BSONObject matcher,
            BSONObject modify ) throws Exception {
        Sequoiadb db = null;
        try {
            db = new Sequoiadb( urls, user, password, new ConfigOptions() );
            DBCollection cl = db.getCollectionSpace( csName )
                    .getCollection( clName );
            cl.update( matcher, modify, new BasicBSONObject() );
        } finally {
            if ( db != null ) {
                db.close();
            }
        }
    }

    /**
     * @descreption 指定db下集合删除数据
     * @param urls
     * @param user
     * @param password
     * @param csName
     * @param clName
     * @param matcher
     * @return
     * @throws
     */
    public static void delete( String urls, String user, String password,
            String csName, String clName, BSONObject matcher )
            throws Exception {
        Sequoiadb db = null;
        try {
            db = new Sequoiadb( urls, user, password, new ConfigOptions() );
            DBCollection cl = db.getCollectionSpace( csName )
                    .getCollection( clName );
            cl.delete( matcher );
        } finally {
            if ( db != null ) {
                db.close();
            }
        }
    }

    /**
     * @descreption 指定db下集合查询数据
     * @param urls
     * @param user
     * @param password
     * @param csName
     * @param clName
     * @param matcher
     * @return
     * @throws
     */
    public static List< BSONObject > query( String urls, String user,
            String password, String csName, String clName,
            BSONObject matcher ) {
        Sequoiadb db = null;
        DBCursor cursor = null;
        List< BSONObject > objects = new ArrayList<>();
        try {
            db = new Sequoiadb( urls, user, password, new ConfigOptions() );
            DBCollection cl = db.getCollectionSpace( csName )
                    .getCollection( clName );
            cursor = cl.query( matcher, null, null, null );
            while ( cursor.hasNext() ) {
                objects.add( ( BasicBSONObject ) cursor.getNext() );
            }
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
            if ( db != null ) {
                db.close();
            }
        }
        return objects;
    }

    /**
     * @descreption 连接db创建集合空间和集合
     * @param urls
     * @param user
     * @param password
     * @param csName
     * @param clName
     * @return
     * @throws
     */
    public static void createCSCL( String urls, String user, String password,
            String csName, String clName ) {
        Sequoiadb db = null;
        try {
            db = new Sequoiadb( urls, user, password, new ConfigOptions() );
            if ( !db.isCollectionSpaceExist( csName ) ) {
                db.createCollectionSpace( csName );
            }
            CollectionSpace cs = db.getCollectionSpace( csName );
            if ( !cs.isCollectionExist( clName ) ) {
                cs.createCollection( clName );
            }
        } finally {
            if ( db != null ) {
                db.close();
            }
        }
    }

    /**
     * @descreption 连接db创建集合空间和集合 
     * @param sdb 
     * @param csName 
     * @param doMainName 
     * @return
     * @throws Exception
     */
    public static void checkCsInDomain( Sequoiadb sdb, String csName,
            String doMainName ) throws Exception {
        Domain domain = sdb.getDomain( doMainName );
        DBCursor listCSInDomain = null;
        try {
            listCSInDomain = domain.listCSInDomain();
            while ( listCSInDomain.hasNext() ) {
                BSONObject csBson = listCSInDomain.getNext();
                if ( csName.equals( BsonUtils.getString( csBson, "Name" ) ) ) {
                    return;
                }
            }
        } finally {
            if ( null != listCSInDomain ) {
                listCSInDomain.close();
            }
        }
        throw new Exception( csName + " not in domain " + doMainName );
    }

    /**
     * @descreption get scmFile meta csName by connect SDB
     * @param ws
     * @return
     */
    public static String getFileMetaCsName( WsWrapper ws ) {
        return getFileMetaCsName( ws.getName() );
    }

    /**
     * @descreption get scmFile meta csName by connect SDB
     * @param wsName
     * @return
     */
    public static String getFileMetaCsName( String wsName ) {
        return wsName + "_META";
    }

    /**
     * @descreption get scmFile data csName by connect SDB
     * @param siteId
     * @param wsName
     * @return
     */
    public static String getFileDataCsName( int siteId, String wsName )
            throws ScmException {
        String prefix = wsName + "_LOB";
        String shardType = null;
        BSONObject dataShardingType = getDataShardingTypeForSdb( siteId,
                wsName );
        if ( null == dataShardingType ) {
            shardType = "year";
        } else {
            shardType = ( String ) dataShardingType.get( "collection_space" );
            if ( null == shardType ) {
                shardType = "year"; // default year
            }
        }

        if ( !shardType.equals( "none" ) ) {
            prefix += "_";
        }

        String postfix = getCsClPostfix( shardType );

        return prefix + postfix;
    }

    /**
     * @descreption get scmFile data csName by connect SDB
     * @param siteId
     * @param wsName
     * @return
     */
    public static String getFileDataClName( int siteId, String wsName )
            throws ScmException {
        String prefix = "LOB_";
        String shardType = null;
        BSONObject dataShardingType = getDataShardingTypeForSdb( siteId,
                wsName );
        if ( null == dataShardingType ) {
            shardType = "month";
        } else {
            shardType = ( String ) dataShardingType.get( "collection" );
            if ( null == shardType ) {
                shardType = "month"; // default month
            }
        }
        String postfix = getCsClPostfix( shardType );

        return prefix + postfix;
    }

    /**
     * @descreption 根据分区规则获取CS和CL后缀
     * @param shardType
     * @return
     */
    public static String getCsClPostfix( String shardType ) {
        Date currTime = new Date();
        String currY = yearFm.format( currTime );
        String currM = monthFm.format( currTime );
        String currD = daythFm.format( currTime );
        String postfix = null;
        if ( shardType.equals( "none" ) ) {
            postfix = "";
        } else if ( shardType.equals( "year" ) ) {
            postfix = currY;
        } else if ( shardType.equals( "quarter" ) ) {
            int quarter = ( int ) Math.ceil( Double.parseDouble( currM ) / 3 );
            postfix = currY + "Q" + quarter;
        } else if ( shardType.equals( "month" ) ) {
            postfix = currY + currM;
        } else if ( shardType.equals( "day" ) ) {
            postfix = currY + currM + currD;
        }
        return postfix;
    }

    /**
     * @descreption 获取工作区下db数据源的分区规则
     * @param siteId
     * @param wsName
     * @return
     */
    private static BSONObject getDataShardingTypeForSdb( int siteId,
            String wsName ) throws ScmException {
        Object dataShardingType = getWsProperties( siteId, wsName,
                "data_sharding_type" );
        return ( BSONObject ) dataShardingType;
    }

    /**
     * @descreption 获取工作区下其他数据源的分区规则
     * @param siteId
     * @param wsName
     * @return
     */
    public static String getDataShardingTypeForOtherDs( int siteId,
            String wsName ) throws ScmException {
        Object dataShardingType = getWsProperties( siteId, wsName,
                "data_sharding_type" );
        return ( String ) dataShardingType;
    }

    /**
     * @descreption 获取工作区前缀
     * @param siteId
     * @param wsName
     * @return
     */
    public static Object getContainerPrefix( int siteId, String wsName )
            throws ScmException {
        return getWsProperties( siteId, wsName, "container_prefix" );
    }

    /**
     * @descreption 获取工作区前缀
     * @param siteId
     * @param wsName
     * @param key
     * @return
     */
    private static Object getWsProperties( int siteId, String wsName,
            String key ) throws ScmException {
        ScmSession session = null;
        Object dataShardingType = null;
        ScmCursor< ScmWorkspaceInfo > cursor = null;
        try {
            session = ScmSessionUtils.createSession( ScmInfo.getRootSite() );
            cursor = ScmFactory.Workspace.listWorkspace( session );
            while ( cursor.hasNext() ) {
                ScmWorkspaceInfo info = cursor.getNext();
                if ( info.getName().equals( wsName ) ) {
                    List< BSONObject > dataLocation = info.getDataLocation();
                    for ( BSONObject obj : dataLocation ) {
                        int localSiteId = ( int ) obj.get( "site_id" );
                        if ( siteId == localSiteId ) {
                            dataShardingType = obj.get( key );
                            break;
                        }
                    }
                    break;
                }
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            throw e;
        } finally {
            if ( null != session ) {
                session.close();
            }
            if ( null != cursor ) {
                cursor.close();
            }
        }
        return dataShardingType;
    }

    public static class Lob {

        /**
         * @descreption special cases are used, such as analog LOB remain, by connect DB
         * @param site
         * @param fileId
         * @param filePath
         * @return
         */
        public static void putLob( SiteWrapper site, WsWrapper ws, ScmId fileId,
                String filePath ) throws Exception {
            DatasourceType dsType = site.getDataType();
            if ( dsType.equals( DatasourceType.SEQUOIADB ) ) {
                putDataInSdb( site, ws, fileId, filePath );
            } else if ( dsType.equals( DatasourceType.HBASE ) ) {
                putDataInHbase( site, ws, fileId, filePath );
            } else if ( dsType.equals( DatasourceType.CEPH_S3 ) ) {
                CephS3Utils.putObject( site, ws, fileId, filePath );
            } else if ( dsType.equals( DatasourceType.CEPH_SWIFT ) ) {
                CephSwiftUtils.createObject( site, ws, fileId, filePath );
            } else if ( dsType.equals( DatasourceType.HDFS ) ) {
                HdfsUtils.upload( site, ws, fileId, filePath );
            } else {
                throw new Exception(
                        dsType + ",dataSourceType is not invalid" );
            }
        }

        /**
         * @descreption 直连db数据源插入lob数据
         * @param site
         * @param ws
         * @param fileId
         * @param lobPath
         * @return
         */
        private static void putDataInSdb( SiteWrapper site, WsWrapper ws,
                ScmId fileId, String lobPath )
                throws IOException, ScmException {
            Sequoiadb db = null;
            DBLob lobDB = null;
            InputStream ism = null;
            try {
                String dsUrl = site.getDataDsUrl();
                db = getSdb( dsUrl );

                String csName = getFileDataCsName( site.getSiteId(),
                        ws.getName() );
                String clName = getFileDataClName( site.getSiteId(),
                        ws.getName() );

                if ( !db.isCollectionSpaceExist( csName ) ) {
                    writeTmpFileInScm( site, ws );
                }

                if (!db.getCollectionSpace(csName).isCollectionExist(clName)){
                    writeTmpFileInScm( site, ws );
                }

                if ( db.isCollectionSpaceExist( csName ) ) {
                    DBCollection clDB = db.getCollectionSpace( csName )
                            .getCollection( clName );

                    lobDB = clDB.createLob( new ObjectId( fileId.get() ) );
                    ism = new FileInputStream( new File( lobPath ) );
                    lobDB.write( ism );
                }
            } finally {
                if ( lobDB != null ) {
                    lobDB.close();
                }
                if ( ism != null ) {
                    ism.close();
                }
                if ( db != null ) {
                    db.close();
                }
            }
        }

        /**
         * @descreption 直连Hbase数据源插入lob数据
         * @param site
         * @param ws
         * @param fileId
         * @param lobPath
         * @return
         */
        private static void putDataInHbase( SiteWrapper site, WsWrapper ws,
                ScmId fileId, String lobPath ) {
            Connection conn = null;
            try {
                conn = HbaseUtils.getConnection( site );
                String tableName = HbaseUtils.getDataTableNameInHbase( site.getSiteId(),
                        ws.getName() );
                HBaseAdmin hAdmin = ( HBaseAdmin ) conn.getAdmin();
                if ( !hAdmin.tableExists( tableName ) ) {
                    writeTmpFileInScm( site, ws );
                }

                if ( hAdmin.tableExists( tableName ) ) {
                    Table table = conn
                            .getTable( TableName.valueOf( tableName ) );
                    Put put;
                    byte[] buffer = TestTools.getBuffer( lobPath );
                    int num = ( int ) Math.ceil(
                            Double.valueOf( buffer.length ) / ( 1024 * 1024 ) );
                    for ( int i = 0; i < num; i++ ) {
                        int len = 1024 * 1024;
                        if ( i == num - 1 ) {
                            len = buffer.length - 1024 * 1024 * i;
                        }
                        byte[] fileblock = new byte[ len ];
                        System.arraycopy( buffer, 1024 * 1024 * i, fileblock, 0,
                                len );

                        put = new Put( Bytes.toBytes( fileId.get() ) );
                        put.addColumn( Bytes.toBytes( "SCM_FILE_DATA" ),
                                Bytes.toBytes( "PIECE_NUM_" + i ), fileblock );
                        table.put( put );
                    }

                    put = new Put( Bytes.toBytes( fileId.get() ) );
                    put.addColumn( Bytes.toBytes( "SCM_FILE_META" ),
                            Bytes.toBytes( "FILE_SIZE" ),
                            Bytes.toBytes( String.valueOf(
                                    new File( lobPath ).length() ) ) );
                    table.put( put );

                    put = new Put( Bytes.toBytes( fileId.get() ) );
                    put.addColumn( Bytes.toBytes( "SCM_FILE_META" ),
                            Bytes.toBytes( "FILE_STATUS" ),
                            Bytes.toBytes( "Available" ) );
                    table.put( put );

                    // put is success?
                    Get get = new Get( Bytes.toBytes( fileId.get() ) );
                    Result res = table.get( get );
                    if ( res.isEmpty() ) {
                        throw new Exception(
                                "error, insert data of hbase failed" );
                    }
                    table.close();
                }
            } catch ( Exception e ) {
                Assert.fail( e.getMessage() );
            } finally {
                if ( null != conn ) {
                    try {
                        conn.close();
                    } catch ( IOException e ) {
                        e.printStackTrace();
                    }
                }
            }
        }

        /**
         * @descreption 工作区下写文件后删除（触发数据源LOB创建）
         * @param site
         * @param ws
         * @return
         */
        private static void writeTmpFileInScm( SiteWrapper site, WsWrapper ws )
                throws ScmException {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace scmWs = ScmFactory.Workspace
                        .getWorkspace( ws.getName(), session );
                ScmFile file = ScmFactory.File.createInstance( scmWs );
                file.setAuthor( "TestSdbTools.putDataInSdb" );
                file.setFileName(
                        "TestSdbTools.putDataInSdb" + UUID.randomUUID() );
                ScmId tmpFileId = file.save();

                ScmFactory.File.deleteInstance( scmWs, tmpFileId, true );
            } finally {
                if ( null != session ) {
                    session.close();
                }
            }
        }

        /**
         * @descreption remove LOB connect DB
         * @param site
         * @param ws
         * @param fileId
         * @return
         */
        public static void removeLob( SiteWrapper site, WsWrapper ws,
                ScmId fileId ) throws Exception {
            DatasourceType dsType = site.getDataType();
            if ( dsType.equals( DatasourceType.SEQUOIADB ) ) {
                deleteDataInSdb( site, ws, fileId );
            } else if ( dsType.equals( DatasourceType.HBASE ) ) {
                deleteDataInHbase( site, ws, fileId );
            } else if ( dsType.equals( DatasourceType.CEPH_S3 ) ) {
                CephS3Utils.deleteObject( site, ws, fileId );
            } else if ( dsType.equals( DatasourceType.CEPH_SWIFT ) ) {
                CephSwiftUtils.deleteObject( site, ws, fileId );
            } else if ( dsType.equals( DatasourceType.HDFS ) ) {
                HdfsUtils.delete( site, ws, fileId );
            } else {
                throw new Exception(
                        dsType + ",dataSourceType is not invalid" );
            }
        }

        /**
         * @descreption remove LOB connect DB
         * @param site
         * @param ws
         * @param fileId
         * @return
         */
        private static void deleteDataInSdb( SiteWrapper site, WsWrapper ws,
                ScmId fileId ) throws ScmException {
            Sequoiadb db = null;
            try {
                String dsUrl = site.getDataDsUrl();
                db = getSdb( dsUrl );

                String csName = getFileDataCsName( site.getSiteId(),
                        ws.getName() );
                String clName = getFileDataClName( site.getSiteId(),
                        ws.getName() );
                DBCollection clDB = db.getCollectionSpace( csName )
                        .getCollection( clName );

                ObjectId lobObjId = new ObjectId( fileId.get() );
                clDB.removeLob( lobObjId );
            } finally {
                if ( null != db ) {
                    db.close();
                }
            }
        }

        /**
         * @descreption 直连Hbase数据源删除数据
         * @param site
         * @param ws
         * @param fileId
         * @return
         */
        private static void deleteDataInHbase( SiteWrapper site, WsWrapper ws,
                ScmId fileId ) throws Exception {
            Connection conn = null;
            try {
                conn = HbaseUtils.getConnection( site );
                String tableName = HbaseUtils.getDataTableNameInHbase( site.getSiteId(),
                        ws.getName() );
                Table table = conn.getTable( TableName.valueOf( tableName ) );

                Delete del = new Delete( Bytes.toBytes( fileId.get() ) );
                table.delete( del );

                // delete is success?
                Get get = new Get( Bytes.toBytes( fileId.get() ) );
                Result res = table.get( get );
                if ( !res.isEmpty() ) {
                    throw new Exception( "error, delete data of hbase failed" );
                }
                table.close();
            } finally {
                if ( null != conn ) {
                    conn.close();
                }
            }
        }
    }

    public static class Task {

        /**
         * @descreption 删除task元数据
         * @param taskId
         * @return
         */
        public static void deleteMeta( ScmId taskId ) {
            Sequoiadb sdb = null;
            try {
                sdb = getSdb( TestScmBase.mainSdbUrl );
                DBCollection cl = sdb.getCollectionSpace( SCM_CS )
                        .getCollection( SCM_CL_TASK );
                if ( null != taskId ) {
                    BSONObject obj = new BasicBSONObject();
                    obj.put( "id", taskId.get() );
                    cl.delete( obj );
                }
            } finally {
                if ( null != sdb ) {
                    sdb.close();
                }
            }
        }

        /**
         * @descreption 打印task元数据信息
         * @return
         */
        public static void printlnTaskInfos() {
            List< BSONObject > bsonObjects = TestSdbTools.query(
                    TestScmBase.mainSdbUrl, TestScmBase.sdbUserName,
                    TestScmBase.sdbPassword, SCM_CS, SCM_CL_TASK,
                    new BasicBSONObject() );
            for ( BSONObject bsonObject : bsonObjects ) {
                System.out.println( "taskInfo : " + bsonObject.toString() );
            }
        }
    }

    public static void createDomain( SiteWrapper site, String domainName ) {
        Sequoiadb sdb = null;
        try {
            sdb = TestSdbTools.getSdb( site.getDataDsUrl() );
            List< String > groupNameList = getGroupNames( sdb );
            BSONObject obj = new BasicBSONObject();
            obj.put( "Groups", groupNameList.toArray() );
            if ( sdb.isDomainExist( domainName ) ) {
                sdb.dropDomain( domainName );
                sdb.createDomain( domainName, obj );
            } else {
                sdb.createDomain( domainName, obj );
            }
        } finally {
            if ( sdb != null ) {
                sdb.close();
            }
        }
    }

    public static void dropDomain( SiteWrapper site, String domainName ) {
        Sequoiadb sdb = null;
        try {
            sdb = TestSdbTools.getSdb( site.getDataDsUrl() );
            List< String > groupNameList = getGroupNames( sdb );
            BSONObject obj = new BasicBSONObject();
            obj.put( "Groups", groupNameList.toArray() );
            if ( sdb.isDomainExist( domainName ) ) {
                Domain domain = sdb.getDomain( domainName );
                DBCursor csCursor = null;
                try {
                    csCursor = domain.listCSInDomain();
                    while ( csCursor.hasNext() ) {
                        BSONObject csCursorNext = csCursor.getNext();
                        String csName = BsonUtils.getString( csCursorNext,
                                "Name" );
                        sdb.dropCollectionSpace( csName );
                    }
                } finally {
                    if ( csCursor != null ) {
                        csCursor.close();
                    }
                }
                sdb.dropDomain( domainName );
            }
        } finally {
            if ( sdb != null ) {
                sdb.close();
            }
        }
    }

    public static List< String > getGroupNames( Sequoiadb db ) {
        List< String > groupNameList = db.getReplicaGroupNames();
        List< String > sysGroupname = new ArrayList< String >();
        int num = groupNameList.size();
        for ( int i = 0; i < num; i++ ) {
            if ( groupNameList.get( i ).contains( "SYS" ) ) {
                sysGroupname.add( groupNameList.get( i ) );
            }
        }
        groupNameList.removeAll( sysGroupname );
        return groupNameList;
    }

    public static void deleteLobCS( SiteWrapper site ) {
        List< String > csNames = getLobCsNames( site );
        Sequoiadb sdb = null;
        try {
            sdb = TestSdbTools.getSdb( site.getDataDsUrl() );
            for ( String csName : csNames ) {
                sdb.dropCollectionSpace( csName );
            }
        } finally {
            if ( sdb != null ) {
                sdb.close();
            }
        }
    }

    private static List< String > getLobCsNames( SiteWrapper site ) {
        Sequoiadb sdb = null;
        List< String > lodCSNames = null;
        try {
            sdb = TestSdbTools.getSdb( site.getDataDsUrl() );
            List< String > csNames = sdb.getCollectionSpaceNames();
            lodCSNames = new ArrayList<>();
            for ( String name : csNames ) {
                if ( name.contains( "LOB" ) ) {
                    lodCSNames.add( name );
                }
            }
        } finally {
            if ( sdb != null ) {
                sdb.close();
            }
        }
        return lodCSNames;
    }
}
