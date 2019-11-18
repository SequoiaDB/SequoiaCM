package com.sequoiacm.metadata;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmClass;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestCreateClass extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestCreateClass.class);
    private ScmSession ss;
    private ScmWorkspace ws;
    final String className = "TestCreateClass";
    List<ScmId> classIds = new ArrayList<>();

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
    }

    @Test
    public void testCreate() throws ScmException {
        final int classNum = 5;
        
        for (int i = 0; i < classNum; ++i) {
            String tmpName = className + i;
            String desc = "模型描述：" + tmpName;
            ScmClass scmClass = ScmFactory.Class.createInstance(ws, tmpName, desc);
            Assert.assertEquals(scmClass.isExist(), true);
            logger.info(scmClass.toString());
            classIds.add(scmClass.getId());
            
            ScmClass info = ScmFactory.Class.getInstance(ws, scmClass.getId());
            Assert.assertEquals(info.isExist(), true);
            Assert.assertEquals(info.getName(), tmpName);
            Assert.assertEquals(info.getDescription(), desc);
            Assert.assertEquals(info.getCreateUser(), getScmUser());
        }
    }
    
    @Test
    public void testCreateDuplicateName() throws ScmException {
        ScmClass scmClass = ScmFactory.Class.createInstance(ws, className, "description");
        classIds.add(scmClass.getId());
        
        try {
            ScmFactory.Class.createInstance(ws, className, "description");
            Assert.fail("creare a class with duplicate name should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CLASS_EXIST, e.getMessage());
        }
    }
    
    @AfterClass
    private void tearDown() throws Exception {
        try {
            for (ScmId scmId : classIds) {
                ScmFactory.Class.deleteInstance(ws, scmId);
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
