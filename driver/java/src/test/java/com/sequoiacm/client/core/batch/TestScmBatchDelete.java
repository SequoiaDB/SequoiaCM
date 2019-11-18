//package com.sequoiacm.client.core.batch;
//
//import org.testng.annotations.Test;
//
//import com.sequoiacm.client.common.ScmType.SessionType;
//import com.sequoiacm.client.core.ScmBatch123;
//import com.sequoiacm.client.core.ScmConfigOption;
//import com.sequoiacm.client.core.ScmFactory;
//import com.sequoiacm.client.core.ScmSession;
//import com.sequoiacm.client.core.ScmWorkspace;
//import com.sequoiacm.client.element.ScmId;
//import com.sequoiacm.client.exception.ScmException;
//import com.sequoiacm.client.util.ScmTestBase;
//
//public class TestScmBatchDelete extends ScmTestBase {
//
//    @Test
//    public static void testDelete() throws ScmException {
//
//        ScmSession session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
//                new ScmConfigOption(url, user, password));
//        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace("ws_default", session);
//
//        // Batch
//        ScmFactory.Batch.deleteInstance(ws, new ScmId("5ae029ab0000010000310003"));
//
//        session.close();
//    }
//    
//    @Test
//    public static void testDelete2() throws ScmException {
//
//        ScmSession session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
//                new ScmConfigOption(url, user, password));
//        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace("ws_default", session);
//
//        // Batch
//        ScmBatch123 batch = ScmFactory.Batch.getInstance(ws, new ScmId("5ac8837b000001000026002a"));
//        batch.delete();
//
//        session.close();
//    }
//}
