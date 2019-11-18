//package com.sequoiacm.client.core.batch;
//
//import java.util.List;
//
//import org.testng.annotations.Test;
//
//import com.sequoiacm.client.common.ScmType.SessionType;
//import com.sequoiacm.client.core.ScmBatchInfo;
//import com.sequoiacm.client.core.ScmConfigOption;
//import com.sequoiacm.client.core.ScmFactory;
//import com.sequoiacm.client.core.ScmSession;
//import com.sequoiacm.client.core.ScmWorkspace;
//import com.sequoiacm.client.element.ScmBatchCondition;
//import com.sequoiacm.client.element.ScmClassProperties;
//import com.sequoiacm.client.exception.ScmException;
//import com.sequoiacm.client.util.ScmTestBase;
//
//public class TestScmBatchGetInfoList extends ScmTestBase {
//
//    @Test
//    public static void testGetInfoList() throws ScmException {
//        ScmSession session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
//                new ScmConfigOption(url, user, password));
//        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);
//
//        ScmBatchCondition condition = new ScmBatchCondition();
//        ScmClassProperties properties = new ScmClassProperties("0");
//        properties.addProperty("name", "value");
//        condition.setProperties(properties);
///*
//        List<ScmBatchInfo> batchList = ScmFactory.Batch.listInstance(ws, condition);
//
//        for (ScmBatchInfo batchInfo : batchList) {
//            System.out.println("id:" + batchInfo.getId());
//            System.out.println("name:" + batchInfo.getName());
//            System.out.println("author:" + batchInfo.getAuthor());
//        }
//*/
//        session.close();
//    }
//}
