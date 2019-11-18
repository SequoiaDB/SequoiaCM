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
import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.element.metadata.ScmStringRule;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestListAttr extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestListAttr.class);
    private ScmSession ss;
    private ScmWorkspace ws;
    private final int attrNum = 5;
    private final String attrName = "TestListAttr";
    private final String desc = "description for attr";
    private List<ScmId> attrIds = new ArrayList<>();

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);

        ScmAttributeConf attrConf = new ScmAttributeConf().setType(AttributeType.STRING)
                .setDescription(desc);
        for (int i = 0; i < attrNum; ++i) {
            attrConf.setName(attrName + i);
            ScmAttribute scmAttr = ScmFactory.Attribute.createInstance(ws, attrConf);
            attrIds.add(scmAttr.getId());
            logger.info(scmAttr.toString());
        }
    }

    @Test
    public void testGetList() throws ScmException {
        List<ScmId> tmpAttrIds = new ArrayList<>(attrIds);
        int total = 0;
        ScmCursor<ScmAttribute> cursor = ScmFactory.Attribute.listInstance(ws,
                new BasicBSONObject(ScmAttributeName.Attribute.DESCRIPTION, desc));
        while (cursor.hasNext()) {
            ScmAttribute info = cursor.getNext();
            Assert.assertTrue(info.getName().startsWith(attrName));
            Assert.assertEquals(info.getDescription(), desc);
            Assert.assertEquals(info.getDisplayName(), "");
            Assert.assertEquals(info.getType(), AttributeType.STRING);
            ScmStringRule rule = (ScmStringRule) info.getCheckRule();
            Assert.assertEquals(rule.getMaxLength(), -1);
            Assert.assertEquals(info.getCreateUser(), getScmUser());
            Assert.assertTrue(tmpAttrIds.contains(info.getId()));
            tmpAttrIds.remove(info.getId());
            ++total;
        }
        cursor.close();
        Assert.assertEquals(total, attrNum, "wrong attr num");

        // filter with operator, list the last attr
        total = 0;
        BasicBSONList inList = new BasicBSONList();
        inList.add(attrName + 0);
        BSONObject matcher = new BasicBSONObject(ClientDefine.QueryOperators.IN, inList);
        cursor = ScmFactory.Attribute.listInstance(ws, 
                new BasicBSONObject(ScmAttributeName.Attribute.NAME, matcher));
        while (cursor.hasNext()) {
            ScmAttribute info = cursor.getNext();
            Assert.assertEquals(info.getId().get(), attrIds.get(0).get());
            Assert.assertEquals(info.getName(), attrName + 0);
            Assert.assertEquals(info.getDescription(), desc);
            Assert.assertEquals(info.getDisplayName(), "");
            Assert.assertEquals(info.getType(), AttributeType.STRING);
            ScmStringRule rule = (ScmStringRule) info.getCheckRule();
            Assert.assertEquals(rule.getMaxLength(), -1);
            Assert.assertEquals(info.getCreateUser(), getScmUser());
            ++total;
        }
        cursor.close();
        Assert.assertEquals(total, 1, "wrong attr num");

        // wrong filter
        total = 0;
        BSONObject errMatch = new BasicBSONObject("$abc", desc);
        try {
            cursor = ScmFactory.Attribute.listInstance(ws, new BasicBSONObject(
                    ScmAttributeName.Attribute.NAME, errMatch));
            Assert.fail("list attr with wrong filter should not be successful");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.METASOURCE_ERROR, e.getMessage());
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            for (ScmId scmId : attrIds) {
                ScmFactory.Attribute.deleteInstance(ws, scmId);
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
