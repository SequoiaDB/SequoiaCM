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
//public class TestScmBatchGetSpecificInfo extends ScmTestBase {
//
//    @Test
//    public static void testGetSpecificInfo() throws ScmException {
//        ScmSession session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
//                new ScmConfigOption(url, user, password));
//        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);
//
//        ScmBatch123 batch = ScmFactory.Batch.getInstance(ws, new ScmId("<batchId>"));
///*
//        if (null != batch) {
//            System.out.println("id:" + batch.getId());
//            System.out.println("name:" + batch.getName());
//            System.out.println("properties:" + batch.getProperties());
//            System.out.println("author:" + batch.getAuthor());
//            System.out.println("createTime:" + batch.getCreateTime());
//            System.out.println("updateUserName:" + batch.getUpdateUserName());
//            System.out.println("updateTime:" + batch.getUpdateTime());
//
//            List<ScmDocumentInfo> docsInfo = batch.getDocumentsInfo();
//            for (ScmDocumentInfo docInfo : docsInfo) {
//                // iterator scmdocumentinfo field
//            }
//
//            List<ScmFileBasicInfo> filesInfo = batch.getFilesInfo();
//            for (ScmFileBasicInfo fileInfo : filesInfo) {
//                System.out.println(fileInfo);
//            }
//        }
//*/
//        session.close();
//    }
//}
