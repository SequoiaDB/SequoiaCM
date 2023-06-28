package com.sequoiacm.testcommon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.BasicBSONObject;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltexInfo;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
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
    private static final int newWsNum = 15;
    private static final int getWsMaxTimesCount = 20;
    private static ArrayBlockingQueue< String > resources;
    private static AtomicInteger count = new AtomicInteger( 0 );
    private static List< String > wsList = new CopyOnWriteArrayList<>();

    public static void init( HashMap< String, String > wsConfig )
            throws Exception {
        List< String > wsNames = createCommonWs( wsConfig );
        resources = new ArrayBlockingQueue<>( wsNames.size() + newWsNum );
        resources.addAll( wsNames );
        resources.remove( TestScmBase.s3WorkSpaces );
        wsList.addAll( wsNames );
    }

    public synchronized static String get() throws Exception {
        int TimesCount = 0;
        while ( true ) {
            String resource = resources.poll( timeout, TimeUnit.SECONDS );
            if ( resource != null ) {
                System.out.println( "get = " + resource );
                if ( getWsIndexStatus( resource ).equals(
                        ScmFulltextStatus.NONE ) && isWsEmpty( resource ) ) {
                    return resource;
                }
                if ( getWsIndexStatus( resource ).equals(
                        ScmFulltextStatus.CREATED ) && isWsEmpty( resource ) ) {
                    dropIndex( resource );
                    return resource;
                }
                resources.offer( resource, timeout, TimeUnit.SECONDS );
                TimesCount++;
                if ( count.incrementAndGet() <= newWsNum ) {
                    return createNewWs();
                }
                if ( TimesCount > getWsMaxTimesCount ) {
                    throw new Exception(
                            "get a workspaces times count should not exceed "
                                    + getWsMaxTimesCount );
                }
            } else {
                // create new resource
                if ( count.incrementAndGet() > newWsNum ) {
                    throw new Exception(
                            "to prevent disk filling, the number of new workspaces should not exceed "
                                    + newWsNum );
                }
                return createNewWs();
            }
        }
    }

    public synchronized static void release( String wsName ) {
        System.out.println( "release = " + wsName );
        try {
            if ( wsName != null ) {
                resources.offer( wsName, timeout, TimeUnit.SECONDS );
            }
        } catch ( InterruptedException e ) {
            e.printStackTrace();
        }
    }

    public static void destroy() throws Exception {
        ScmSession session = null;
        try {
            session = ScmSessionUtils.createSession( ScmInfo.getSite() );
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

    private static void dropIndex( String wsName ) throws Exception {
        ScmSession session = null;
        try {
            session = ScmSessionUtils.createSession( ScmInfo.getRootSite() );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            ScmFactory.Fulltext.dropIndex( ws );
            FullTextUtils.waitWorkSpaceIndexStatus( ws,
                    ScmFulltextStatus.NONE );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }

    }

    private static ScmFulltextStatus getWsIndexStatus( String wsName )
            throws ScmException {
        ScmSession session = null;
        try {
            session = ScmSessionUtils.createSession( ScmInfo.getRootSite() );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            ScmFulltexInfo indexInfo = ScmFactory.Fulltext.getIndexInfo( ws );
            return indexInfo.getStatus();
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private static boolean isWsEmpty( String wsName ) throws ScmException {
        ScmSession session = null;
        try {
            session = ScmSessionUtils.createSession( ScmInfo.getSite() );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            long count = ScmFactory.File.countInstance( ws,
                    ScmType.ScopeType.SCOPE_ALL, new BasicBSONObject() );
            return count == 0;
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private static String createNewWs()
            throws ScmException, InterruptedException {
        String wsName = "ws_pool" + "_new_" + count.get();
        ScmSession session = null;
        try {
            session = ScmSessionUtils.createSession( ScmInfo.getSite() );
            ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
            ScmWorkspaceUtil.wsSetPriority( session, wsName );
            wsList.add( wsName );
            return wsName;
        } finally {
            if ( session != null ) {
                session.close();
            }
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
        try ( ScmSession session = ScmSessionUtils
                .createSession( ScmInfo.getSite() ) ;
                ScmCursor< ScmWorkspaceInfo > wsInfo = ScmFactory.Workspace
                        .listWorkspace( session ) ;) {
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
