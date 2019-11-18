package com.sequoiacm.workspace;

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
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.client.element.bizconf.ScmSdbDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;

public class ListPageWorkspace extends ScmTestMultiCenterBase {
    private ScmSession ss;
    private String wsName1 = "listWorkspaceTest1";
    private String wsName2 = "listWorkspaceTest2";
    private String wsName3 = "listWorkspaceTest3";

    @BeforeClass
    public void init() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ScmFactory.Workspace.deleteWorkspace(ss, wsName1, true);
        ScmFactory.Workspace.deleteWorkspace(ss, wsName2, true);
        ScmFactory.Workspace.deleteWorkspace(ss, wsName3, true);
    }

    @Test
    public void test() throws Exception {
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setName(wsName1);
        conf.setDescription(this.getClass().getName());
        conf.setMetaLocation(new ScmSdbMetaLocation("rootSite", ScmShardingType.MONTH, "domain1"));
        conf.addDataLocation(new ScmSdbDataLocation("rootSite", "domain2", ScmShardingType.YEAR,
                ScmShardingType.QUARTER));
        ScmFactory.Workspace.createWorkspace(ss, conf);

        conf.setName(wsName2);
        ScmFactory.Workspace.createWorkspace(ss, conf);

        conf.setName(wsName3);
        ScmFactory.Workspace.createWorkspace(ss, conf);
        BSONObject ascOrder=new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_ID, 1);
        BSONObject descOrder=new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_ID, -1);
        
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
            ScmFactory.Workspace.deleteWorkspace(ss, wsName1, true);
            ScmFactory.Workspace.deleteWorkspace(ss, wsName2, true);
            ScmFactory.Workspace.deleteWorkspace(ss, wsName3, true);
        }
        finally {
            ss.close();
        }
    }

    // page
    private void qeuryAndCheckPage(ScmSession ss,BSONObject order,long skip,int limit,boolean[] check)
            throws Exception {
        ScmCursor<ScmWorkspaceInfo> cursor = ScmFactory.Workspace.listWorkspace(ss,
                order,skip, limit);
        Set<String> wsNames = new HashSet<String>();
        while (cursor.hasNext()) {
            ScmWorkspaceInfo currentItem = cursor.getNext();
            wsNames.add(currentItem.getName());
        }
        assertEquals(wsNames.contains(wsName3), check[0]);
        assertEquals(wsNames.contains(wsName2), check[1]);
        assertEquals(wsNames.contains(wsName1), check[2]);
        cursor.close();
    }

    // orderby
    private void qeuryAndCheckOrderBy(ScmSession ss,BSONObject order, boolean isAsc)
            throws Exception {
        ScmCursor<ScmWorkspaceInfo> cursor = ScmFactory.Workspace.listWorkspace(ss, order , 0, -1);
        ScmWorkspaceInfo currentItem = null;
        ScmWorkspaceInfo nextItem = null;
        if (cursor.hasNext()) {
            currentItem = cursor.getNext();
        }
        while (cursor.hasNext()) {
            nextItem = cursor.getNext();
            assertEquals(nextItem.getId() > currentItem.getId(), isAsc);
            currentItem = nextItem;
        }
        cursor.close();
    }

}
