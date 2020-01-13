package com.sequoiacm.testcommon.scmutils;

import java.util.UUID;

import org.apache.log4j.Logger;
import org.testng.Assert;

import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

public class ScmAuthUtils extends TestScmBase {
    private static final Logger logger = Logger.getLogger( ScmAuthUtils.class );
    private static final int defaultTimeOut = 1 * 30 * 1000; // 0.5min
    private static final int sleepTime = 1 * 1000;  // 1s

    /**
     * check privilege is effect by wsp
     *
     * @author huangxiaoni
     */
    public static void checkPriority( SiteWrapper site, String username,
            String password, ScmRole role, WsWrapper wsp ) throws Exception {
        checkPriority( site, username, password, role, wsp.getName() );
    }

    /**
     * check privilege is effect by wsName
     *
     * @author huangxiaoni
     */
    public static void checkPriority( SiteWrapper site, String username,
            String password, ScmRole role, String wsName ) throws Exception {
        ScmSession ss = null;
        try {
            // login
            ss = TestScmTools.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName, ss );

            ScmUser user = ScmFactory.User.getUser( ss, username );
            Assert.assertTrue( user.hasRole( role ) );

            //create path
            String dirPath =
                    "/ScmAuthUtils" + "_" + username + "_" + UUID.randomUUID();
            ScmDirectory dir = ScmFactory.Directory
                    .createInstance( ws, dirPath );
            String fileName =
                    "ScmAuthUtils" + "_" + username + "_" + UUID.randomUUID();

            // grant privilege
            int version1 = ScmFactory.Privilege.getMeta( ss ).getVersion();
            ScmResource resource = ScmResourceFactory
                    .createDirectoryResource( wsName, dirPath );
            ScmFactory.Role.grantPrivilege( ss, role, resource,
                    ScmPrivilegeType.DELETE );

            //create file for check privilege
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName );
            file.setDirectory( dir );
            ScmId fileId = file.save();

            // check privilege is effect
            ScmSession newSS = null;
            try {
                newSS = TestScmTools.createSession( site, username, password );
                ScmWorkspace newWS = ScmFactory.Workspace
                        .getWorkspace( wsName, newSS );
                int maxTimes = defaultTimeOut / sleepTime;
                while ( maxTimes-- > 0 ) {
                    try {
                        //the newWS used to check privilege come into effect
                        ScmFactory.File.deleteInstance( newWS, fileId, true );
                        ScmFactory.Directory.deleteInstance( ws, dirPath );
                        return;
                    } catch ( ScmException e ) {
                        if ( ScmError.OPERATION_UNAUTHORIZED == e.getError() ) {
                            Thread.sleep( sleepTime );
                            logger.warn( username + " has tried " + (
                                    defaultTimeOut / sleepTime - maxTimes )
                                    + " times." + "version1 = " + version1
                                    + ",version2 = " + ScmFactory.Privilege
                                    .getMeta( ss ).getVersion() );
                        } else {
                            logger.error(
                                    "failed to wait privilege come into effect,version1 = "
                                            + version1 + ",version2 = "
                                            + ScmFactory.Privilege.getMeta( ss )
                                            .getVersion() + ",fileId = "
                                            + fileId );
                            throw e;
                        }
                    }
                }
                throw new Exception(
                        "privilege did not come into effect, timeout.version1 = "
                                + version1 + ",version2 = "
                                + ScmFactory.Privilege.getMeta( ss )
                                .getVersion() + ",fileId = " + fileId );
            } finally {
                if ( null != newSS ) {
                    newSS.close();
                }
            }
        } finally {
            if ( null != ss ) {
                ss.close();
            }
        }
    }
}
