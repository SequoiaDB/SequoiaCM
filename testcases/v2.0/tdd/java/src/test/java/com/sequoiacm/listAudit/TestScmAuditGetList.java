package com.sequoiacm.listAudit;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmAuditInfo;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;

public class TestScmAuditGetList extends ScmTestMultiCenterBase {

    private ScmSession ss;
    ScmCursor<ScmAuditInfo> listInstance;

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer1().getUrl(), getScmUser(), getScmPasswd()));

    }

    @Test
    public void testAttach() throws ScmException {
       BSONObject condition = new BasicBSONObject();
       condition.put(ScmAttributeName.Audit.USERNAME, getScmUser());
       listInstance = ScmFactory.Audit.listInstance(ss, condition);
       while (listInstance.hasNext()) {
           ScmAuditInfo info = listInstance.getNext();
           Assert.assertEquals(info.getUserName(), getScmUser());
           break;
       }
       listInstance.close();
    }

    @AfterClass
    public void tearDown() throws ScmException {
        ss.close();
    }
}
