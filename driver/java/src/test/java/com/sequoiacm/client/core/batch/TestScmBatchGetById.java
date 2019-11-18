//package com.sequoiacm.client.core.batch;
//
//import java.util.List;
//
//import org.testng.annotations.Test;
//
//import com.sequoiacm.client.common.ScmType.SessionType;
//import com.sequoiacm.client.core.ScmBatch123;
//import com.sequoiacm.client.core.ScmConfigOption;
//import com.sequoiacm.client.core.ScmFactory;
//import com.sequoiacm.client.core.ScmFile;
//import com.sequoiacm.client.core.ScmSession;
//import com.sequoiacm.client.core.ScmWorkspace;
//import com.sequoiacm.client.element.ScmId;
//import com.sequoiacm.client.exception.ScmException;
//import com.sequoiacm.client.util.ScmTestBase;
//
//public class TestScmBatchGetById extends ScmTestBase {
//
//    @Test
//    public static void testGet() throws ScmException {
//        ScmSession session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
//                new ScmConfigOption(url, user, password));
//        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace("ws_default", session);
//
//        // Batch
//        ScmBatch123 batch = ScmFactory.Batch.getInstance(ws, new ScmId("5ac1f3a200000100002b0005"));
//        List<ScmFile> files = batch.listFiles();
//        for (ScmFile scmFile : files) {
//            System.out.println(scmFile);
//            
//        }
//        System.out.println(batch);
//        session.close();
//    }
//}
