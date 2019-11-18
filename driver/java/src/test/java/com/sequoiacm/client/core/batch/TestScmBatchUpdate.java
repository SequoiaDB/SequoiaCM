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
//import com.sequoiacm.client.element.ScmClassProperties;
//import com.sequoiacm.client.element.ScmId;
//import com.sequoiacm.client.element.ScmProperties;
//import com.sequoiacm.client.exception.ScmException;
//import com.sequoiacm.client.util.ScmTestBase;
//
//public class TestScmBatchUpdate extends ScmTestBase {
//
//    @Test
//    public static void testUpdate() throws ScmException {
//        ScmSession session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
//                new ScmConfigOption(url, user, password));
//        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace("ws_default", session);
//
//        // Batch
//        ScmId batchId = new ScmId("5ac1f3a200000100002b0005");
//        ScmBatch123 batch = ScmFactory.Batch.getInstance(ws, batchId);
//        ScmProperties properties = batch.getProperties();
//        System.out.println(properties);
//        properties.addProperty("k2", "v2");
//        batch.setProperties(properties);
//        System.out.println(properties);
//        
//        batch.setName("update_name2");
//        System.out.println(batch.getName());
//        
//        batch.setName("update_name3");
//        System.out.println(batch.getName());
//        
//        System.out.println("-----");
//
//        session.close();
//    }
//}
