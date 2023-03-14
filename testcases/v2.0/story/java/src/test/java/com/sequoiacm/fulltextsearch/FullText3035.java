package com.sequoiacm.fulltextsearch;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsPool;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description: SCM-3035 :: 设置用户工作区权限，全文检索
 * @author fanyu
 * @Date:2020/11/16
 * @version:1.0
 */
public class FullText3035 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private String fileName = "file3035";
    private ScmId fileId = null;
    private String filePath = null;
    private String useaname = "user3035";
    private String rolename = "role3035";

    @BeforeClass
    private void setUp() throws Exception {
        filePath = TestTools.LocalFile
                .getFileByType( TestTools.LocalFile.FileType.TEXT );
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        createUserAndRole();
        // 创建多版本文件
        fileId = ScmFileUtils.create( ws, fileName, filePath );
        // 创建索引
        ScmFactory.Fulltext.createIndex( ws, new ScmFulltextOption(
                new BasicBSONObject(), ScmFulltextMode.sync ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
    }

    @Test
    private void test() throws Exception {
        // 有权限
        BSONObject condition1 = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        FullTextUtils.searchAndCheckResults( ws, ScmType.ScopeType.SCOPE_ALL,
                condition1, condition1 );

        // 无权限
        ScmSession session1 = null;
        try {
            session1 = ScmSessionUtils.createSession( site, useaname, useaname );
            ScmWorkspace ws1 = ScmFactory.Workspace.getWorkspace( wsName,
                    session1 );
            ScmFactory.Fulltext.simpleSeracher( ws1 )
                    .fileCondition( condition1 )
                    .scope( ScmType.ScopeType.SCOPE_ALL )
                    .notMatch( "condition" ).search();
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                throw e;
            }
        } finally {
            if ( session1 != null ) {
                session1.close();
            }
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Role.deleteRole( session, rolename );
                ScmFactory.User.deleteUser( session, useaname );
                ScmFactory.File.deleteInstance( ws, fileId, true );
                ScmFactory.Fulltext.dropIndex( ws );
                FullTextUtils.waitWorkSpaceIndexStatus( ws,
                        ScmFulltextStatus.NONE );
            }
        } finally {
            if ( wsName != null ) {
                WsPool.release( wsName );
            }
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void createUserAndRole() throws Exception {
        // 清理环境
        try {
            ScmFactory.Role.deleteRole( session, rolename );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                throw e;
            }
        }
        try {
            ScmFactory.User.deleteUser( session, useaname );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                throw e;
            }
        }
        // 创建用户、角色和授权
        ScmUser scmUser = ScmFactory.User.createUser( session, useaname,
                ScmUserPasswordType.LOCAL, useaname );
        ScmRole role = ScmFactory.Role.createRole( session, rolename, "" );
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.addRole( role );
        ScmFactory.User.alterUser( session, scmUser, modifier );
        ScmResource resource = ScmResourceFactory
                .createWorkspaceResource( wsName );
        ScmFactory.Role.grantPrivilege( session, role, resource,
                ScmPrivilegeType.CREATE );
        ScmFactory.Role.grantPrivilege( session, role, resource,
                ScmPrivilegeType.DELETE );
        ScmFactory.Role.grantPrivilege( session, role, resource,
                ScmPrivilegeType.UPDATE );
        ScmAuthUtils.checkPriority( site, useaname, useaname, role, wsName );
    }
}