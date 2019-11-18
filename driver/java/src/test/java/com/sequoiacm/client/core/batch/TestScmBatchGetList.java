//package com.sequoiacm.client.core.batch;
//
//import com.sequoiacm.client.core.*;
//
//import org.bson.BasicBSONObject;
//import org.testng.annotations.Test;
//
//import com.sequoiacm.client.common.ScmType.SessionType;
//import com.sequoiacm.client.exception.ScmException;
//import com.sequoiacm.client.util.ScmTestBase;
//
//public class TestScmBatchGetList extends ScmTestBase {
//
//    @Test
//    public static void testGetList() throws ScmException {
//        ScmSession session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
//                new ScmConfigOption(url, user, password));
//        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace("ws_default", session);
//
//        // Batch
//        BasicBSONObject matcher = new BasicBSONObject();
////        matcher.put("name", "batch-new2");
//        matcher.put("create_user", "create-user");
//        ScmCursor<ScmBatchInfo> cursor = ScmFactory.Batch.listInstance(ws, matcher);
//        while (cursor.hasNext()) {
//            ScmBatchInfo batchInfo = cursor.getNext();
//            System.out.println(batchInfo);
//        }
//        session.close();
//    }
//}
