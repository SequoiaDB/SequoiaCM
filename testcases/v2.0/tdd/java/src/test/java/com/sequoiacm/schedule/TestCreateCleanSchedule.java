package com.sequoiacm.schedule;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmSiteInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestCreateCleanSchedule extends ScmTestMultiCenterBase {
    private final static Logger logger = Logger.getLogger(TestCreateCleanSchedule.class);

    private ScmSession ss;
    private ScmWorkspace ws;
    private ScmFile file;
    private ScmId scheduleId;

    @BeforeClass
    public void setUp() throws Exception {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer2().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);

        // server2 create 1 File
        file = ScmFactory.File.createInstance(ws);
        file.setFileName(ScmTestTools.getClassName());
        file.setAuthor("test");
        file.setTitle("sequoiacm");
        file.setMimeType(MimeType.PLAIN);
        file.save();

        readFileFromRoot(file.getFileId());
    }

    private void readFileFromRoot(ScmId fileId) throws Exception {
        ScmSession rootss = null;
        ScmWorkspace myWs = null;
        try {
            String workDir = getDataDirectory() + File.separator + ScmTestTools.getClassName();
            ScmTestTools.createDir(workDir);
            String targetFile = workDir + File.separator + "test.txt";
            ScmTestTools.deleteFile(targetFile);
            rootss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
            myWs = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), rootss);

            ScmFile file = ScmFactory.File.getInstance(myWs, fileId);
            file.getContent(targetFile);
        }
        catch (Exception e) {
            logger.error("readFileFromRoot failed", e);
            throw e;
        }
        finally {
            if (null != rootss) {
                rootss.close();
            }
        }
    }

    @Test
    public void createSchedule() throws ScmException {

        try {
            ScmSiteInfo info = ScmTestTools.getSiteInfo(ss, getSiteId2());
            ScmScheduleContent content = new ScmScheduleCleanFileContent(info.getName(), "0d", null);
            String cron = "* * * * * ?";
            ScmSchedule sch = ScmSystem.Schedule.create(ss, getWorkspaceName(),
                    ScheduleType.CLEAN_FILE, "s1", "desc", content, cron);

            scheduleId = sch.getId();

            while (true) {
                ScmFile tmpFile = ScmFactory.File.getInstance(ws, file.getFileId());
                List<ScmFileLocation> locationList = tmpFile.getLocationList();
                logger.info(ScmTestTools.formatLocationList(locationList));
                if (locationList.size() == 1) {
                    ScmFileLocation location = locationList.get(0);
                    if (location.getSiteId() == getSiteId1()) {
                        break;
                    }
                }

                Thread.sleep(1000);
            }
        }
        catch (Exception e) {
            logger.error("createSchedule failed", e);
            Assert.fail(e.getMessage());
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            TestScheduleCommon.deleteScheduleSilence(ss, scheduleId);
            if (null != file) {
                file.delete(true);
            }
        }
        finally {
            ss.close();
        }
    }
}