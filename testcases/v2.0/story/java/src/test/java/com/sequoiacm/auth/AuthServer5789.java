package com.sequoiacm.auth;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * @descreption SCM-5789:驱动新增统计角色数量接口验证
 * @author ZhangYanan
 * @date 2022/11/05
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class AuthServer5789 extends TestScmBase {
    private static final String role = "role5789R";
    private static final String description = "testRole5789";
    private int roleNum = 10;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private BSONObject cond = null;
    private ArrayList< String > roleNameList = new ArrayList<>();

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        cond = ScmQueryBuilder.start( ScmAttributeName.Role.DESCRIPTION )
                .is( description ).get();

        for ( int i = 0; i < roleNum; i++ ) {
            ScmFactory.Role.createRole( session, role + i, description );
            roleNameList.add( "ROLE_" + role + i );
        }
    }

    @Test(groups = { GroupTags.oneSite, GroupTags.twoSite, GroupTags.fourSite })
    public void test() throws ScmException {
        countRoles();
        listRoles();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( int i = 0; i < roleNum; i++ ) {
                    ScmFactory.Role.deleteRole( session, role + i );
                }
            }
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
    }

    private void countRoles() throws ScmException {
        long actRoleNum = ScmFactory.Role.countRole( session, cond );
        Assert.assertEquals( actRoleNum, roleNum );
    }

    private void listRoles() throws ScmException {
        ScmCursor< ScmRole > scmRoleScmCursor = null;
        ArrayList< String > actRoleNameList = new ArrayList<>();
        try {
            scmRoleScmCursor = ScmFactory.Role.listRoles( session, cond,
                    new BasicBSONObject( ScmAttributeName.Role.ROLE_NAME, 1 ),
                    0, -1 );
            while ( scmRoleScmCursor.hasNext() ) {
                String roleName = scmRoleScmCursor.getNext().getRoleName();
                actRoleNameList.add( roleName );
            }

            Assert.assertEquals( actRoleNameList, roleNameList );
        } finally {
            if ( scmRoleScmCursor != null ) {
                scmRoleScmCursor.close();
            }
        }

    }
}
