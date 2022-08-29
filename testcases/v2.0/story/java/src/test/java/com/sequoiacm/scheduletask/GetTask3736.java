package com.sequoiacm.scheduletask;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @description SCM-3736:getTask接口指定orederby、skip和limit参数获取task信息
 * @author YiPan
 * @createDate 2021.8.26
 * @updateUser YiPan
 * @updateDate 2021.8.26
 * @updateRemark
 * @version v1.0
 */
public class GetTask3736 extends TestScmBase {
    private String fileName = "file3736";
    private ScmSession session;
    private ScmSchedule schedule;
    private SiteWrapper branchSite;
    private SiteWrapper rootSite;
    private SiteWrapper cleanSite;
    private WsWrapper wsp;
    private boolean runSuccess = false;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        wsp = ScmInfo.getWs();
        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        cleanSite = ScmScheduleUtils.getSortBranchSites().get( 0 );
        session = TestScmTools.createSession( rootSite );
    }

    @Test(groups = { "fourSite", "star", GroupTags.base })
    public void test() throws Exception {
        schedule = ScmScheduleUtils.createSchedule( session, branchSite,
                rootSite, cleanSite, wsp, fileName );
        // start_time
        orderByStart_time( 1, -1, 5 );
        orderByStart_time( -1, -1, 5 );

        // target_Site
        orderByType( 1, 5, 10 );
        orderByType( -1, 5, 10 );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmSystem.Schedule.delete( session, schedule.getId() );
                ScmScheduleUtils.cleanTask( session, schedule.getId() );
            } finally {
                session.close();
            }
        }
    }

    private void orderByStart_time( int sort, int skip, int limit )
            throws ScmException {
        List< Date > actStart_times = new ArrayList<>();
        List< Date > allStart_times = new ArrayList<>();
        List< ScmTask > tasks = schedule.getTasks( new BasicBSONObject(),
                new BasicBSONObject(), 0, -1 );
        Assert.assertNotEquals( tasks, 0 );
        for ( int i = 0; i < tasks.size(); i++ ) {
            allStart_times.add( tasks.get( i ).getStartTime() );
        }
        tasks = schedule.getTasks( new BasicBSONObject(),
                new BasicBSONObject( ScmAttributeName.Task.START_TIME, sort ),
                skip, limit );
        for ( int i = 0; i < tasks.size(); i++ ) {
            actStart_times.add( tasks.get( i ).getStartTime() );
        }
        Collections.sort( allStart_times );
        if ( sort == -1 ) {
            Collections.reverse( allStart_times );
        }
        if ( skip < 0 ) {
            skip = 0;
        }
        List< Date > expStart_times = allStart_times.subList( skip,
                skip + limit );
        Assert.assertEquals( actStart_times, expStart_times );
    }

    private void orderByType( int sort, int skip, int limit )
            throws ScmException {
        List< Integer > actTypes = new ArrayList<>();
        List< Integer > allTypes = new ArrayList<>();
        List< ScmTask > tasks = schedule.getTasks( new BasicBSONObject(),
                new BasicBSONObject(), 0, -1 );
        Assert.assertNotEquals( tasks, 0 );
        for ( int i = 0; i < tasks.size(); i++ ) {
            allTypes.add( tasks.get( i ).getType() );
        }
        tasks = schedule.getTasks( new BasicBSONObject(),
                new BasicBSONObject( ScmAttributeName.Task.TYPE, sort ), skip,
                limit );
        for ( int i = 0; i < tasks.size(); i++ ) {
            actTypes.add( tasks.get( i ).getType() );
        }
        Collections.sort( allTypes );
        if ( sort == -1 ) {
            Collections.reverse( allTypes );
        }
        if ( skip < 0 ) {
            skip = 0;
        }
        List< Integer > expTypes = allTypes.subList( skip, skip + limit );
        Assert.assertEquals( actTypes, expTypes );
    }
}