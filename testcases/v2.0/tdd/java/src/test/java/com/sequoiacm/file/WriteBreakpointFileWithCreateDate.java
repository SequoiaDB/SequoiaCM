package com.sequoiacm.file;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

/**
 * 创建断点文件时带创建时间
 *
 */
public class WriteBreakpointFileWithCreateDate extends ScmTestMultiCenterBase {
    private static final Logger logger = Logger.getLogger(WriteBreakpointFileWithCreateDate.class);

    private String srcFile;
    private ScmSession bSiteSs;
    private ScmWorkspace ws;
    private ScmId id;
    private String fileName;
    private boolean needDeleteBPFile = false;

    @BeforeClass
    public void setUp() throws ScmException {
        String workDir = getDataDirectory() + File.separator + ScmTestTools.getClassName();
        ScmTestTools.createDir(workDir);
        srcFile = getDataDirectory() + File.separator + "test.txt";
        // site A createFile
        bSiteSs = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer2().getUrl(), getScmUser(), getScmPasswd()));
    }

    @Test
    public void getFile() throws ScmException, IOException, ParseException {
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), bSiteSs);
        fileName = ScmTestTools.getClassName();
        ScmBreakpointFile bpFile = ScmFactory.BreakpointFile.createInstance(ws, fileName);

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date d = format.parse("2017-12-11");
        d = new Date(1000 * 1000);
        bpFile.setCreateTime(d);

        bpFile.upload(new File(srcFile));
        needDeleteBPFile = true;
        bpFile.isCompleted();
        logger.info("breakPoint:file=" + bpFile.getFileName() + ",id=" + bpFile.getDataId()
                + ",createTime=" + bpFile.getCreateTime());

        ScmFile file = ScmFactory.File.createInstance(ws);
        file.setTitle(ScmTestTools.getClassName());
        file.setFileName(ScmTestTools.getClassName());
        file.setContent(bpFile);
        // file.setCreateTime(d);
        file.save();
        needDeleteBPFile = false;
        id = file.getFileId();

        logger.info("fileId=" + id.get() + ",fileCreatTime=" + file.getCreateTime().getTime()
                + ",dataId=" + file.getDataId().get() + ",dataCreateTime="
                + file.getDataCreateTime().getTime());

        Assert.assertTrue(bpFile.getDataId().equals(file.getDataId().get()));
        Assert.assertTrue(bpFile.getCreateTime().getTime() == file.getDataCreateTime().getTime());
        Assert.assertTrue(!file.getDataId().get().equals(file.getFileId().get()));
        Assert.assertTrue(file.getDataCreateTime().getTime() != file.getCreateTime().getTime());
    }

    private void deleteBreakpointFile(String fileName) {
        try {
            ScmFactory.BreakpointFile.deleteInstance(ws, fileName);
        }
        catch (Exception e) {
            logger.warn("delete breakpoint file failed:file=" + fileName, e);
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {
        if (needDeleteBPFile) {
            deleteBreakpointFile(fileName);
        }

        if (null != id) {
            ScmTestTools.removeScmFileSilence(ws, id);
        }

        if (null != bSiteSs) {
            bSiteSs.close();
        }
    }
}