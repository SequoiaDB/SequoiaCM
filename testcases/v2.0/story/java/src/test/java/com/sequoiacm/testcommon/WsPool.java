package com.sequoiacm.testcommon;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.BasicBSONObject;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
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
    private static final int newWsNum = 3;
    private static ArrayBlockingQueue< String > resources;
    private static AtomicInteger count = new AtomicInteger( 0 );
    private static List< String > wsList = new CopyOnWriteArrayList<>();

    public static void init( List< String > list ) throws ScmException {
        resources = new ArrayBlockingQueue<>( 12 );
        resources.addAll( list );
        wsList.addAll( list );
    }

    public static String get() throws Exception {
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

    public static void release( String wsName ) {
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
            session = TestScmTools.createSession( ScmInfo.getSite() );
            ThreadExecutor threadExec = new ThreadExecutor();
            for ( String wsName : wsList ) {
                if ( getWsIndexStatus( wsName ).equals( ScmFulltextStatus.NONE )
                        && isWsEmpty( wsName ) ) {
                    threadExec.addWorker(
                            new WsPool().new DropWS( session, wsName ) );
                }
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
            session = TestScmTools.createSession( ScmInfo.getRootSite() );
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
            session = TestScmTools.createSession( ScmInfo.getRootSite() );
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
            session = TestScmTools.createSession( ScmInfo.getSite() );
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
            throws ScmException, UnknownHostException, InterruptedException {
        String wsName = TestScmBase.FULLTEXT_WS_PREFIX + InetAddress.getLocalHost().getHostName().replace( "-",
                "_" ) + "_new_" + count.get();
         ScmSession session = null;
        try {
             session = TestScmTools.createSession( ScmInfo.getSite() );
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
}
