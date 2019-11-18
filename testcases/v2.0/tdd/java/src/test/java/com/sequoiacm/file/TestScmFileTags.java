package com.sequoiacm.file;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

/**
 * 设置文件自定义标签
 * 
 * @author yanglei
 *
 **/
public class TestScmFileTags extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestScmFileTags.class);
    private ScmSession ss;
    private ScmWorkspace ws;
    private ScmId fileId;

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
    }

    @Test
    public void testTags() throws ScmException, IOException {
        ScmFile createdFile = ScmFactory.File.createInstance(ws);
        createdFile.setFileName(ScmTestTools.getClassName());
        // set tags
        ScmTags tags = new ScmTags();
        tags.addTag("tag-value1");
        tags.addTag("tag-value2");
        createdFile.setTags(tags);
        fileId = createdFile.save();
        checkTags(fileId, tags);

        // set tags=null,empty tags
        createdFile.setTags(null);
        checkTags(fileId, new ScmTags());

        // add tag = null
        try {
            createdFile.addTag(null);
            Assert.fail("tag cannot be null");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }

        // add tag=''
        try {
            createdFile.addTag("");
            Assert.fail("tag cannot be empty");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }

        // add tag start with '$' and contains '.'
        createdFile.addTag("$abc");
        createdFile.addTag("a.bc");
        ScmTags expectTags = new ScmTags();
        expectTags.addTag("$abc");
        expectTags.addTag("a.bc");
        checkTags(fileId, expectTags);

        // overlay update(setTags)
        ScmTags newTags = new ScmTags();
        newTags.addTag("aa");
        newTags.addTag("aa");
        createdFile.setTags(newTags);
        checkTags(fileId, newTags);

        // remove tag
        createdFile.removeTag("aa");
        checkTags(fileId, new ScmTags());

    }

    private void checkTags(ScmId fileId, ScmTags expectTags) throws ScmException {
        ScmFile savedFile = ScmFactory.File.getInstance(ws, fileId);
        Assert.assertEquals(savedFile.getTags().toString(), expectTags.toString());
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            if (null != fileId) {
                ScmFactory.File.deleteInstance(ws, fileId, true);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            ScmTestTools.releaseSession(ss);
        }
    }

}