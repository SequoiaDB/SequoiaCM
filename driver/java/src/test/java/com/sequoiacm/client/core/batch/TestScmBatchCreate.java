//package com.sequoiacm.client.core.batch;
//
//import java.util.List;
//import java.util.Map;
//
//import org.testng.annotations.Test;
//
//import com.sequoiacm.client.common.ScmType.SessionType;
//import com.sequoiacm.client.core.ScmBatch;
//import com.sequoiacm.client.core.ScmBatch123;
//import com.sequoiacm.client.core.ScmClass;
//import com.sequoiacm.client.core.ScmConfigOption;
//import com.sequoiacm.client.core.ScmFactory;
//import com.sequoiacm.client.core.ScmSession;
//import com.sequoiacm.client.core.ScmWorkspace;
//import com.sequoiacm.client.element.ScmClassAttr;
//import com.sequoiacm.client.element.ScmProperties;
//import com.sequoiacm.client.exception.ScmException;
//import com.sequoiacm.client.util.ScmTestBase;
//
//public class TestScmBatchCreate extends ScmTestBase {
//
//    @Test
//    public static void testCreate() throws ScmException {
//        ScmSession session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
//                new ScmConfigOption(url, user, password));
//        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace("ws_default", session);
//
////        ScmClass scmClass = ScmFactory.Class.getInstance(ws, 1);
//        
//        // Batch
//        ScmBatch batch = ScmFactory.Batch.createInstance(ws);
//        batch.setName("batch-new2");
//        ScmProperties props = new ScmProperties();
//        props.addProperty("name", "value");
//        batch.setProperties(props);
//        batch.save();
//
//        session.close();
//    }
//}
