//package com.sequoiacm.testcommon.scmutils;
//
//import java.util.*;
//import java.util.concurrent.TimeoutException;
//
//import org.testng.Assert;
//
//import com.alibaba.fastjson.JSON;
//import com.sequoiacm.client.common.ScmType;
//import com.sequoiacm.client.core.ScmCursor;
//import com.sequoiacm.client.core.ScmFactory;
//import com.sequoiacm.client.core.ScmSession;
//import com.sequoiacm.client.core.ScmWorkspace;
//import com.sequoiacm.client.element.ScmFileBasicInfo;
//import com.sequoiacm.client.element.ScmTags;
//import com.sequoiacm.client.element.tag.ScmTagCondition;
//import com.sequoiacm.client.exception.ScmException;
//import com.sequoiacm.common.ScmWorkspaceTagRetrievalStatus;
//import com.sequoiacm.exception.ScmError;
//
///**
// * @author
// * @descreption
// * @date
// * @updateUser
// * @updateDate 2023/5/18
// * @updateRemark
// */
//public class TagRetrievalUtils {
//
//    public static final String SCM_SYSTEM_CS = "SCMSYSTEM";
//    public static final String GLOBAL_CONFIG_CL = "GLOBAL_CONFIG";
//
//
//    public static ScmTags createTag( String... tags ) throws ScmException {
//        ScmTags scmTags = new ScmTags();
//        for ( String tag : tags ) {
//            scmTags.addTag( tag );
//        }
//        return scmTags;
//    }
//
//    public static HashMap< String, String > createCustomTag( String str ) {
//        Map customTag = JSON.parseObject( str, Map.class );
//        return ( HashMap< String, String > ) customTag;
//    }
//
//    public static void checkSearchFileList(
//            ScmCursor< ScmFileBasicInfo > scmCursor, String... fileNameList )
//            throws ScmException {
//        Set< String > actFileList = new HashSet<>();
//        Set< String > expFileList = new HashSet<>(
//                Arrays.asList( fileNameList ) );
//        while ( scmCursor.hasNext() ) {
//            ScmFileBasicInfo fileInfo = scmCursor.getNext();
//            actFileList.add( fileInfo.getFileName() );
//        }
//        scmCursor.close();
//        Assert.assertEquals( actFileList, expFileList );
//    }
//
//    public static void waitFileTagBuild( ScmWorkspace ws, ScmTagCondition cond,
//                                         long expCountFile ) throws ScmException, InterruptedException {
//        long actCountFile = -1;
//        int i = 0;
//        do {
//            try {
//                actCountFile = ScmFactory.Tag.countFile( ws,
//                        ScmType.ScopeType.SCOPE_ALL, cond, null );
//            } catch ( ScmException e ) {
//                if ( !e.getError().equals( ScmError.METASOURCE_ERROR ) ) {
//                    throw e;
//                }
//            }
//            i++;
//            Thread.sleep( 1000 );
//            if ( i > 60 ) {
//                Assert.fail( "countFile超时,countFile=" + actCountFile );
//            }
//        } while ( actCountFile != expCountFile );
//    }
//
//    /**
//     * @descreption 有限时间内等待工作区标签检索变成 waitStatus 状态
//     * @param ws
//     * @param waitStatus
//     * @param second
//     * @return
//     * @throws ScmException
//     */
//    public static void waitWsTagRetrievalStatus( ScmWorkspace ws,
//                                                 ScmWorkspaceTagRetrievalStatus waitStatus, int second )
//            throws ScmException, InterruptedException {
//        long waitCont = 0;
//        while ( true ) {
//            ScmWorkspaceTagRetrievalStatus status = ws.getTagRetrievalStatus();
//            if ( waitStatus.getValue().equals( status.getValue() ) ) {
//                break;
//            }
//            if ( waitCont >= second ) {
//                Assert.fail( "wait time out get tagRetrieval status "
//                        + waitStatus.getValue() );
//            }
//            Thread.sleep( 1000 );
//            waitCont++;
//        }
//    }
//
//    public static void waitForTagRetrievalStatus(ScmSession session, String wsName,
//                                                 int timeoutInSeconds, ScmWorkspaceTagRetrievalStatus exceptstatus )
//            throws TimeoutException, InterruptedException, ScmException {
//        long startTime = System.currentTimeMillis();
//        long endTime = startTime + ( timeoutInSeconds * 1000 );
//
//        while ( System.currentTimeMillis() < endTime ) {
//            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,session);
//            if ( ws.getTagRetrievalStatus().equals( exceptstatus ) ) {
//                return; // 期望的状态已经达到，方法返回
//            }
//        }
//        // 超时异常，抛出 TimeoutException
//        throw new TimeoutException(
//                "Timeout waiting for tag retrieval status to become " + exceptstatus );
//    }
//
//}
