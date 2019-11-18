package com.sequoiacm.user;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;

public class TestPageListUser extends ScmTestMultiCenterBase {
    private ScmSession ss;
    private String userName1 = "listUserTest1";
    private String userName2 = "listUserTest2";
    private String userName3 = "listUserTest3";

    @BeforeClass
    public void init() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
    }

    @Test
    public void test() throws Exception {
        ScmUserPasswordType userType = ScmUserPasswordType.LOCAL;
        ScmFactory.User.createUser(ss, userName1, userType, "pwd");
        ScmFactory.User.createUser(ss, userName2, userType, "pwd");
        ScmFactory.User.createUser(ss, userName3, userType, "pwd");

        BSONObject emptyCondition = new BasicBSONObject();
        long size = getUserSize();

        // limit
        boolean[] check1 = { true, true, false };
        qeuryAndCheckPage(ss, emptyCondition, size - 3, 2, check1);
        // skip
        boolean[] check2 = { false, true, true };
        qeuryAndCheckPage(ss, emptyCondition, size - 2, -1, check2);
        // page: skip limit
        boolean[] check3 = { false, true, false };
        qeuryAndCheckPage(ss, emptyCondition, size - 2, 1, check3);

        // condition
        BSONObject condition = new BasicBSONObject("password_type", userType);
        qeuryAndCheckCondition(ss, condition, null, 0, -1, userType);

    }

    private long getUserSize() throws ScmException {
        long size = 0;
        ScmCursor<ScmUser> cursor = ScmFactory.User.listUsers(ss);
        while (cursor.hasNext()) {
            cursor.getNext();
            size++;
        }
        return size;
    }

    @AfterClass
    public void cleanUp() throws ScmException {
        try {
            ScmFactory.User.deleteUser(ss, userName1);
            ScmFactory.User.deleteUser(ss, userName2);
            ScmFactory.User.deleteUser(ss, userName3);
        }
        finally {
            ss.close();
        }
    }

    // page
    private void qeuryAndCheckPage(ScmSession ss, BSONObject condition, long skip, int limit,
            boolean[] check) throws Exception {
        ScmCursor<ScmUser> cursor = ScmFactory.User.listUsers(ss, condition, skip, limit);
        Set<String> batchName = new HashSet<String>();
        while (cursor.hasNext()) {
            ScmUser currentItem = cursor.getNext();
            batchName.add(currentItem.getUsername());
        }
        assertEquals(batchName.contains(userName1), check[0]);
        assertEquals(batchName.contains(userName2), check[1]);
        assertEquals(batchName.contains(userName3), check[2]);
        cursor.close();
    }

    private void qeuryAndCheckCondition(ScmSession ws, BSONObject condition, BSONObject order,
            long skip, int limit, ScmUserPasswordType userType) throws ScmException {
        ScmCursor<ScmUser> cursor = ScmFactory.User.listUsers(ss, condition, skip, limit);
        while (cursor.hasNext()) {
            ScmUser currentItem = cursor.getNext();
            assertEquals(currentItem.getPasswordType(), userType);
        }
    }

}
