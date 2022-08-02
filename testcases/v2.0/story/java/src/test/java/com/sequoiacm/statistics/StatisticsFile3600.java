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
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-3600:使用关闭的session,查询统计信息
 * @author fanyu
 * @Date:2021/03/30
 * @version:1.0
 */
public class StatisticsFile3600 extends TestScmBase {
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private Calendar calendar = null;
    private Date beginDate = null;
    private Date endDate = null;

    @BeforeClass
    private void setUp() throws Exception {
        calendar = Calendar.getInstance();
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        calendar.set( Calendar.YEAR, calendar.get( Calendar.YEAR ) + 1 );
        endDate = calendar.getTime();
        calendar.set( Calendar.YEAR, calendar.get( Calendar.YEAR ) - 1 );
        beginDate = calendar.getTime();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test1() throws Exception {
        ScmSession session = TestScmTools.createSession( site );
        session.close();
        // 查询上传接口统计信息
        try {
            ScmSystem.Statistics.fileStatistician( session )
                    .user( TestScmBase.scmUserName ).beginDate( beginDate )
                    .endDate( endDate ).workspace( wsp.getName() ).upload()
                    .get();
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.SESSION_CLOSED ) {
                throw e;
            }
        }

        // 查询下载接口统计信息
        try {
            ScmSystem.Statistics.fileStatistician( session )
                    .user( TestScmBase.scmUserName ).beginDate( beginDate )
                    .endDate( endDate ).workspace( wsp.getName() ).download()
                    .get();
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.SESSION_CLOSED ) {
                throw e;
            }
        }
    }

    @AfterClass
    private void tearDown() throws Exception {
    }
}