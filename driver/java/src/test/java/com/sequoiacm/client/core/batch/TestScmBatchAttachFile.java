//package com.sequoiacm.client.core.batch;
//
//import java.util.List;
//
//import org.testng.annotations.Test;
//
//import com.sequoiacm.client.common.ScmType.SessionType;
//import com.sequoiacm.client.core.ScmBatch;
//import com.sequoiacm.client.core.ScmConfigOption;
//import com.sequoiacm.client.core.ScmFactory;
//import com.sequoiacm.client.core.ScmFile;
//import com.sequoiacm.client.core.ScmSession;
//import com.sequoiacm.client.core.ScmWorkspace;
//import com.sequoiacm.client.element.ScmId;
//import com.sequoiacm.client.exception.ScmException;
//import com.sequoiacm.client.util.ScmTestBase;
//
//public class TestScmBatchAttachFile extends ScmTestBase {
//
//    @Test
//    public static void testAttach() throws ScmException {
//        ScmSession session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
//                new ScmConfigOption(url, user, password));
//        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace("ws_default", session);
//
//        // Batch
//        ScmId batchId = new ScmId("5ac1f3a200000100002b0005");
//        ScmId fileId = new ScmId("5ac1de7e00000100002c0006");
//        ScmBatch batch = ScmFactory.Batch.getInstance(ws, batchId);
//        List<ScmFile> old = batch.listFiles();
//        System.out.println("before: " + old);
//        batch.attachFile(fileId);
//        List<ScmFile> newL = batch.listFiles();
//        System.out.println("after: " + newL);
//        
//        
//        batch = ScmFactory.Batch.getInstance(ws, batchId);
//        System.out.println("query again: " + batch.listFiles());
//        session.close();
//    }
//}
