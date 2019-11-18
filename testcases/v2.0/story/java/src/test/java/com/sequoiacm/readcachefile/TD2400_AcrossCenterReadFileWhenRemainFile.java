package com.sequoiacm.readcachefile;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @Description: SCM-2400 :: 主、分站点存在残留文件，跨中心读文件
 * @author fanyu
 * @Date:2019年02月28日
 * @version:1.0
 */

public class TD2400_AcrossCenterReadFileWhenRemainFile extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List<SiteWrapper> branSites = null;
    private final int branSitesNum = 2;
    private WsWrapper wsp = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;

    private String fileName = "file965";
    private List<ScmId> fileIdList = new CopyOnWriteArrayList<ScmId>();
    private int[] remainSizes1 = {10, 0, 1, 10};
    private int[] remainSizes2 = {10, 10, 5, 8};
    private int fileSize = 10;
    private File localPath = null;
    private String filePath = null;
    private List<String> remainFilePathList1 = new ArrayList<String>();
    private List<String> remainFilePathList2 = new ArrayList<String>();

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, IOException {
        localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
        filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
        TestTools.LocalFile.removeFile(localPath);
        TestTools.LocalFile.createDir(localPath.toString());
        TestTools.LocalFile.createFile(filePath, "test", fileSize);
        rootSite = ScmInfo.getRootSite();
        branSites = ScmInfo.getBranchSites(branSitesNum);
        wsp = ScmInfo.getWs();
        sessionA = TestScmTools.createSession(branSites.get(0));
        wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);
        BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(fileName).get();
        ScmFileUtils.cleanFile(wsp, cond);
    }

    @DataProvider(name = "range-provider")
    public Object[][] generateRangData() throws Exception {
        createRemainFile(remainSizes1, remainFilePathList1);
        createRemainFile(remainSizes2, remainFilePathList2);
        return new Object[][]{
                //mainSite  branchSite
                {remainFilePathList1.get(0), remainFilePathList2.get(0)},
                {remainFilePathList1.get(1), remainFilePathList2.get(1)},
                {remainFilePathList1.get(2), remainFilePathList2.get(2)},
                {remainFilePathList1.get(3), remainFilePathList2.get(3)}
        };
    }


    @Test(groups = {"fourSite"}, dataProvider = "range-provider")
    private void test(String remainFilePath1, String remainFilePath2) throws Exception {
        // write from centerA
        ScmId fileId = createFile(wsA, fileName, filePath);
        fileIdList.add(fileId);
        // remain file from centerB
        TestSdbTools.Lob.putLob(rootSite, wsp, fileId, remainFilePath1);
        TestSdbTools.Lob.putLob(branSites.get(1), wsp, fileId, remainFilePath2);
        // read from centerB
        this.readFile(fileId, branSites.get(1));
        // check meta and data,because the metadata is directly modified when remainsize is equal to filesize,
        // rootsite does not cache data
        if (new File(remainFilePath2).length() == fileSize) {
            SiteWrapper[] expSites = {branSites.get(0), branSites.get(1)};
            ScmFileUtils.checkMetaAndData(wsp, fileId, expSites, localPath, filePath);
        } else {
            SiteWrapper[] expSites = {rootSite, branSites.get(0), branSites.get(1)};
            ScmFileUtils.checkMetaAndData(wsp, fileId, expSites, localPath, filePath);
        }
        readFile(fileId, rootSite);
        SiteWrapper[] expSites = {rootSite, branSites.get(0), branSites.get(1)};
        ScmFileUtils.checkMetaAndData(wsp, fileId, expSites, localPath, filePath);
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if (runSuccess || forceClear) {
                for (ScmId fileId : fileIdList) {
                    ScmFactory.File.deleteInstance(wsA, fileId, true);
                }
                TestTools.LocalFile.removeFile(localPath);
            }
        } finally {
            if (sessionA != null) {
                sessionA.close();
            }
        }
    }

    private void readFile(ScmId fileId, SiteWrapper site) throws Exception {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession(site);
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);

            // read scmfile
            ScmFile file = ScmFactory.File.getInstance(ws, fileId);
            String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId());
            file.getContent(downloadPath);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private void createRemainFile(int[] remainSizes, List<String> remainFilePathList) throws IOException {
        for (int i = 0; i < remainSizes.length; i++) {
            if (remainSizes[i] == fileSize) {
                remainFilePathList.add(filePath);
            } else {
                String tmpfilePath = localPath + File.separator + "localFile_" + remainSizes[i] + ".txt";
                TestTools.LocalFile.createFile(tmpfilePath, remainSizes[i]);
                remainFilePathList.add(tmpfilePath);
            }
        }
    }

    private ScmId createFile(ScmWorkspace ws, String fileName, String filePath) throws ScmException {
        ScmFile file = ScmFactory.File.createInstance(ws);
        file.setFileName(fileName + "_" + UUID.randomUUID());
        file.setAuthor(fileName);
        file.setContent(filePath);
        return file.save();
    }
}