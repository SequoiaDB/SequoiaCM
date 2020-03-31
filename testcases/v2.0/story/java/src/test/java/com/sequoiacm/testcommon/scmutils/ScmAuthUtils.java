
package com.sequoiacm.testcommon.scmutils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.testng.Assert;

import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.NodeWrapper;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

public class ScmAuthUtils extends TestScmBase {
    private static final Logger logger = Logger.getLogger( ScmAuthUtils.class );
    private static final int defaultTimeOut = 10 * 1000; // 10s
    private static final int sleepTime = 200;  // 200ms
    private static RestTemplate rest;

    static {
        HttpComponentsClientHttpRequestFactory factory = new
                HttpComponentsClientHttpRequestFactory();
        factory.setConnectionRequestTimeout( 10000 );
        factory.setConnectTimeout( 10000 );
        factory.setBufferRequestBody( false );
        factory.setReadTimeout( 30000 );
        rest = new RestTemplate( factory );
    }

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
            String password, ScmRole role, String wsName )
            throws Exception {
        ScmSession ss = null;
        ScmSession newSS = null;
        try {
            // login
            ss = TestScmTools.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName, ss );

            ScmUser user = ScmFactory.User.getUser( ss, username );
            Assert.assertTrue( user.hasRole( role ) );

            //create scm dir
            List< NodeWrapper > nodeList = site.getNodes();
            List< ScmDirectory > scmDirs = new ArrayList<>();
            String dirPath =
                    "/ScmAuthUtils" + "_" + username + "_" + UUID.randomUUID();
            for ( int i = 0; i < nodeList.size(); i++ ) {
                ScmDirectory dir = ScmFactory.Directory
                        .createInstance( ws, dirPath );
                if ( i == 0 ) {
                    ScmResource resource = ScmResourceFactory
                            .createDirectoryResource( wsName, dirPath );
                    // grant privilege
                    ScmFactory.Role.grantPrivilege( ss, role, resource,
                            ScmPrivilegeType.DELETE );
                }
                scmDirs.add( dir );
                dirPath = dirPath + "/" + i;
            }
            int version1 = ScmFactory.Privilege.getMeta( ss ).getVersion();
            newSS = TestScmTools.createSession( site, username, password );
            int maxTimes = defaultTimeOut / sleepTime;
            for ( int i = scmDirs.size()-1; i >= 0; i-- ) {
                while ( maxTimes-- > 0 ) {
                    try {
                        //the newWS used to check privilege come into effect
                        deleteScmDirByRest( newSS, nodeList.get( i ),
                                wsName, scmDirs.get( i ) );
                        break;
                    } catch ( ScmException e ) {
                        Thread.sleep( sleepTime );
                        if ( ScmError.OPERATION_UNAUTHORIZED == e.getError() ) {
                            logger.warn( username + " has tried " + (
                                    defaultTimeOut / sleepTime - maxTimes )
                                    + " times." + "version1 = " + version1
                                    + ",version2 = " + ScmFactory.Privilege
                                    .getMeta( ss ).getVersion() );
                        } else {
                            logger.error(
                                    "failed to wait privilege come into " +
                                            "effect,version1 = "

                                            + version1 + ",version2 = "
                                            + ScmFactory.Privilege.getMeta( ss )
                                            .getVersion() + ",scmDir = "
                                            + scmDirs.get( i ).getId() );
                            throw e;
                        }
                    }
                }
                if ( maxTimes == -1 ) {
                    throw new Exception(
                            "privilege did not come into effect, timeout" +
                                    ".version1" + " = "
                                    + version1 + ",version2 = "
                                    + ScmFactory.Privilege.getMeta( ss )
                                    .getVersion() + ",scmDirid = " +
                                    scmDirs.get(
                                            i ).getId() );
                }
                maxTimes = defaultTimeOut / sleepTime;
            }
        } finally {
            if ( ss != null ) {
                ss.close();
            }
            if ( newSS != null ) {
                newSS.close();
            }
        }
    }

    private static void deleteScmDirByRest( ScmSession session,
            NodeWrapper node, String wsName, ScmDirectory scmDirectory )
            throws ScmException {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add( "x-auth-token", session.getSessionId() );
        HttpEntity entity = new HttpEntity<>( new LinkedMultiValueMap<>(),
                requestHeaders );
        try {
            rest.exchange( "http://" + node.getUrl() +
                            "/api/v1/directories/id/"
                            + scmDirectory.getId() + "?workspace_name=" +
                            wsName, HttpMethod.DELETE, entity,
                    String.class );
        } catch ( HttpClientErrorException e ) {
            if ( e.getResponseBodyAsString() != null && e
                    .getResponseBodyAsString().contains( "-109" ) ) {
                throw new ScmException( -109, e.getResponseBodyAsString() );
            }
        }
    }
}