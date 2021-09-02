package com.sequoiacm.scheduletask;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
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
 * @description SCM-3733:getTasks接口指定orderby参数获取task信息
 * @author YiPan
 * @createDate 2021.8.26
 * @updateUser YiPan
 * @updateDate 2021.8.26
 * @updateRemark
 * @version v1.0
 */
public class GetTask3733 extends TestScmBase {
    private String fileName = "file3733";
    private ScmSession session;
    private ScmSchedule schedule;
    private SiteWrapper branchSite;
    private SiteWrapper rootSite;
    private SiteWrapper cleanSite;
    private WsWrapper wsp;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        wsp = ScmInfo.getWs();
        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        cleanSite = ScmScheduleUtils.getSortBranchSites().get( 0 );
        session = TestScmTools.createSession( rootSite );
    }

    @Test(groups = { "fourSite" })
    public void test() throws Exception {
        schedule = ScmScheduleUtils.createSchedule( session, branchSite,
                rootSite, cleanSite, wsp, fileName );
        // id字段
        orderById( 1 );
        orderById( -1 );

        // type字段
        orderByType( 1 );
        orderByType( -1 );

        // Date类型
        orderByDate( 1 );
        orderByDate( -1 );
        runSuccess = true;
    }

    @AfterClass
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

    private void orderById( int sort ) throws ScmException {
        List< String > actIds = new ArrayList<>();
        List< String > expIds = new ArrayList<>();
        List< ScmTask > tasks = schedule.getTasks( new BasicBSONObject(),
                new BasicBSONObject( ScmAttributeName.Task.ID, sort ), 0, -1 );
        Assert.assertNotEquals( tasks, 0 );
        for ( int i = 0; i < tasks.size(); i++ ) {
            actIds.add( tasks.get( i ).getId().get() );
        }
        expIds.addAll( actIds );
        Collections.sort( expIds );
        if ( sort == -1 ) {
            Collections.reverse( expIds );
        }
        Assert.assertEquals( actIds, expIds );
    }

    private void orderByType( int sort ) throws ScmException {
        List< Integer > actTypes = new ArrayList<>();
        List< Integer > expTypes = new ArrayList<>();
        List< ScmTask > tasks = schedule.getTasks( new BasicBSONObject(),
                new BasicBSONObject( ScmAttributeName.Task.TYPE, sort ), 0,
                -1 );
        Assert.assertNotEquals( tasks, 0 );
        for ( int i = 0; i < tasks.size(); i++ ) {
            actTypes.add( tasks.get( i ).getType() );
        }
        expTypes.addAll( actTypes );
        Collections.sort( expTypes );
        if ( sort == -1 ) {
            Collections.reverse( expTypes );
        }
        Assert.assertEquals( actTypes, expTypes );
    }

    private void orderByDate( int sort ) throws ScmException {
        List< Date > actStart_times = new ArrayList<>();
        List< Date > expStart_times = new ArrayList<>();
        List< ScmTask > tasks = schedule.getTasks( new BasicBSONObject(),
                new BasicBSONObject( ScmAttributeName.Task.START_TIME, sort ),
                0, -1 );
        Assert.assertNotEquals( tasks, 0 );
        for ( int i = 0; i < tasks.size(); i++ ) {
            actStart_times.add( tasks.get( i ).getStartTime() );
        }
        expStart_times.addAll( actStart_times );
        Collections.sort( expStart_times );
        if ( sort == -1 ) {
            Collections.reverse( expStart_times );
        }
        Assert.assertEquals( actStart_times, expStart_times );
    }
}