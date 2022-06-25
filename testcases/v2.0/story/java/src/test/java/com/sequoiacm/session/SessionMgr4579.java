package com.sequoiacm.session;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @descreption SCM-4579:用户创建session池，指定超时时间
 * @author YiPan
 * @date 2022/6/23
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class SessionMgr4579 extends TestScmBase {
    private ScmSessionMgr sessionMgr;
    private int connectionRequestTimeout = 2000;
    private ScmSessionPoolConf scmSessionPoolConf;
    private ScmSession session;

    @BeforeClass
    private void setUp() throws ScmException {
        ScmConfigOption scmConfigOption = TestScmTools
                .getScmConfigOption( ScmInfo.getRootSite().getSiteName() );
        scmSessionPoolConf = ScmSessionPoolConf.builder()
                .setSessionConfig( scmConfigOption ).get();

    }

    @Test
    private void test() throws ScmException {
        // 设置connectionRequestTimeout为无效值
        try {
            scmSessionPoolConf.setConnectionRequestTimeout( -1 );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.INVALID_ARGUMENT ) ) {
                throw e;
            }
        }

        // 设置connectionRequestTimeout为有效值创建，调小最大连接数为2
        scmSessionPoolConf
                .setConnectionRequestTimeout( connectionRequestTimeout );
        scmSessionPoolConf.setMaxConnections( 2 );
        sessionMgr = ScmFactory.Session.createSessionMgr( scmSessionPoolConf );

        // 获取session，创建满连接(列取文件不关游标可以占用连接)
        session = sessionMgr.getSession();
        ScmCursor< ScmFileBasicInfo > cursor1 = listFile();
        ScmCursor< ScmFileBasicInfo > cursor2 = listFile();

        // 校验实际超时时间>=connectionRequestTimeout
        long start = System.currentTimeMillis();
        try {
            ScmFactory.Workspace.listWorkspace( session );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.NETWORK_IO ) ) {
                throw e;
            }
            long end = System.currentTimeMillis();
            if ( ( end - start ) < connectionRequestTimeout ) {
                Assert.fail( "act timeOut=" + ( end - start ) );
            }
        }
        cursor1.close();
        cursor2.close();
    }

    @AfterClass
    private void tearDown() {
        session.close();
        sessionMgr.close();
    }

    private ScmCursor< ScmFileBasicInfo > listFile() throws ScmException {
        ScmWorkspace ws = ScmFactory.Workspace
                .getWorkspace( ScmInfo.getWs().getName(), session );
        return ScmFactory.File.listInstance( ws, ScmType.ScopeType.SCOPE_ALL,
                new BasicBSONObject() );
    }
}
