package com.sequoiacm.testcommon.scmutils;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmDataCheckLevel;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ScmScheduleUtils extends TestScmBase {
    private static final Logger logger = Logger.getLogger( ScmTaskUtils.class );
    private static final int defaultTimeOut = 5 * 60; // 5min

    public static void checkScmFile( ScmWorkspace ws, List< ScmId > fileIds,
            SiteWrapper[] expSites ) throws Exception {
        checkScmFile( ws, fileIds, 0, fileIds.size(), expSites,
                defaultTimeOut );
    }

    public static void checkScmFile( ScmWorkspace ws, List< ScmId > fileIds,
            SiteWrapper[] expSites, int timeOutSec ) throws Exception {
        checkScmFile( ws, fileIds, 0, fileIds.size(), expSites, timeOutSec );
    }

    public static void checkScmFile( ScmWorkspace ws, List< ScmId > fileIds,
            int startNum, int endNum, SiteWrapper[] expSites )
            throws Exception {
        checkScmFile( ws, fileIds, startNum, endNum, expSites, defaultTimeOut );
    }

    public static void checkScmFile( ScmWorkspace ws, List< ScmId > fileIds,
            int startNum, int endNum, SiteWrapper[] expSites, int timeOutSec )
            throws Exception {
        ScmId fileId = null;
        for ( int i = startNum; i < endNum; i++ ) {
            fileId = fileIds.get( i );
            int sleepTime = 500;
            int maxRetryTimes = ( timeOutSec * 1000 ) / sleepTime;
            int retryTimes = 0;
            while ( true ) {
                try {
                    ScmFileUtils.checkMeta( ws, fileId, expSites );
                    break;
                } catch ( Exception e ) {
                    if ( e.getMessage() != null
                            && e.getMessage()
                                    .contains( "Failed to check siteNum" )
                            && retryTimes < maxRetryTimes ) {
                        Thread.sleep( sleepTime );
                        retryTimes++;
                    } else {
                        TestSdbTools.Task.printlnTaskInfos();
                        throw new Exception(
                                "failed to wait task finished, " + "fileId = "
                                        + fileId + ", " + e.getMessage() );
                    }
                }
            }
        }
    }

    public static void cleanTask( ScmSession session, ScmId scheId )
            throws Exception {
        ScmCursor< ScmTaskBasicInfo > cursor = null;
        try {
            BSONObject cond = ScmQueryBuilder.start( "schedule_id" )
                    .is( scheId.get() ).get();
            cursor = ScmSystem.Task.listTask( session, cond );
            while ( cursor.hasNext() ) {
                ScmTaskBasicInfo info = cursor.getNext();
                ScmId taskId = info.getId();
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } catch ( ScmException e ) {
            throw e;
        } finally {
            cursor.close();
        }
    }

    /**
     * judge by schedule_id
     */
    public static boolean isRunningOfSche( ScmSession session,
            ScmId scheduleId ) throws Exception {
        BSONObject cond = ScmQueryBuilder.start( "schedule_id" )
                .is( scheduleId.get() ).get();
        return isRunningOfSche( session, scheduleId, cond );
    }

    /**
     * judge by schedule_id and type of task
     */
    public static boolean isRunningOfSche( ScmSession session, ScmId scheduleId,
            int taskType ) throws Exception {
        BSONObject condition = ScmQueryBuilder.start( "schedule_id" )
                .is( scheduleId.get() ).and( "type" ).is( taskType ).get();
        return isRunningOfSche( session, scheduleId, condition );
    }

    /**
     * judge by condition
     */
    public static boolean isRunningOfSche( ScmSession session, ScmId scheduleId,
            BSONObject condition ) throws Exception {
        ScmCursor< ScmTaskBasicInfo > cursor = null;
        boolean isRunning = false;
        int maxRetryTimes = 300;
        int retryTimes = 0;
        int sleepTime = 200;
        try {
            while ( true ) {
                cursor = ScmSystem.Task.listTask( session, condition );
                isRunning = cursor.hasNext();
                if ( isRunning ) {
                    logger.info( "retryTimes = " + retryTimes
                            + ", retry time = " + retryTimes * sleepTime );
                    break;
                } else if ( retryTimes < maxRetryTimes ) {
                    Thread.sleep( sleepTime );
                    retryTimes++;
                } else if ( retryTimes == maxRetryTimes ) {
                    throw new Exception( "waiting transfer running failed." );
                }
            }
        } finally {
            cursor.close();
        }
        return isRunning;
    }

    public static void outputTaskInfo( ScmSession session, ScmId scheduleId )
            throws ScmException {
        List< String > infoList = new ArrayList<>();
        ScmCursor< ScmTaskBasicInfo > cursor = null;
        ScmId taskId = null;
        try {
            BSONObject cond = ScmQueryBuilder.start( "schedule_id" )
                    .is( scheduleId.get() ).get();
            cursor = ScmSystem.Task.listTask( session, cond );
            while ( cursor.hasNext() ) {
                ScmTaskBasicInfo info = cursor.getNext();
                taskId = info.getId();

                ScmTask task = ScmSystem.Task.getTask( session, taskId );
                infoList.add( task.toString() + "\n" );
            }
        } finally {
            cursor.close();
        }
        logger.info( "scheduleId = " + scheduleId.get() + ", task info \n"
                + infoList );
    }

    public static void outputScmfileInfo( ScmWorkspace ws,
            List< ScmId > fileIds ) throws ScmException {
        List< String > infoList = new ArrayList<>();
        for ( ScmId fileId : fileIds ) {
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            infoList.add(
                    file.toString() + ", " + file.getLocationList() + "\n" );
        }
        logger.info( "ws = " + ws.getName() + ", file info \n" + infoList );
    }

    public static ScmId getTaskId( ScmSession session, ScmId scheduleId )
            throws Exception {
        ScmCursor< ScmTaskBasicInfo > cursor = null;
        ScmId taskId = null;
        try {
            BSONObject cond = ScmQueryBuilder.start( "schedule_id" )
                    .is( scheduleId.get() ).get();
            cursor = ScmSystem.Task.listTask( session, cond );
            while ( cursor.hasNext() ) {
                ScmTaskBasicInfo info = cursor.getNext();
                taskId = info.getId();
            }
        } finally {
            cursor.close();
        }
        return taskId;
    }

    /**
     * scheduleServer host time is not sync with SCM host time, cause may not
     * match scmFile need to add sleep strategy
     */
    public static long sleepStrategy( ScmSession session, ScmWorkspace ws,
            ScmId scheduleId, ScmId fileId, int expEstCount ) throws Exception {
        long sleepTime = 0;
        ScmId taskId = getTaskId( session, scheduleId );
        ScmTask task = ScmSystem.Task.getTask( session, taskId );
        if ( task.getActualCount() < expEstCount ) {
            // get the create time of scmFile
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            long fileCreateTime = file.getCreateTime().getTime();

            // get the create time of schedule'task content
            BSONObject cond = task.getContent();
            // System.out.println("content: " + content);
            BSONObject condA = ( BSONObject ) ( ( BasicBSONList ) cond
                    .get( "$and" ) ).get( 0 );
            long scheMatchTime = ( long ) ( ( BSONObject ) ( ( BSONObject ) ( ( BSONObject ) condA
                    .get( "site_list" ) ).get( "$elemMatch" ) )
                            .get( "create_time" ) ).get( "$lt" );

            // computational time difference
            long maxDiffTime = 17 * 1000;
            long timeDiff = fileCreateTime - scheMatchTime;
            sleepTime = timeDiff + 1200;
            if ( timeDiff > 0 && timeDiff < maxDiffTime ) {
                Thread.sleep( sleepTime );
                logger.info( "sleep end, " + "sleepTime: " + sleepTime );
            } else if ( timeDiff > maxDiffTime ) {
                logger.info( "task content \n" + cond + "\nscheMatchTime: "
                        + scheMatchTime );
                logger.info( "file info \n" + file.toString()
                        + "\nfileCreateTime: " + fileCreateTime );
                throw new Exception(
                        "the time difference between the schedule host and "
                                + "the scm host is more than " + maxDiffTime
                                + "." );
            } else if ( timeDiff < 0 ) {
                throw new Exception( "task is normal." );
            }
        }
        logger.info( "sleepStrategy end." );
        return sleepTime;
    }

    public static Date getDate( String dateStr ) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                "yyyy-MM-dd" );
        Date date = simpleDateFormat.parse( dateStr );
        return date;
    }

    /**
     * @Descrip 获取根据id排序后的所有分站点
     * @return
     * @throws Exception
     */
    public static List< SiteWrapper > getSortBranchSites() throws Exception {
        List< SiteWrapper > branchSites = ScmInfo.getBranchSites();
        if ( branchSites.size() < 1 ) {
            throw new Exception(
                    "the number of branchSites must greater than 2 " );
        }
        Collections.sort( branchSites, new Comparator< SiteWrapper >() {
            @Override
            public int compare( SiteWrapper o1, SiteWrapper o2 ) {
                return o1.getSiteId() - o2.getSiteId();
            }
        } );
        return branchSites;
    }

    public static ScmSchedule createCopySchedule( ScmSession session,
            SiteWrapper sourceSite, SiteWrapper targetSite, WsWrapper wsp,
            BSONObject cond ) throws ScmException {
        UUID uuid = UUID.randomUUID();
        String maxStayTime = "0d";
        String scheduleName = "testCopy" + uuid;
        String description = "copy " + uuid;
        ScmScheduleBuilder schBuilder = ScmSystem.Schedule
                .scheduleBuilder( session );
        ScmScheduleCopyFileContent copyContent = new ScmScheduleCopyFileContent(
                sourceSite.getSiteName(), targetSite.getSiteName(), maxStayTime,
                cond );
        schBuilder.type( ScheduleType.COPY_FILE ).workspace( wsp.getName() )
                .name( scheduleName ).description( description )
                .content( copyContent ).cron( "* * * * * ?" ).enable( true );
        return schBuilder.build();
    }

    public static ScmSchedule createCleanSchedule( ScmSession session,
            SiteWrapper targetSite, WsWrapper wsp, BSONObject cond )
            throws ScmException {
        UUID uuid = UUID.randomUUID();
        String maxStayTime = "0d";
        String scheduleName = "testClean" + uuid;
        String description = "copy " + uuid;
        ScmScheduleBuilder schBuilder = ScmSystem.Schedule
                .scheduleBuilder( session );
        ScmScheduleCleanFileContent cleanContent = new ScmScheduleCleanFileContent(
                targetSite.getSiteName(), maxStayTime, cond );
        schBuilder.type( ScheduleType.CLEAN_FILE ).workspace( wsp.getName() )
                .name( scheduleName ).description( description )
                .content( cleanContent ).cron( "* * * * * ?" ).enable( true );
        return schBuilder.build();
    }

    /**
     * @Descrip 创建迁移任务
     * @param session
     * @param sourceSite
     * @param targetSite
     * @param wsp
     * @param cond
     * @param region
     * @param zone
     * @return
     * @throws ScmException
     */
    public static ScmSchedule createCopySchedule( ScmSession session,
            SiteWrapper sourceSite, SiteWrapper targetSite, WsWrapper wsp,
            BSONObject cond, String region, String zone ) throws ScmException {
        UUID uuid = UUID.randomUUID();
        String maxStayTime = "0d";
        String scheduleName = "testCopy" + uuid;
        String description = "copy " + uuid;
        ScmScheduleBuilder schBuilder = ScmSystem.Schedule
                .scheduleBuilder( session );
        ScmScheduleCopyFileContent copyContent = new ScmScheduleCopyFileContent(
                sourceSite.getSiteName(), targetSite.getSiteName(), maxStayTime,
                cond );
        schBuilder.type( ScheduleType.COPY_FILE ).workspace( wsp.getName() )
                .name( scheduleName ).description( description )
                .content( copyContent ).cron( "* * * * * ?" ).enable( true )
                .preferredRegion( region ).preferredZone( zone );
        return schBuilder.build();
    }

    /**
     * @Descrip 创建清理任务
     * @param session
     * @param targetSite
     * @param wsp
     * @param cond
     * @param region
     * @param zone
     * @return
     * @throws ScmException
     */
    public static ScmSchedule createCleanSchedule( ScmSession session,
            SiteWrapper targetSite, WsWrapper wsp, BSONObject cond,
            String region, String zone ) throws ScmException {
        UUID uuid = UUID.randomUUID();
        String maxStayTime = "0d";
        String scheduleName = "testClean" + uuid;
        String description = "copy " + uuid;
        ScmScheduleBuilder schBuilder = ScmSystem.Schedule
                .scheduleBuilder( session );
        ScmScheduleCleanFileContent cleanContent = new ScmScheduleCleanFileContent(
                targetSite.getSiteName(), maxStayTime, cond );
        schBuilder.type( ScheduleType.CLEAN_FILE ).workspace( wsp.getName() )
                .name( scheduleName ).description( description )
                .content( cleanContent ).cron( "* * * * * ?" ).enable( true )
                .preferredRegion( region ).preferredZone( zone );
        return schBuilder.build();
    }

    /**
     * 创建清理调度任务更新为迁移任务
     * 
     * @param session
     * @param sourceSite
     * @param targetSite
     * @param cleanSite
     * @param wsp
     * @param fileName
     * @return
     * @throws Exception
     */
    public static ScmSchedule createSchedule( ScmSession session,
            SiteWrapper sourceSite, SiteWrapper targetSite,
            SiteWrapper cleanSite, WsWrapper wsp, String fileName )
            throws Exception {
        String maxStayTime = "0d";
        BSONObject queryCond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmScheduleBuilder scmScheduleBuilder = ScmSystem.Schedule
                .scheduleBuilder( session );
        ScmScheduleCleanFileContent cleanContent = new ScmScheduleCleanFileContent(
                cleanSite.getSiteName(), maxStayTime, queryCond );
        scmScheduleBuilder.type( ScheduleType.CLEAN_FILE )
                .workspace( wsp.getName() ).name( "clean file " + fileName )
                .content( cleanContent ).cron( "* * * * * ? " ).enable( true );
        ScmSchedule schedule = scmScheduleBuilder.build();
        waitForTask( schedule, 10 );
        ScmScheduleCopyFileContent copyContent = new ScmScheduleCopyFileContent(
                sourceSite.getSiteName(), targetSite.getSiteName(), maxStayTime,
                queryCond );
        schedule.updateSchedule( ScheduleType.COPY_FILE, copyContent );
        waitForTask( schedule, 20 );
        schedule.disable();
        return schedule;
    }

    /**
     * 创建迁移清理调度任务
     *
     * @param session
     * @param sourceSite
     * @param targetSite
     * @param wsName
     * @param cond
     * @param checkLevel
     * @param isRecycleSpace
     * @param quickStart
     * @return
     * @throws Exception
     */
    public static ScmSchedule createMoveSchedule( ScmSession session,
            SiteWrapper sourceSite, SiteWrapper targetSite, String wsName,
            BSONObject cond, ScmDataCheckLevel checkLevel,
            boolean isRecycleSpace, boolean quickStart ) throws ScmException {
        return createMoveSchedule( session, sourceSite, targetSite, wsName,
                cond, ScmType.ScopeType.SCOPE_CURRENT, checkLevel,
                isRecycleSpace, quickStart );
    }

    /**
     * 创建迁移清理调度任务
     *
     * @param session
     * @param sourceSite
     * @param targetSite
     * @param wsName
     * @param cond
     * @param checkLevel
     * @param isRecycleSpace
     * @param quickStart
     * @param scopeType
     * @return
     * @throws Exception
     */
    public static ScmSchedule createMoveSchedule( ScmSession session,
            SiteWrapper sourceSite, SiteWrapper targetSite, String wsName,
            BSONObject cond, ScmType.ScopeType scopeType,
            ScmDataCheckLevel checkLevel, boolean isRecycleSpace,
            boolean quickStart ) throws ScmException {
        UUID uuid = UUID.randomUUID();
        String maxStayTime = "0d";
        String scheduleName = "testMove" + uuid;
        String description = "move " + uuid;
        ScmScheduleBuilder schBuilder = ScmSystem.Schedule
                .scheduleBuilder( session );
        ScmScheduleMoveFileContent copyContent = new ScmScheduleMoveFileContent(
                sourceSite.getSiteName(), targetSite.getSiteName(), maxStayTime,
                cond, scopeType, 30000L, checkLevel, isRecycleSpace,
                quickStart );
        schBuilder.type( ScheduleType.MOVE_FILE ).workspace( wsName )
                .name( scheduleName ).description( description )
                .content( copyContent ).cron( "0/15 * * * * ?" ).enable( true );
        return schBuilder.build();
    }

    /**
     * 校验db数据源lob表是否存在
     *
     * @param site
     * @param csName
     * @return
     * @throws Exception
     */
    public static boolean checkLobCS( SiteWrapper site, String csName )
            throws Exception {
        if ( site.getDataType() != ScmType.DatasourceType.SEQUOIADB ) {
            throw new Exception( "站点类型必须为SEQUOIADB！" );
        }
        boolean isExist;
        Sequoiadb db = null;
        try {
            String dsUrl = site.getDataDsUrl();
            db = TestSdbTools.getSdb( dsUrl );
            if ( !db.isCollectionSpaceExist( csName ) ) {
                isExist = false;
            } else {
                isExist = true;
            }
        } finally {
            db.close();
        }
        return isExist;
    }

    public static void deleteLobCS( SiteWrapper site, String csName ) {
        Sequoiadb db = null;
        try {
            String dsUrl = site.getDataDsUrl();
            db = TestSdbTools.getSdb( dsUrl );
            if ( db.isCollectionSpaceExist( csName ) ) {
                db.dropCollectionSpace( csName );
            }
        } finally {
            db.close();
        }
    }

    /**
     * @Descrip 等待调度任务执行
     * @param schedule
     * @param minTaskNum
     *            调度任务执行次数
     * @throws Exception
     */
    public static void waitForTask( ScmSchedule schedule, int minTaskNum )
            throws Exception {
        int time = 0;
        while ( true ) {
            List< ScmTask > tasks = schedule.getTasks( new BasicBSONObject(),
                    new BasicBSONObject(), 0, -1 );
            if ( tasks.size() > minTaskNum ) {
                break;
            }
            time++;
            if ( time > 120 ) {
                throw new Exception( "waiting for task timeout" );
            }
            Thread.sleep( 1000 );
        }
    }

    /**
     * @Descrip 校验节点所在region
     * @param tasks
     * @param session
     * @param region
     * @throws Exception
     */
    public static void checkNodeRegion( List< ScmTask > tasks,
            ScmSession session, String region ) throws Exception {
        List< String > runNodeNames = findRunNodeName( tasks );
        for ( String runNodeName : runNodeNames ) {
            checkRegion( runNodeName, session, region );
        }
    }

    /**
     * @Descrip 校验节点所在region和zone
     * @param tasks
     * @param session
     * @param region
     * @param zone
     * @throws Exception
     */
    public static void checkNodeRegionAndZone( List< ScmTask > tasks,
            ScmSession session, String region, String zone ) throws Exception {
        List< String > runNodeNames = findRunNodeName( tasks );
        for ( String runNodeName : runNodeNames ) {
            checkRegionAndZone( runNodeName, session, region, zone );
        }
    }

    /**
     * @Descrip 获取执行调度任务的节点
     * @param tasks
     * @return
     */
    public static List< String > findRunNodeName( List< ScmTask > tasks ) {
        final String SCMSYSTEM = "SCMSYSTEM";
        final String CONTENTSERVER = "CONTENTSERVER";
        final String id = "id";
        final String name = "name";
        List< BSONObject > nodes = new ArrayList<>();
        Sequoiadb db = new Sequoiadb( TestScmBase.mainSdbUrl, "", "" );
        try {
            DBCollection contentServerCl = db.getCollectionSpace( SCMSYSTEM )
                    .getCollection( CONTENTSERVER );
            DBCursor query = contentServerCl.query();
            while ( query.hasNext() ) {
                BSONObject node = query.getNext();
                nodes.add( node );
            }
        } finally {
            db.close();
        }
        List< String > runNodeNames = new ArrayList<>();
        for ( ScmTask task : tasks ) {
            for ( BSONObject node : nodes ) {
                Integer node_id = ( Integer ) node.get( id );
                if ( node_id.equals( task.getServerId() ) ) {
                    runNodeNames.add( ( String ) node.get( name ) );
                }
            }
        }
        return runNodeNames;
    }

    private static void checkRegionAndZone( String runNodeName,
            ScmSession session, String region, String zone ) throws Exception {
        if ( runNodeName == null ) {
            throw new Exception( "The node info to serviceId cannot be found" );
        }
        List< ScmServiceInstance > contentServerInstanceList = ScmSystem.ServiceCenter
                .getContentServerInstanceList( session );
        for ( ScmServiceInstance contentServer : contentServerInstanceList ) {
            String nodeIpPort = contentServer.getIp() + "_"
                    + contentServer.getPort();
            if ( nodeIpPort.equals( runNodeName ) ) {
                if ( !( contentServer.getRegion().equals( region )
                        && contentServer.getZone().equals( zone ) ) ) {
                    throw new Exception( "except region zone is " + region + " "
                            + zone + "; but actually region zone is "
                            + contentServer.getRegion() + " "
                            + contentServer.getZone() );
                }
            }
        }
    }

    private static void checkRegion( String runNodeName, ScmSession session,
            String region ) throws Exception {
        if ( runNodeName == null ) {
            throw new Exception( "The node info to serviceId cannot be found" );
        }
        List< ScmServiceInstance > contentServerInstanceList = ScmSystem.ServiceCenter
                .getContentServerInstanceList( session );
        for ( ScmServiceInstance contentServer : contentServerInstanceList ) {
            String nodeIpPort = contentServer.getIp() + "_"
                    + contentServer.getPort();
            if ( ( nodeIpPort.equals( runNodeName ) ) ) {
                if ( !( contentServer.getRegion().equals( region ) ) ) {
                    throw new Exception( "except region is " + region
                            + "; but actually region is "
                            + contentServer.getRegion() );
                }
            }
        }
    }

    /**
     * @Descrip 获取success_count值大于0的任务
     * @param schedule
     * @return
     * @throws ScmException
     */
    public static List< ScmTask > getSuccessTasks( ScmSchedule schedule )
            throws ScmException {
        BSONObject orderBy = ScmQueryBuilder
                .start( ScmAttributeName.Task.STOP_TIME ).is( -1 ).get();
        List< ScmTask > tasks = schedule
                .getTasks(
                        new BasicBSONObject( "success_count",
                                new BasicBSONObject( "$gt", 0 ) ),
                        orderBy, 0, -1 );
        return tasks;
    }

    /**
     * @descreption 获取调度任务触发的第一个任务
     * @param schedule
     * @return
     * @throws ScmException
     */
    public static ScmTask getFirstTask( ScmSchedule schedule )
            throws ScmException {
        BSONObject orderBy = ScmQueryBuilder
                .start( ScmAttributeName.Task.START_TIME ).is( 1 ).get();
        List< ScmTask > tasks = schedule.getTasks( new BasicBSONObject(),
                orderBy, 0, 1 );
        return tasks.get( 0 );
    }

    /**
     * @descreption 清理工作区某个站点上数据源的空表
     * @param session
     * @param wsName
     * @throws ScmException
     */
    public static void cleanNullCS( ScmSession session, String wsName )
            throws Exception {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        ScmSpaceRecyclingTaskConfig conf = new ScmSpaceRecyclingTaskConfig();
        conf.setWorkspace( ws );
        conf.setRecycleScope( ScmSpaceRecycleScope.mothBefore( 0 ) );
        ScmId task = ScmSystem.Task.startSpaceRecyclingTask( conf );
        ScmTaskUtils.waitTaskFinish( session, task );
    }

    /**
     * @descreption 根据时间偏移offNum年构造预期的LobCS表名
     * @param wsp
     * @param site
     * @param beginTime
     * @param offNum
     * @return
     */
    public static Set< String > initLobCSName( WsWrapper wsp, SiteWrapper site,
            long beginTime, int offNum ) throws Exception {
        if ( site.getDataType() != ScmType.DatasourceType.SEQUOIADB ) {
            throw new Exception( "only supported SequoiaDB DataSource" );
        }
        Calendar instance = Calendar.getInstance();
        instance.setTime( new Date( beginTime ) );
        Set< String > LobCSNames = new HashSet<>();
        for ( int i = 0; i < offNum; i++ ) {
            String name = initCSNameByTimestamp( wsp, site,
                    instance.getTimeInMillis() );
            LobCSNames.add( name );
            instance.add( Calendar.YEAR, -1 );
        }
        return LobCSNames;
    }

    /**
     * @descreption 根据时间偏移offNum年构造预期的LobCS表名
     * @param wsName
     * @param shardingType
     * @param beginTime
     * @param csNum
     * @return
     * @throws Exception
     */
    public static Set< String > initLobCSName( String wsName,
            ScmShardingType shardingType, long beginTime, int csNum ) {
        Calendar instance = Calendar.getInstance();
        instance.setTime( new Date( beginTime ) );
        Set< String > LobCSNames = new HashSet<>();
        for ( int i = 0; i < csNum; i++ ) {
            String name = initCSNameByTimestamp( wsName, shardingType,
                    instance.getTimeInMillis() );
            LobCSNames.add( name );
            instance.add( Calendar.YEAR, -1 );
        }
        return LobCSNames;
    }

    /**
     * @descreption 根据时间构造预期的LobCS表名
     * @param wsp
     * @param site
     * @param timestamp
     * @return
     */
    public static String initCSNameByTimestamp( WsWrapper wsp, SiteWrapper site,
            long timestamp ) {
        String keyWord = "collection_space";
        BSONObject dataShardingType = wsp
                .getDataShardingType( site.getSiteId() );
        Calendar instance = Calendar.getInstance();
        instance.setTime( new Date( timestamp ) );
        int year = instance.get( Calendar.YEAR );
        int month = instance.get( Calendar.MONTH ) + 1;
        int quarter = ( month - 1 ) / 3 + 1;
        if ( dataShardingType == null ) {
            return wsp.getName() + "_LOB_" + year;
        } else {
            if ( dataShardingType.get( keyWord )
                    .equals( ScmShardingType.MONTH.getName() ) ) {
                return wsp.getName() + "_LOB_" + year
                        + String.format( "%02d", month );
            } else if ( dataShardingType.get( keyWord )
                    .equals( ScmShardingType.YEAR.getName() ) ) {
                return wsp.getName() + "_LOB_" + year;
            } else if ( dataShardingType.get( keyWord )
                    .equals( ScmShardingType.QUARTER.getName() ) ) {
                return wsp.getName() + "_LOB_" + year + "Q" + quarter;
            } else {
                return wsp.getName() + "_LOB";
            }
        }
    }

    /**
     * @descreption 根据时间构造预期的LobCS表名
     * @param wsName
     * @param dataShardingType
     * @param timestamp
     * @return
     */
    public static String initCSNameByTimestamp( String wsName,
            ScmShardingType dataShardingType, long timestamp ) {
        Calendar instance = Calendar.getInstance();
        instance.setTime( new Date( timestamp ) );
        int year = instance.get( Calendar.YEAR );
        int month = instance.get( Calendar.MONTH ) + 1;
        int quarter = ( month - 1 ) / 3 + 1;
        if ( dataShardingType.equals( ScmShardingType.MONTH ) ) {
            return wsName + "_LOB_" + year + String.format( "%02d", month );
        } else if ( dataShardingType.equals( ScmShardingType.YEAR ) ) {
            return wsName + "_LOB_" + year;
        } else if ( dataShardingType.equals( ScmShardingType.QUARTER ) ) {
            return wsName + "_LOB_" + year + "Q" + quarter;
        } else {
            return wsName + "_LOB";
        }
    }

    /**
     * @descreption 获取任务的ExtraInfo以数组返回
     * @param task
     * @return
     */
    public static Object[] getTaskExtraInfo( ScmTask task ) {
        String keyWord = "space_recycling_removed_collection_space";
        BSONObject extraInfo = task.getExtraInfo();
        BasicBSONList csNames = new BasicBSONList();
        if ( extraInfo != null ) {
            csNames = ( BasicBSONList ) extraInfo.get( keyWord );
        }
        return csNames.toArray();
    }

    /**
     * @descreption 用于检测用例执行时出现月份变更，如果在1分钟内会发送转月，则等待1分钟
     * @param instance
     * @throws InterruptedException
     */
    public static void checkMonthChange( Calendar instance )
            throws InterruptedException {
        int months = instance.get( Calendar.MONTH );
        instance.add( Calendar.SECOND, 1 );
        int monthsSeek1Sec = instance.get( Calendar.MONTH );
        instance.add( Calendar.SECOND, -1 );
        if ( monthsSeek1Sec != months ) {
            Thread.sleep( 60000 );
        }
    }
}
