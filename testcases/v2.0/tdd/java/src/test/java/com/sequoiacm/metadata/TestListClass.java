package com.sequoiacm.metadata;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ClientDefine;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmClass;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmClassBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestListClass extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestListClass.class);
    private ScmSession ss;
    private ScmWorkspace ws;
    private final int classNum = 5;
    private final String className = "TestListClass";
    private final String desc = "description for class";
    private List<ScmId> classIds = new ArrayList<>();

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);

        for (int i = 0; i < classNum; ++i) {
            ScmClass scmClass = ScmFactory.Class.createInstance(ws, className + i, desc);
            classIds.add(scmClass.getId());
            logger.info(scmClass.toString());
        }
    }

    @Test
    public void testGetList() throws ScmException {
        List<ScmId> tmpClassIds = new ArrayList<>(classIds);
        int total = 0;
        ScmCursor<ScmClassBasicInfo> cursor = ScmFactory.Class.listInstance(ws,
                new BasicBSONObject(ScmAttributeName.Class.DESCRIPTION, desc));
        while (cursor.hasNext()) {
            ScmClassBasicInfo info = cursor.getNext();
            Assert.assertTrue(info.getName().startsWith(className));
            Assert.assertEquals(info.getDescription(), desc);
            Assert.assertEquals(info.getCreateUser(), getScmUser());
            Assert.assertTrue(tmpClassIds.contains(info.getId()));
            tmpClassIds.remove(info.getId());
            ++total;
        }
        cursor.close();
        Assert.assertEquals(total, classNum, "wrong class num");

        // filter with operator
        total = 0;
        BasicBSONList inList = new BasicBSONList();
        inList.add(className + (classNum - 1));
        BSONObject matcher = new BasicBSONObject(ClientDefine.QueryOperators.IN, inList);
        cursor = ScmFactory.Class.listInstance(ws, 
                new BasicBSONObject(ScmAttributeName.Class.NAME, matcher));
        while (cursor.hasNext()) {
            ScmClassBasicInfo info = cursor.getNext();
            Assert.assertEquals(info.getId().get(), classIds.get(classNum-1).get());
            Assert.assertEquals(info.getName(), className + (classNum - 1));
            Assert.assertEquals(info.getDescription(), desc);
            Assert.assertEquals(info.getCreateUser(), getScmUser());
            ++total;
        }
        cursor.close();
        Assert.assertEquals(total, 1, "wrong class num");

        // wrong filter
        total = 0;
        BSONObject errMatch = new BasicBSONObject("$abc", desc);
        try {
            cursor = ScmFactory.Class.listInstance(ws, new BasicBSONObject(
                    ScmAttributeName.Class.NAME, errMatch));
            Assert.fail("list class with wrong filter should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METASOURCE_ERROR, e.getMessage());
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            ScmCursor<ScmClassBasicInfo> cursor = ScmFactory.Class.listInstance(ws,
                    new BasicBSONObject(FieldName.Class.FIELD_DESCRIPTION, desc));
            while (cursor.hasNext()) {
                ScmId classId = cursor.getNext().getId();
                ScmFactory.Class.deleteInstance(ws, classId);
            }
            cursor.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            ScmTestTools.releaseSession(ss);
        }
    }
}
