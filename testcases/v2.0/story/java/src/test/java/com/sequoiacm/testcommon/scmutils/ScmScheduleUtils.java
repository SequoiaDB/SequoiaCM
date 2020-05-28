package com.sequoiacm.testcommon.scmutils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.element.ScmTaskBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestSdbTools;

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
                        "the time difference between the schedule host and the scm host is more than "
                                + maxDiffTime + "." );
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
}
