package com.sequoiacm.role;

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
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;

public class TestPageListRole extends ScmTestMultiCenterBase {
    private ScmSession ss;
    private String roleName1 = "listRoleTest1";
    private String roleName2 = "listRoleTest2";
    private String roleName3 = "listRoleTest3";

    @BeforeClass
    public void init() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        deleteRole(roleName1);
        deleteRole(roleName2);
        deleteRole(roleName3);
    }

    private void deleteRole(String roleName) {
        try {
            ScmFactory.Role.deleteRole(ss, roleName);
        }catch (Exception e) {
        }
    }

    @Test
    public void test() throws Exception {
        ScmFactory.Role.createRole(ss, roleName1, "descr");
        ScmFactory.Role.createRole(ss, roleName2, "descr");
        ScmFactory.Role.createRole(ss, roleName3, "descr");
        
        BSONObject ascOrder=new BasicBSONObject(FieldName.FIELD_ALL_OBJECTID, 1);
        BSONObject descOrder=new BasicBSONObject(FieldName.FIELD_ALL_OBJECTID, -1);
        
        //orderBy: asc desc
        qeuryAndCheckOrderBy(ss, ascOrder, true);
        qeuryAndCheckOrderBy(ss, descOrder, false);
        
        //limit
        boolean[] check1= {true,true,false};
        qeuryAndCheckPage(ss, descOrder,0,2,check1);
        //skip
        boolean[] check2= {false,true,true};
        qeuryAndCheckPage(ss, descOrder,1,-1,check2);
        //page: skip limit 
        boolean[] check3= {false,true,false};
        qeuryAndCheckPage(ss, descOrder,1,1,check3);

    }

    @AfterClass
    public void cleanUp() throws ScmException {
        try {
            ScmFactory.Role.deleteRole(ss, roleName1);
            ScmFactory.Role.deleteRole(ss, roleName2);
            ScmFactory.Role.deleteRole(ss, roleName3);
        }
        finally {
            ss.close();
        }
    }

    // page
    private void qeuryAndCheckPage(ScmSession ss,BSONObject order,long skip,int limit,boolean[] check)
            throws Exception {
        ScmCursor<ScmRole> cursor = ScmFactory.Role.listRoles(ss, order, skip, limit);
        Set<String> roleNames = new HashSet<String>();
        while (cursor.hasNext()) {
            ScmRole currentItem = cursor.getNext();
            roleNames.add(currentItem.getRoleName());
        }
        assertEquals(roleNames.contains("ROLE_"+roleName3), check[0]);
        assertEquals(roleNames.contains("ROLE_"+roleName2), check[1]);
        assertEquals(roleNames.contains("ROLE_"+roleName1), check[2]);
        cursor.close();
    }

    // orderby
    private void qeuryAndCheckOrderBy(ScmSession ss,BSONObject order, boolean isAsc)
            throws Exception {
        ScmCursor<ScmRole> cursor = ScmFactory.Role.listRoles(ss, order, 0, -1);
        ScmRole currentItem = null;
        ScmRole nextItem = null;
        if (cursor.hasNext()) {
            currentItem = cursor.getNext();
        }
        while (cursor.hasNext()) {
            nextItem = cursor.getNext();
            assertEquals(nextItem.getRoleId().compareTo(currentItem.getRoleId())>0, isAsc);
            currentItem = nextItem;
        }
        cursor.close();
    }

}
