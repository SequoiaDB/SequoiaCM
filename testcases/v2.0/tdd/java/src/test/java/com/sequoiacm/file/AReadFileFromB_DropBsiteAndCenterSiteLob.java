//package com.sequoiacm.file;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.OutputStream;
//
//import org.bson.types.ObjectId;
//import org.testng.Assert;
//import org.testng.annotations.AfterClass;
//import org.testng.annotations.BeforeClass;
//import org.testng.annotations.Test;
//
//import com.sequoiacm.client.common.ScmType.InputStreamType;
//import com.sequoiacm.client.common.ScmType.SessionType;
//import com.sequoiacm.client.core.ScmConfigOption;
//import com.sequoiacm.client.core.ScmFactory;
//import com.sequoiacm.client.core.ScmFile;
//import com.sequoiacm.client.core.ScmInputStream;
//import com.sequoiacm.client.core.ScmSession;
//import com.sequoiacm.client.core.ScmWorkspace;
//import com.sequoiacm.client.exception.ScmException;
//import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
//import com.sequoiacm.testcommon.ScmTestTools;
//import com.sequoiadb.base.CollectionSpace;
//import com.sequoiadb.base.DBCollection;
//import com.sequoiadb.base.Sequoiadb;
//
///**
// * A中心创建ScmInputStream对象，删除B 主中心的LOB，A中心下载该文件
// *
// * @author huangqiaohui
// *
// */
//public class AReadFileFromB_DropBsiteAndCenterSiteLob extends ScmTestMultiCenterBase {
//
//    private String srcFile;
//    private String downFile;
//    private ScmFile bScmFile;
//    private String centerCoord;
//    private String lobCS;
//    private String lobCL = "LOB";
//    private ScmSession bSiteSs;
//    private String sdb2;
//
//    @BeforeClass
//    public void setUp() throws ScmException {
//        lobCS = getWorkspaceName() + "_LOB";
//        String workDir = getDataDirectory() + File.separator + ScmTestTools.getClassName();
//        ScmTestTools.createDir(workDir);
//        srcFile = getDataDirectory() + File.separator + "test.txt";
//        downFile = workDir + File.separator + "down.txt";
//        sdb2 = getServer2().getUrl();
//        centerCoord = getServer1().getUrl();
//        // site B createFile
//        bSiteSs = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
//                getServer2().getUrl(), getScmUser(), getScmPasswd()));
//        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), bSiteSs);
//        bScmFile = ScmTestTools.createScmFile(ws, srcFile, ScmTestTools.getClassName(), "", "testTitle");
//    }
//
//    @Test(enabled=false)
//    public void getFile() throws ScmException, IOException {
//        ScmSession ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
//                new ScmConfigOption(getServer3().getUrl(), getScmUser(),
//                        getScmPasswd()));
//        OutputStream os = null;
//        ScmInputStream is = null;
//        Sequoiadb dbB = null;
//        Sequoiadb dbCenter = null;
//        try {
//            // A Site create ScmInputStream
//            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
//            ScmFile sf = ScmFactory.File.getInstance(ws, bScmFile.getFileId());
//            is = ScmFactory.File.createInputStream(InputStreamType.SEEKABLE, sf);
//
//            // Drop BSite CenterSite Lob
//            dbB = new Sequoiadb(sdb2, "", "");
//            removeLob(dbB, sf.getFileId().get());
//            dbCenter = new Sequoiadb(centerCoord, "", "");
//            removeLob(dbCenter, sf.getFileId().get());
//
//            // ASite getFile
//            os = new FileOutputStream(downFile);
//            is.read(os);
//            Assert.assertEquals(ScmTestTools.getMD5(srcFile), ScmTestTools.getMD5(downFile));
//        }
//        finally {
//            if (os != null) {
//                os.close();
//            }
//            if (is != null) {
//                is.close();
//            }
//            if (dbB != null) {
//                dbB.disconnect();
//            }
//            if (dbCenter != null) {
//                dbCenter.disconnect();
//            }
//            ss.close();
//        }
//    }
//
//    private void removeLob(Sequoiadb db, String oid) throws IOException {
//        CollectionSpace cs = db.getCollectionSpace(lobCS);
//        DBCollection cl = cs.getCollection(lobCL);
//        cl.removeLob(new ObjectId(oid));
//    }
//
//    @AfterClass
//    public void tearDown() throws ScmException {
//        bScmFile.delete(true);
//        bSiteSs.close();
//    }
//}
