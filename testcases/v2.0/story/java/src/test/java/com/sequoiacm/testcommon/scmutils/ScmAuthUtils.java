package com.sequoiacm.testcommon.scmutils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.testng.Assert;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.amazonaws.util.Base64;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmServiceInstance;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.NodeWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

public class ScmAuthUtils extends TestScmBase {
    private static final Logger logger = Logger.getLogger( ScmAuthUtils.class );
    private static final int defaultTimeOut = 10 * 1000; // 10s
    private static final int sleepTime = 1000; // 1s
    private static RestTemplate rest;

    static {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectionRequestTimeout( 10000 );
        factory.setConnectTimeout( 10000 );
        factory.setBufferRequestBody( false );
        factory.setReadTimeout( 60000 );
        rest = new RestTemplate( factory );
    }

    /**
     * @Description: 检查主站点和传进来site的节点权限
     * @param site
     *            站点对象
     * @param username
     *            scm用户名
     * @param password
     *            scm用户密码
     * @param role
     *            scm角色
     * @param wsp
     *            工作区对象
     * @throws Exception
     */
    public static void checkPriority( SiteWrapper site, String username,
            String password, ScmRole role, WsWrapper wsp ) throws Exception {
        checkPriority( site, username, password, role, wsp.getName() );
    }

    /**
     * @Description: 检查主站点和传进来site的节点权限
     * @param site
     *            站点对象
     * @param username
     *            scm用户名
     * @param password
     *            scm用户密码
     * @param role
     *            scm角色
     * @param wsName
     *            工作区名
     * @throws Exception
     */
    public static void checkPriority( SiteWrapper site, String username,
            String password, ScmRole role, String wsName ) throws Exception {
        SiteWrapper rootSite = ScmInfo.getRootSite();
        List< NodeWrapper > nodeWrappers = new ArrayList<>();
        nodeWrappers.addAll( site.getNodes() );
        if ( site.getSiteId() != rootSite.getSiteId() ) {
            nodeWrappers.addAll( rootSite.getNodes() );
        }
        ScmSession ss = null;
        List< ScmDirectory > scmDirs = new ArrayList<>();
        try {
            ss = TestScmTools.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName, ss );
            for ( int i = 0; i < nodeWrappers.size(); i++ ) {
                String dirPath = "/ScmAuthUtils" + "_" + username + "_"
                        + UUID.randomUUID();
                ScmDirectory dir = ScmFactory.Directory.createInstance( ws,
                        dirPath );
                ScmResource resource = ScmResourceFactory
                        .createDirectoryResource( wsName, dirPath );
                // grant privilege
                ScmFactory.Role.grantPrivilege( ss, role, resource,
                        ScmPrivilegeType.DELETE );
                scmDirs.add( dir );
            }
            ScmUser user = ScmFactory.User.getUser( ss, username );
            Assert.assertTrue( user.hasRole( role ) );
        } finally {
            if ( ss != null ) {
                ss.close();
            }
        }

