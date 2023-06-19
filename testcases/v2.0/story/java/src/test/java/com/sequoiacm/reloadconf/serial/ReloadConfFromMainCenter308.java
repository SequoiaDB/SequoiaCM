package com.sequoiacm.reloadconf.serial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ServerScope;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;

/**
 * @Testcase: SCM-308:配置未变更，刷新配置（从主中心）
 * @author huangxiaoni init
 * @date 2017.5.26
 */

public class ReloadConfFromMainCenter308 extends TestScmBase {
    private static String fileName = "ReloadConfFromMainCenter308";
    private static SiteWrapper site = null;
    private static NodeWrapper node = null;
    private static WsWrapper wsp = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        wsp = ScmInfo.getWs();
        site = ScmInfo.getSite();
        node = site.getNode();
    }

    // SEQUOIACM-1364
    @Test(groups = { GroupTags.base }, enabled = false)
    private void testReloadBizConfFromMainCenter() throws Exception {
        ScmSession session = null;
        try {
            ScmConfigOption scOpt = new ScmConfigOption(
                    node.getHost() + ":" + node.getRestPort() );
            session = ScmFactory.Session
                    .createSession( SessionType.NOT_AUTH_SESSION, scOpt );
            List< BSONObject > list = ScmSystem.Configuration.reloadBizConf(
                    ServerScope.ALL_SITE, node.getSiteId(), session );

            // check results
            List< NodeWrapper > expNodeList = ScmInfo.getNodeList();
            String errStr = "reloadBizConf failed, actual infoList after "
                    + "reloadBizConf: \n" + list + "expect nodeInfo: \n"
                    + expNodeList;

            Assert.assertEquals( list.size(), expNodeList.size(), errStr );

            // compare node id
            List< Integer > serverIdList = new ArrayList<>();
            List< Integer > expServerIdList = new ArrayList<>();
            for ( int i = 0; i < list.size(); i++ ) {
                Object errormsg = list.get( i ).get( "errormsg" );
                Assert.assertEquals( errormsg, "", errStr );
                int nodeId = ( int ) list.get( i ).get( "server_id" );
                serverIdList.add( nodeId );
                expServerIdList.add( expNodeList.get( i ).getId() );
            }
            Collections.sort( serverIdList );
            Collections.sort( expServerIdList );
            Assert.assertEquals( serverIdList, expServerIdList );
            this.bizOperator();
        } finally {
            if ( session != null )
                session.close();
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
    }

    private void bizOperator() throws ScmException {
        ScmSession session = null;
        ScmWorkspace ws = null;
        try {
            session = ScmSessionUtils.createSession();
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName + "_" + UUID.randomUUID() );
            ScmId fileId = file.save();

            ScmFactory.File.deleteInstance( ws, fileId, true );
        } finally {
            if ( session != null )
                session.close();
        }
    }

}