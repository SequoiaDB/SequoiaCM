/**
 *
 */
package com.sequoiacm.testcommon.scmutils;

import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

import org.bson.BSONObject;

import com.sequoiacm.client.common.ScmType.StatisticsType;
import com.sequoiacm.client.common.TrafficType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmStatisticsFileDelta;
import com.sequoiacm.client.core.ScmStatisticsTraffic;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.TestScmBase;

/**
 * @Description tatisticsUtils.java
 * @author wuyan
 * @date 2018.9.13
 */
public class StatisticsUtils extends TestScmBase {

    // get the timestamp of the Day,eg "2018-09-12" is 1536681600000
    public static long getTimestampOfTheDay() {
        Date date = new Date();
        SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd" );
        String nowdate = df.format( date );
        long currentTimestamp = 0;
        try {
            currentTimestamp = df.parse( nowdate ).getTime();
        } catch ( ParseException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return currentTimestamp;
    }

    public static HashMap< String, Long > statisticsFile( ScmWorkspace ws,
            ScmSession session ) throws ScmException {
        long currentTimestamp = getTimestampOfTheDay();
        ScmSystem.Statistics.refresh( session, StatisticsType.TRAFFIC,
                ws.getName() );
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.Traffic.WORKSPACE_NAME )
                .is( ws.getName() ).put( ScmAttributeName.Traffic.RECORD_TIME )
                .is( currentTimestamp ).get();

        ScmCursor< ScmStatisticsTraffic > cursor = ScmSystem.Statistics
                .listTraffic( session, cond );
        HashMap< String, Long > map = new HashMap< String, Long >();
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmStatisticsTraffic trafficInfo = cursor.getNext();
            TrafficType type = trafficInfo.getType();
            long traffic = trafficInfo.getTraffic();
            if ( type.getName().equals( "file_upload" ) ) {
                map.put( "file_upload", traffic );
            } else if ( type.getName().equals( "file_download" ) ) {
                map.put( "file_download", traffic );
            }
            map.put( type.toString(), traffic );
            size++;
        }
        cursor.close();

        // no statistics ,the traffic is 0
        if ( size == 0 ) {
            long traffic = 0;
            map.put( "file_upload", traffic );
            map.put( "file_download", traffic );
        }
        return map;
    }

    public static HashMap< String, Long > statisticsFileDelta( ScmWorkspace ws,
            ScmSession session ) throws ScmException {
        long currentTimestamp = getTimestampOfTheDay();
        ScmSystem.Statistics.refresh( session, StatisticsType.FILE_DELTA,
                ws.getName() );
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.Traffic.WORKSPACE_NAME )
                .is( ws.getName() )
                .put( ScmAttributeName.FileDelta.RECORD_TIME )
                .is( currentTimestamp ).get();

        ScmCursor< ScmStatisticsFileDelta > cursor = ScmSystem.Statistics
                .listFileDelta( session, cond );
        HashMap< String, Long > map = new HashMap< String, Long >();
        long count_delta = 0;
        long size_delta = 0;
        while ( cursor.hasNext() ) {
            ScmStatisticsFileDelta traffic = cursor.getNext();
            count_delta = traffic.getCountDelta();
            size_delta = traffic.getSizeDelta();
        }
        cursor.close();

        map.put( "count_delta", count_delta );
        map.put( "size_delta", size_delta );
        return map;
    }

    /**
     * create File by stream
     *
     * @param ws
     * @param fileName
     * @param data
     * @param authorName
     * @throws ScmException
     */
    public static ScmId createFileByStream( ScmWorkspace ws, String fileName,
            byte[] data, String authorName ) throws ScmException {
        return createFileByStream( ws, fileName, data, authorName, 0 );
    }

    public static ScmId createFileByStream( ScmWorkspace ws, String fileName,
            byte[] data, String authorName, long timestamp )
            throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        new Random().nextBytes( data );
        file.setContent( new ByteArrayInputStream( data ) );
        file.setFileName( fileName );
        file.setAuthor( authorName );
        if ( timestamp != 0 ) {
            Date date = new Date( timestamp );
            file.setCreateTime( date );
        }

        ScmId fileId = file.save();
        return fileId;
    }
}
