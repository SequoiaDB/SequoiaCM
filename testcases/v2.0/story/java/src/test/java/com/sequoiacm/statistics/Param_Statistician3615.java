package com.sequoiacm.statistics;

import java.util.Calendar;
import java.util.Date;

import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.statistics.common.ScmTimeAccuracy;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;

/**
 * @Description: SCM-3615:fileStatistician接口参数校验
 * @author fanyu
 * @Date:2021/04/02
 * @version:1.0
 */

public class Param_Statistician3615 extends TestScmBase {
    private SiteWrapper site;
    private ScmSession session;
    private Calendar calendar = null;
    private Date endDate = null;
    private Date beginDate = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        calendar = Calendar.getInstance();
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        calendar.set( Calendar.HOUR_OF_DAY,
                calendar.get( Calendar.HOUR_OF_DAY ) + 1 );
        endDate = calendar.getTime();
        calendar.set( Calendar.HOUR_OF_DAY,
                calendar.get( Calendar.HOUR_OF_DAY ) - 10 );
        beginDate = calendar.getTime();
    }

    @Test
    private void test1() throws ScmException {
        // session为null
        try {
            ScmSystem.Statistics.fileStatistician( null )
                    .user( TestScmBase.scmUserName ).beginDate( beginDate )
                    .endDate( endDate ).timeAccuracy( ScmTimeAccuracy.DAY )
                    .upload().get();
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        // 不调用upload或者download接口
        try {
            ScmSystem.Statistics.fileStatistician( session )
                    .user( TestScmBase.scmUserName ).beginDate( beginDate )
                    .endDate( endDate ).timeAccuracy( ScmTimeAccuracy.DAY )
                    .get();
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @Test
    private void test2() throws ScmException {
        // begin小于end
        try {
            ScmSystem.Statistics.fileStatistician( session )
                    .user( TestScmBase.scmUserName ).beginDate( endDate )
                    .endDate( beginDate ).timeAccuracy( ScmTimeAccuracy.DAY )
                    .upload().get();
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        // begin为null
        try {
            ScmSystem.Statistics.fileStatistician( session )
                    .user( TestScmBase.scmUserName ).beginDate( null )
                    .endDate( endDate ).timeAccuracy( ScmTimeAccuracy.DAY )
                    .upload().get();
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        // 不调用begin
        try {
            ScmSystem.Statistics.fileStatistician( session )
                    .user( TestScmBase.scmUserName ).endDate( endDate )
                    .timeAccuracy( ScmTimeAccuracy.DAY ).upload().get();
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @Test
    private void test3() throws ScmException {
        // end为null
        try {
            ScmSystem.Statistics.fileStatistician( session )
                    .user( TestScmBase.scmUserName ).beginDate( beginDate )
                    .endDate( null ).timeAccuracy( ScmTimeAccuracy.DAY )
                    .upload().get();
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        // 不调用end
        try {
            ScmSystem.Statistics.fileStatistician( session )
                    .user( TestScmBase.scmUserName ).beginDate( endDate )
                    .timeAccuracy( ScmTimeAccuracy.DAY ).upload().get();
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        if ( session != null ) {
            session.close();
        }
    }
}
