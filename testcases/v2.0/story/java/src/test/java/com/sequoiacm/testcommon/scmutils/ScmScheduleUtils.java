package com.sequoiacm.testcommon.scmutils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.*;
import com.sequoiacm.testcommon.*;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import org.apache.log4j.Logger;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.client.exception.ScmException;

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
     * @Descrip 获取success_count值大于1的任务
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
}
