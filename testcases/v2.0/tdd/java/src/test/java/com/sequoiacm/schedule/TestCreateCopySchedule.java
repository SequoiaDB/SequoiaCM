package com.sequoiacm.schedule;

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
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.element.ScmSiteInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestCreateCopySchedule extends ScmTestMultiCenterBase {
    private final static Logger logger = Logger.getLogger(TestCreateCopySchedule.class);

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
    }

    @Test
    public void createSchedule() throws ScmException {

        try {
            ScmSiteInfo rootsite = ScmTestTools.getSiteInfo(ss, getSiteId1());
            ScmSiteInfo site2 = ScmTestTools.getSiteInfo(ss, getSiteId2());
            ScmScheduleContent content = new ScmScheduleCopyFileContent(site2.getName(),
                    rootsite.getName(), "0d", null);
            String cron = "* * * * * ?";
            ScmSchedule sch = ScmSystem.Schedule.create(ss, getWorkspaceName(),
                    ScheduleType.COPY_FILE, "s1", "desc", content, cron);

            scheduleId = sch.getId();

            boolean isTaskFinish = false;
            while (!isTaskFinish) {
                ScmFile tmpFile = ScmFactory.File.getInstance(ws, file.getFileId());
                List<ScmFileLocation> locationList = tmpFile.getLocationList();
                logger.info(ScmTestTools.formatLocationList(locationList));
                for (ScmFileLocation location : locationList) {
                    if (location.getSiteId() == getSiteId1()) {
                        isTaskFinish = true;
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