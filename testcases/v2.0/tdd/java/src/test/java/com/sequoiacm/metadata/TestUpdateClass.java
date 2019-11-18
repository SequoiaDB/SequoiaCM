package com.sequoiacm.metadata;

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

public class TestUpdateClass extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestUpdateClass.class);
    private ScmSession ss;
    private ScmWorkspace ws;
    private ScmId classId1;
    private ScmId classId2;
    private String className = ScmTestTools.getClassName();
    private String dupName = "TestUpdateClassNameDuplicate";

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
        
        ScmClass scmClass1 = ScmFactory.Class.createInstance(ws, className, "");
        classId1 = scmClass1.getId();
        logger.info("create class:" + scmClass1.toString());
        
        ScmClass scmClass2 = ScmFactory.Class.createInstance(ws, dupName, "");
        classId2 = scmClass2.getId();
        logger.info("create class:" + scmClass2.toString());
    }

    @Test
    public void testUpdate() throws ScmException, InterruptedException {
        ScmClass oldClass = ScmFactory.Class.getInstance(ws, classId1);
        logger.info("oldClass: " + oldClass.toString());
        Assert.assertEquals(oldClass.isExist(), true);
        Assert.assertEquals(oldClass.getName(), className);
        Assert.assertEquals(oldClass.getDescription(), "");
        Assert.assertEquals(oldClass.getCreateTime(), oldClass.getUpdateTime());
        
        // set new value
        Thread.sleep(10);
        oldClass.setName(className + "-new");
        oldClass.setDescription("模型中文描述");
        
        ScmClass newClass = ScmFactory.Class.getInstance(ws, classId1);
        logger.info("newClass: " + newClass.toString());
        Assert.assertEquals(newClass.isExist(), true);
        Assert.assertEquals(newClass.getName(), className + "-new");
        Assert.assertEquals(newClass.getDescription(), "模型中文描述");
        Assert.assertEquals(newClass.getCreateTime(), oldClass.getCreateTime());
        Assert.assertNotEquals(newClass.getCreateTime(), newClass.getUpdateTime(), 
                "create and update time should not equal");

        /*
         *  test set null
         */
        try {
            oldClass.setName(null);
            Assert.fail("set class name=null should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }
        
        try {
            oldClass.setName("");
            Assert.fail("set class name empty string should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }
        
        try {
            oldClass.setDescription(null);;
            Assert.fail("set class description=null should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }
        
        /*
         *  test set duplicate name
         */
        // set newName=oldName, is ok
        oldClass.setName(className + "-new");
        
        // set dup name
        try {
            oldClass.setName(dupName);
            Assert.fail("set duplicate name should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METADATA_CLASS_EXIST, e.getMessage());
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            ScmFactory.Class.deleteInstance(ws, classId1);
            ScmFactory.Class.deleteInstance(ws, classId2);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            ScmTestTools.releaseSession(ss);
        }
    }
}
