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
 * @Description: SCM-3599:非鉴权用户查询上传/下载接口统计信息
 * @author fanyu
 * @Date:2021/03/30
 * @version:1.0
 */
public class StatisticsFile3599 extends TestScmBase {
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private Calendar calendar = null;
    private Date beginDate = null;
    private Date endDate = null;

    @BeforeClass
    private void setUp() throws Exception {
        calendar = Calendar.getInstance();
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createNoAuthSession( site );
    }

    @Test
    private void test() throws Exception {
        calendar.set( Calendar.YEAR, calendar.get( Calendar.YEAR ) + 1 );
        endDate = calendar.getTime();
        calendar.set( Calendar.YEAR, calendar.get( Calendar.YEAR ) - 1 );
        beginDate = calendar.getTime();

        // 查询上传接口统计信息
        try {
            ScmSystem.Statistics.fileStatistician( session )
                    .user( TestScmBase.scmUserName ).beginDate( beginDate )
                    .endDate( endDate ).workspace( wsp.getName() ).upload()
                    .get();
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_FORBIDDEN ) {
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
            if ( e.getError() != ScmError.HTTP_FORBIDDEN ) {
                throw e;
            }
        }
    }

    @AfterClass
    private void tearDown() throws Exception {
        if ( session != null ) {
            session.close();
        }
    }
}