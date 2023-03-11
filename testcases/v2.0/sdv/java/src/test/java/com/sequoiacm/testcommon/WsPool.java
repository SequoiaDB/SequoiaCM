package com.sequoiacm.testcommon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description ws 池子
 * @author fanyu
 * @version 1.00
 * @Date 2020/9/9
 */
public class WsPool {
    private static final int timeout = 2;
    private static final int newWsNum = 3;
    private static final int getWsMaxTimesCount = 10;
    private static ArrayBlockingQueue< String > resources;
    private static AtomicInteger count = new AtomicInteger( 0 );
    private static List< String > wsList = new CopyOnWriteArrayList<>();

    public static void init( HashMap< String, String > wsConfig )
            throws Exception {
        List< String > wsNames = createCommonWs( wsConfig );
        resources = new ArrayBlockingQueue<>( 12 );
        resources.addAll( wsNames );
        wsList.addAll( wsNames );
    }

    public static void destroy() throws Exception {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( ScmInfo.getSite() );
            ThreadExecutor threadExec = new ThreadExecutor();
            for ( String wsName : wsList ) {
                threadExec.addWorker(
                        new WsPool().new DropWS( session, wsName ) );
            }
            threadExec.run();
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class DropWS {
        private ScmSession session;
        private String wsName;

        public DropWS( ScmSession session, String wsName ) {
            this.session = session;
            this.wsName = wsName;
        }

        @ExecuteOrder(step = 1)
        private void created() throws Exception {
            ScmWorkspaceUtil.deleteWs( wsName, session );
        }
    }

    public static ScmWorkspace createWS( ScmSession session, String wsName,
            int siteNum, ScmShardingType dataLocationShardingType )
            throws ScmException, InterruptedException {
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setDataLocations( ScmWorkspaceUtil.getDataLocationList( siteNum,
                dataLocationShardingType ) );
        conf.setMetaLocation(
                ScmWorkspaceUtil.getMetaLocation( ScmShardingType.YEAR ) );
        conf.setName( wsName );
        conf.setEnableDirectory( true );
        return ScmWorkspaceUtil.createWS( session, conf );
    }

    private static List< String > createCommonWs(
            HashMap< String, String > wsOptions ) throws Exception {
        HashMap< String, String > wsConfig = new HashMap<>( wsOptions );
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( ScmInfo.getSite() );
            ScmCursor< ScmWorkspaceInfo > wsInfo = ScmFactory.Workspace
                    .listWorkspace( session );
            Set< String > keys = wsConfig.keySet();
            // 从待创建的工作区列表中移除环境中已存在的
            while ( wsInfo.hasNext() ) {
                ScmWorkspaceInfo scmWorkspaceInfo = wsInfo.getNext();
                String ws_Name = scmWorkspaceInfo.getName();
                keys.remove( ws_Name );
            }
            ThreadExecutor threadExec = new ThreadExecutor();
            // 创建公共工作区
            for ( String wsName : keys ) {
                threadExec.addWorker( new WsPool().new CreateWS( session,
                        wsName, wsConfig.get( wsName ) ) );
            }
            threadExec.run();
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
        return new ArrayList<>( wsConfig.keySet() );
    }

    private class CreateWS {
        private ScmSession session;
        private String wsName;
        private String type;

        public CreateWS( ScmSession session, String wsName, String type ) {
            this.session = session;
            this.wsName = wsName;
            this.type = type;
        }

        @ExecuteOrder(step = 1)
        private void created() throws ScmException, InterruptedException {
            if ( type != null ) {
                // null表示分区规则使用默认值
                createWS( session, wsName, ScmInfo.getSiteNum(),
                        ScmShardingType.getShardingType( type ) );
            } else {
                ScmWorkspaceUtil.createDisEnableDirectoryWS( session, wsName,
                        ScmInfo.getSiteNum() );
            }
            ScmWorkspaceUtil.wsSetPriority( session, wsName );
        }
    }
}