        ScmSession newSS = null;
        try {
            // login
            // the newSS used to check privilege come into effect
            newSS = TestScmTools.createSession( site, username, password );
            for ( int i = 0; i < nodeWrappers.size(); i++ ) {
                checkNodePriority( newSS, wsName, nodeWrappers.get( i ),
                        scmDirs.get( i ) );
            }
        } finally {
            if ( newSS != null ) {
                newSS.close();
            }
        }
    }

    /**
     * @Description: 检查单个节点权限
     * @param ss
     *            scm会话
     * @param wsName
     *            工作名
     * @param node
     *            需要检测的节点对象
     * @param scmDirectory
     *            文件夹
     * @throws Exception
     */
    private static void checkNodePriority( ScmSession ss, String wsName,
            NodeWrapper node, ScmDirectory scmDirectory ) throws Exception {
        int version1 = ScmFactory.Privilege.getMeta( ss ).getVersion();
        int maxTimes = ScmAuthUtils.defaultTimeOut / ScmAuthUtils.sleepTime;
        while ( maxTimes-- > 0 ) {
            try {
                Thread.sleep( ScmAuthUtils.sleepTime );
                ScmAuthUtils.deleteScmDirByRest( ss, node, wsName,
                        scmDirectory );
                break;
            } catch ( ScmException e ) {
                if ( ScmError.OPERATION_UNAUTHORIZED == e.getError() ) {
                    ScmAuthUtils.logger.warn( ss.getUser() + " has tried "
                            + ( ScmAuthUtils.defaultTimeOut
                                    / ScmAuthUtils.sleepTime - maxTimes )
                            + " times." + "version1 = " + version1
                            + ",version2 = "
                            + ScmFactory.Privilege.getMeta( ss ).getVersion() );
                } else {
                    ScmAuthUtils.logger
                            .error( "failed to wait privilege come into "
                                    + "effect,version1 = " + version1
                                    + ",version2 = "
                                    + ScmFactory.Privilege.getMeta( ss )
                                            .getVersion()
                                    + ",scmDir = " + scmDirectory.getId() );
                    throw e;
                }
            }
        }
        if ( maxTimes == -1 ) {
            throw new Exception( "privilege did not come into effect, timeout"
                    + ".version1" + " = " + version1 + ",version2 = "
                    + ScmFactory.Privilege.getMeta( ss ).getVersion()
                    + ",scmDirid = " + scmDirectory.getId() );
        }
    }

    /**
     * @Description: 为了检查权限，连接rest删除文件夹
     * @param session
     *            scm会话
     * @param node
     *            需要检测的节点对象
     * @param wsName
     *            工作区名
     * @param scmDirectory
     *            文件夹
     * @throws ScmException
     */
    private static void deleteScmDirByRest( ScmSession session,
            NodeWrapper node, String wsName, ScmDirectory scmDirectory )
            throws ScmException {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add( "x-auth-token", session.getSessionId() );
        HttpEntity< LinkedMultiValueMap< Object, Object > > entity = new HttpEntity<>(
                new LinkedMultiValueMap<>(), requestHeaders );
        try {
            rest.exchange( "http://" + node.getUrl() + "/api/v1/directories/id/"
                    + scmDirectory.getId() + "?workspace_name=" + wsName,
                    HttpMethod.DELETE, entity, String.class );
        } catch ( HttpClientErrorException e ) {
            if ( e.getResponseBodyAsString() != null
                    && e.getResponseBodyAsString().contains( "-109" ) ) {
                throw new ScmException( -109, e.getResponseBodyAsString() );
            }
        }
    }

    /**
     * 登录接口，先检测用户名和密码，如果正确则登录成功；没有用户名和密码时，使用签名进行登录
     * 
     * @param username
     * @param password
     * @param signatureInfo
     * @return
     */
    public static String login( String username, String password,
            BSONObject signatureInfo ) {
        MultiValueMap< Object, Object > body = new LinkedMultiValueMap<>();
        body.add( "username", username );
        body.add( "password", password );
        if ( signatureInfo != null ) {
            body.add( "signature_info", signatureInfo );
        }
        HttpEntity< MultiValueMap< Object, Object > > entity = new HttpEntity<>(
                body, new HttpHeaders() );
        ScmServiceInstance authServer = ScmInfo.getAuthServerList().get( 0 );
        Map< String, String > response = rest
                .exchange(
                        "http://" + authServer.getIp() + ":"
                                + authServer.getPort() + "/login",
                        HttpMethod.POST, entity, String.class )
                .getHeaders().toSingleValueMap();
        return response.get( "x-auth-token" );
    }

    /**
     * 调用rest接口进行登录，必须使用这个接口进行登出，否则session会残留
     * 
     * @param sessionId
     */
    public static void logout( String sessionId ) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add( "x-auth-token", sessionId );
        HttpEntity< LinkedMultiValueMap< Object, Object > > entity = new HttpEntity<>(
                new LinkedMultiValueMap<>(), requestHeaders );
        ScmServiceInstance authServer = ScmInfo.getAuthServerList().get( 0 );
        rest.exchange( "http://" + authServer.getIp() + ":"
                + authServer.getPort() + "/logout", HttpMethod.POST, entity,
                String.class );
    }

    /**
     * 做简单的业务操作,用户必须有read权限
     * 
     * @param sessionId
     * @throws UnsupportedEncodingException
     */
    public static void getRootDir( String sessionId, String wsName )
            throws UnsupportedEncodingException {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add( "x-auth-token", sessionId );
        HttpEntity< LinkedMultiValueMap< Object, Object > > entity = new HttpEntity<>(
                new LinkedMultiValueMap<>(), requestHeaders );
        String response = rest.exchange( "http://" + gateWayList.get( 0 ) + "/"
                + ScmInfo.getSite().getSiteServiceName()
                + "/api/v1//directories/id/000000000000000000000000?workspace_name="
                + wsName, HttpMethod.HEAD, entity, String.class ).getHeaders()
                .getFirst( "directory" );
        response = URLDecoder.decode( response, "UTF-8" );
        JSONObject dirInfo1 = JSON.parseObject( response );
        Assert.assertEquals( dirInfo1.getString( "name" ), "/" );
    }

    /**
     * 刷新AccessKey
     * 
     * @param session
     * @param username
     * @param password
     * @param signatureInfo
     * @return 返回值是数组arr[0]为accesskey，arr[1]为secretkey，arr[2]为用户名
     * @throws Exception
     */
    public static String[] refreshAccessKey( ScmSession session,
            String username, String password, BSONObject signatureInfo )
            throws Exception {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add( "x-auth-token", session.getSessionId() );
        MultiValueMap< Object, Object > body = new LinkedMultiValueMap<>();
        body.add( "username", username );
        body.add( "password", password );
        if ( signatureInfo != null ) {
            body.add( "signature_info", signatureInfo );
        }
        HttpEntity< MultiValueMap< Object, Object > > entity = new HttpEntity<>(
                body, requestHeaders );
        ScmServiceInstance authServer = ScmInfo.getAuthServerList().get( 0 );
        ResponseEntity< String > response = rest.exchange(
                "http://" + authServer.getIp() + ":" + authServer.getPort()
                        + "/api/v1/accesskey?action=refresh",
                HttpMethod.POST, entity, String.class );
        if ( response.getStatusCode() == HttpStatus.OK ) {
            JSONObject jsonObject = JSON.parseObject( response.getBody() );
            return new String[] { jsonObject.getString( "accesskey" ),
                    jsonObject.getString( "secretkey" ),
                    jsonObject.getString( "username" ) };
        }
        throw new Exception( response.toString() );
    }

    /**
     * 创建管理员用户
     * 
     * @param session
     * @param wsName
     * @param username
     * @param password
     * @throws Exception
     */
    public static void createAdminUser( ScmSession session, String wsName,
            String username, String password ) throws Exception {
        ScmUser user = createUser( session, username, password );
        ScmRole role = ScmFactory.Role.getRole( session, "ROLE_AUTH_ADMIN" );
        alterUser( session, wsName, user, role, ScmPrivilegeType.ALL );
        checkPriority( ScmInfo.getSite(), username, password, role, wsName );
    }

    /**
     * 创建普通用户
     * 
     * @param session
     * @param wsName
     * @param username
     * @param password
     * @param roleName
     * @param privilege
     * @throws Exception
     */
    public static void createNormalUser( ScmSession session, String wsName,
            String username, String password, String roleName,
            ScmPrivilegeType privilege ) throws Exception {
        ScmUser user = createUser( session, username, password );
        ScmRole role = createRole( session, roleName );
        alterUser( session, wsName, user, role, privilege );
        checkPriority( ScmInfo.getSite(), username, password, role, wsName );
    }

    /**
     * 创建用户
     * 
     * @param session
     * @param username
     * @param password
     * @return
     * @throws ScmException
     */
    public static ScmUser createUser( ScmSession session, String username,
            String password ) throws ScmException {
        try {
            ScmFactory.User.deleteUser( session, username );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                throw e;
            }
        }
        return ScmFactory.User.createUser( session, username,
                ScmUserPasswordType.LOCAL, password );
    }

    /**
     * 创建角色
     * 
     * @param session
     * @param roleName
     * @return
     * @throws ScmException
     */
    public static ScmRole createRole( ScmSession session, String roleName )
            throws ScmException {
        try {
            ScmFactory.Role.deleteRole( session, roleName );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                throw e;
            }
        }
        return ScmFactory.Role.createRole( session, roleName, roleName );
    }

    /**
     * 更新的用户，比如：授权
     * 
     * @param session
     * @param wsName
     * @param user
     * @param role
     * @param privilege
     * @return
     * @throws ScmException
     */
    public static ScmUser alterUser( ScmSession session, String wsName,
            ScmUser user, ScmRole role, ScmPrivilegeType privilege )
            throws ScmException {
        ScmUserModifier modifier = new ScmUserModifier();
        ScmResource rs = ScmResourceFactory.createWorkspaceResource( wsName );
        ScmFactory.Role.grantPrivilege( session, role, rs, privilege );
        modifier.addRole( role );
        return ScmFactory.User.alterUser( session, user, modifier );
    }

    /**
     * 16进制编码
     * 
     * @param data
     * @return
     */
    public static String toHex( byte[] data ) {
        StringBuilder sb = new StringBuilder( data.length * 2 );
        for ( int i = 0; i < data.length; i++ ) {
            String hex = Integer.toHexString( data[ i ] );
            if ( hex.length() == 1 ) {
                // Append leading zero.
                sb.append( "0" );
            } else if ( hex.length() == 8 ) {
                // Remove ff prefix from negative numbers.
                hex = hex.substring( 6 );
            }
            sb.append( hex );
        }
        return sb.toString().toLowerCase( Locale.getDefault() );
    }

    /**
     * 64进制编码
     * 
     * @param data
     * @return
     */
    public static String encodeAsBase64( byte[] data ) {
        return Base64.encodeAsString( data );
    }

    /**
     * 计算签名
     * 
     * @param stringData
     * @param key
     * @param algorithm
     * @return
     */
    public static byte[] sign( String stringData, byte[] key,
            String algorithm ) {
        try {
            byte[] data = stringData.getBytes( "UTF-8" );
            Mac mac = Mac.getInstance( algorithm );
            mac.init( new SecretKeySpec( key, algorithm ) );
            return mac.doFinal( data );
        } catch ( Exception e ) {
            throw new RuntimeException(
                    "Unable to calculate a request signature: "
                            + e.getMessage(),
                    e );
        }
    }
}
