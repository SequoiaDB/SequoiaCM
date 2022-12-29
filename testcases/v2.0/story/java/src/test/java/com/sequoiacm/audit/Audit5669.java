package com.sequoiacm.audit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.element.ScmUpdateConfResult;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.config.ConfigCommonDefind;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @descreption SCM-5669:并发修改相同节点配置相同配置项
 * @author YiPan
 * @date 2022/12/28
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Audit5669 extends TestScmBase {
    private final String newUserName = "user5669";
    private ScmSession session = null;
    private boolean runSuccess = false;
    private final Map< String, String > DQLMap = new HashMap<>();
    private final Map< String, String > DMLMap = new HashMap<>();

    @BeforeClass
    public void setUp() throws Exception {
        session = TestScmTools.createSession( ScmInfo.getSite() );
        ConfUtil.deleteAuditConf( ConfUtil.AUTH_SERVER_SERVICE_NAME );
        ScmAuthUtils.deleteUser( session, newUserName );
        init();
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( new UpdateConfig( DQLMap ) );
        t.addWorker( new UpdateConfig( DMLMap ) );
        t.run();

        ScmFactory.User.createUser( session, newUserName,
                ScmUserPasswordType.TOKEN, newUserName );
        boolean flag = ConfUtil.checkAuditByType( session, "CREATE_USER",
                "userName=" + newUserName );
        if ( !flag ) {
            ScmFactory.User.getUser( session, newUserName );
            Assert.assertTrue( ConfUtil.checkAuditByType( session, "USER_DQL",
                    "userName=" + newUserName ) );
        }
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                ScmAuthUtils.deleteUser( session, newUserName );
                ConfUtil.deleteAuditConf( ConfUtil.AUTH_SERVER_SERVICE_NAME );
            }
        } finally {
            session.close();
        }
    }

    private void init() {
        DQLMap.put( com.sequoiacm.audit.ConfigCommonDefind.scm_audit_userMask,
                "LOCAL" );
        DMLMap.put( com.sequoiacm.audit.ConfigCommonDefind.scm_audit_userMask,
                "LOCAL" );
        DQLMap.put( ConfigCommonDefind.scm_audit_mask, "USER_DQL" );
        DMLMap.put( ConfigCommonDefind.scm_audit_mask, "USER_DML" );
    }

    private class UpdateConfig {
        private Map< String, String > confMap;

        public UpdateConfig( Map< String, String > confMap ) {
            this.confMap = confMap;
        }

        @ExecuteOrder(step = 1)
        private void run() throws ScmException {
            ScmUpdateConfResultSet scmUpdateConfResultSet = ConfUtil
                    .updateConf( ConfUtil.AUTH_SERVER_SERVICE_NAME, confMap );
            List< ScmUpdateConfResult > successes = scmUpdateConfResultSet
                    .getSuccesses();
            if ( successes.size() > 0 ) {
                Assert.assertEquals( successes.get( 0 ).getServiceName(),
                        "AUTH-SERVER" );
            } else {
                Assert.fail( scmUpdateConfResultSet.toString() );
            }
        }
    }
}