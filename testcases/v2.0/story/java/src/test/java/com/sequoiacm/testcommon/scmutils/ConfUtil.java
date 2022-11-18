package com.sequoiacm.testcommon.scmutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.testng.Assert;

import com.alibaba.fastjson.JSONObject;
import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmAuditInfo;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmServiceInstance;
import com.sequoiacm.client.element.ScmUpdateConfResult;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.config.ConfigCommonDefind;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * Created by fanyu on 2018/11/30.
 */
public class ConfUtil extends TestScmBase {
    public static final String GATEWAY_SERVICE_NAME = "gateway";
    public static final String ADMINSERVER_SERVICE_NAME = "admin-server";
    public static final String AUTH_SERVER_SERVICE_NAME = "auth-server";
    private static RestTemplate rest = null;

    static {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectionRequestTimeout( 10000 );
        factory.setConnectTimeout( 10000 );
        factory.setBufferRequestBody( false );
        factory.setReadTimeout( 30000 );
        rest = new RestTemplate( factory );
    }

    public static void restore( String serviceName )
            throws ScmException, InterruptedException {
        ScmSession session = null;
        try {
            SiteWrapper site = ScmInfo.getSite();
            session = TestScmTools.createSession( site );
            // restore audit configuration
            ScmConfigProperties confProp = null;
            if ( serviceName != null ) {
                confProp = ScmConfigProperties.builder().service( serviceName )
                        .updateProperty( ConfigCommonDefind.scm_audit_mask,
                                "ALL" )
                        .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                                "TOKEN" )
                        .build();
            } else {
                confProp = ScmConfigProperties.builder().allInstance()
                        .updateProperty( ConfigCommonDefind.scm_audit_mask,
                                "ALL" )
                        .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                                "TOKEN" )
                        .build();
            }
            ScmUpdateConfResultSet restoreResult = ScmSystem.Configuration
                    .setConfigProperties( session, confProp );
            System.out
                    .println( "restore result = " + restoreResult.toString() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    public static ScmUpdateConfResultSet updateConf( String serviceName,
            Map< String, String > confMap ) throws ScmException {
        ScmSession session = null;
        try {
            SiteWrapper site = ScmInfo.getSite();
            session = TestScmTools.createSession( site );
            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .service( serviceName ).updateProperties( confMap ).build();
            ScmUpdateConfResultSet actResults = ScmSystem.Configuration
                    .setConfigProperties( session, confProp );
            System.out.println( "update results = " + actResults.toString() );
            return actResults;
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    public static void deleteAuditConf( String serviceName )
            throws ScmException {
        SiteWrapper site = ScmInfo.getSite();
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site );
            List< ScmServiceInstance > instances = ScmSystem.ServiceCenter
                    .getServiceInstanceList( session, serviceName );
            Set< String > keySet = new HashSet< String >();
            for ( ScmServiceInstance instance : instances ) {
                Map< String, String > map = ConfUtil.getConfByRest(
                        instance.getIp() + ":" + instance.getPort() );
                for ( String key : map.keySet() ) {
                    if ( key.contains( "scm.audit" ) ) {
                        keySet.add( key );
                    }
                }
            }
            deleteConf( session, serviceName, keySet );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    /**
     * @descreption deleteAuditConf()接口的s3版本（s3端口号需要+1才能访问）
     * @param serviceName
     * @throws ScmException
     */
    public static void deleteS3AuditConf( String serviceName )
            throws ScmException {
        SiteWrapper site = ScmInfo.getSite();
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site );
            List< ScmServiceInstance > instances = ScmSystem.ServiceCenter
                    .getServiceInstanceList( session, serviceName );
            Set< String > keySet = new HashSet< String >();
            for ( ScmServiceInstance instance : instances ) {
                // s3节点暴露端口号+1
                int port = instance.getPort() + 1;
                Map< String, String > map = ConfUtil.getConfByRest(
                        instance.getIp() + ":" + port, "?action=actuator" );
                for ( String key : map.keySet() ) {
                    if ( key.contains( "scm.audit" ) ) {
                        keySet.add( key );
                    }
                }
            }
            deleteConf( session, serviceName, keySet );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    /**
     * 删除网关统计相关配置
     * 
     * @throws ScmException
     */
    public static void deleteGateWayStatisticalConf() throws Exception {
        // 删除配置前清理统计表
        StatisticsUtils.clearStatisticalInfo();
        SiteWrapper site = ScmInfo.getSite();
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site );
            Set< String > confSet = new HashSet<>();
            confSet.add( "scm.statistics.types" );
            confSet.add(
                    "scm.statistics.types.file_download.conditions.workspaces" );
            confSet.add( "scm.statistics.rawDataCacheSize" );
            confSet.add( "scm.statistics.rawDataReportPeriod" );
            confSet.add(
                    "scm.statistics.types.file_upload.conditions.workspaces" );
            confSet.add(
                    "scm.statistics.types.file_download.conditions.workspaceRegex" );
            confSet.add(
                    "scm.statistics.types.file_upload.conditions.workspaceRegex" );
            deleteConf( session, ConfUtil.GATEWAY_SERVICE_NAME, confSet );

            Set< String > confSet1 = new HashSet<>();
            confSet1.add( "scm.statistics.timeGranularity" );
            deleteConf( session, ConfUtil.ADMINSERVER_SERVICE_NAME, confSet1 );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }

        // 动态刷新是懒加载，写文件时才会加载新配置对象，销毁旧配置对象
        WsWrapper wsp = ScmInfo.getWs();
        for ( String gateway : TestScmBase.gateWayList ) {
            ScmSession session1 = null;
            try {
                ScmConfigOption scOpt = new ScmConfigOption(
                        gateway + "/" + site.getSiteServiceName(),
                        TestScmBase.scmUserName, TestScmBase.scmPassword );
                session1 = ScmFactory.Session.createSession(
                        ScmType.SessionType.AUTH_SESSION, scOpt );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session1 );
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setFileName(
                        "deleteGateWayStaticConf_" + UUID.randomUUID() );
                ScmId fileId = file.save();
                ScmFactory.File.deleteInstance( ws, fileId, true );
            } finally {
                if ( session1 != null ) {
                    session1.close();
                }

            }
        }
    }

    public static ScmUpdateConfResultSet deleteConf( ScmSession session,
            String serviceName, Set< String > keySet ) throws ScmException {
        List keyList = new ArrayList< String >();
        keyList.addAll( keySet );
        if ( !keySet.isEmpty() ) {
            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .deleteProperties( keyList ).service( serviceName ).build();
            ScmUpdateConfResultSet deleteResult = ScmSystem.Configuration
                    .setConfigProperties( session, confProp );
            System.out.println( "delete result = " + deleteResult.toString() );
            return deleteResult;
        }
        return null;
    }

    public static void checkNotTakeEffect( String serviceName )
            throws ScmException {
        switch ( serviceName ) {
        case "schedule-server":
            try {
                createScheuleAndCheck();
            } catch ( Exception e ) {
                Assert.assertEquals( e.getMessage().contains( "not logged" ),
                        true, "configuration is not effective,msg = "
                                + e.getMessage() );
            }
            break;
        case "auth-server":
            try {
                createUserAndCheck();
            } catch ( Exception e ) {
                Assert.assertEquals( e.getMessage().contains( "not logged" ),
                        true, "configuration is not effective,msg = "
                                + e.getMessage() );
            }
            break;
        default:
            List< SiteWrapper > sites = ScmInfo.getAllSites();
            for ( SiteWrapper site : sites ) {
                if ( site.getSiteServiceName().equals( serviceName ) ) {
                    checkNotTakeEffect( site, TestTools.getClassName() + "_"
                            + UUID.randomUUID() );
                    break;
                }
            }
            Assert.fail( "serviceName is invalid" );
        }
    }

    public static void checkNotTakeEffect( SiteWrapper site, String fileName )
            throws ScmException {
        ScmSession session = null;
        ScmId fileId = null;
        ScmWorkspace ws = null;
        try {
            session = TestScmTools.createSession( site );
            WsWrapper wsp = ScmInfo.getWs();
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
            // create file
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName + "_" + UUID.randomUUID() );
            fileId = file.save();
            // check audit
            boolean isLogged = checkAudit( session, new BasicBSONObject()
                    .append( ScmAttributeName.Audit.TYPE, "CREATE_FILE" ),
                    fileId.get() );
            if ( isLogged ) {
                printCurrConf( site.getNode().getUrl() );
                Assert.fail( "the service's configration should not be updated,"
                        + "serviceName = " + site.getSiteServiceName() );
            }
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
            if ( session != null ) {
                session.close();
            }
        }
    }

    public static void checkTakeEffect( String serviceName ) throws Exception {
        switch ( serviceName ) {
        case "schedule-server":
            createScheuleAndCheck();
            break;
        case "auth-server":
            createUserAndCheck();
            break;
        default:
            List< SiteWrapper > sites = ScmInfo.getAllSites();
            for ( SiteWrapper site : sites ) {
                if ( site.getSiteServiceName().equals( serviceName ) ) {
                    checkTakeEffect( site, TestTools.getClassName() + "_"
                            + UUID.randomUUID() );
                    break;
                }
            }
            Assert.fail( "serviceName is invalid" );
        }
    }

    public static void checkTakeEffect( SiteWrapper site, String fileName )
            throws Exception {
        ScmSession session = null;
        ScmId fileId = null;
        ScmWorkspace ws = null;
        try {
            session = TestScmTools.createSession( site );
            // create file
            WsWrapper wsp = ScmInfo.getWs();
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName + "_" + UUID.randomUUID() );
            fileId = file.save();

            // check audit
            boolean isLogged = checkAudit( session, new BasicBSONObject()
                    .append( ScmAttributeName.Audit.TYPE, "CREATE_FILE" ),
                    fileId.get() );
            if ( !isLogged ) {
                printCurrConf( site.getNode().getUrl() );
                throw new Exception(
                        "fileId = " + fileId.get() + " is not logged" );
            }
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
            if ( session != null ) {
                session.close();
            }
        }
    }

    /**
     * 1.successResult.getInstance() return hostname:port, but site.getUrl()
     * from db maybe return ip:port, so just check successResult.getInstance()
     * is not null. 2.we do not know the content of
     * failResult.getErrorMessage(), so just check failResult.getErrorMessage()
     * is not null.
     */
    public static void checkResultSet( ScmUpdateConfResultSet actResults,
            int expSuccessNum, int expFailNum, List< String > okServices,
            List< String > failedSrevices ) {
        System.out.println( "actResults = " + actResults.toString() );
        List< ScmUpdateConfResult > successResults = actResults.getSuccesses();
        List< ScmUpdateConfResult > failResults = actResults.getFailures();
        Assert.assertTrue( successResults.size() == expSuccessNum );
        Assert.assertTrue( failResults.size() == expFailNum,
                actResults.toString() );
        for ( ScmUpdateConfResult successResult : successResults ) {
            Assert.assertNotNull( successResult.getInstance(),
                    actResults.toString() );
            Assert.assertNull( successResult.getErrorMessage(),
                    actResults.toString() );
            Assert.assertEquals( isInSiteList( successResult, okServices ),
                    true,
                    "act servicesName is not in exp ServiceNames,actResults ="
                            + " " + actResults.toString() + ",expOKResults = "
                            + okServices.toString() );
        }
        for ( ScmUpdateConfResult failResult : failResults ) {
            Assert.assertNotNull( failResult.getInstance(),
                    actResults.toString() );
            Assert.assertNotNull( failResult.getErrorMessage(),
                    actResults.toString() );
            Assert.assertEquals( isInSiteList( failResult, failedSrevices ),
                    true,
                    "act servicesName is not in exp ServiceNames,actResults ="
                            + " " + actResults.toString() + ",expBadResults = "
                            + okServices.toString() );
        }
    }

    public static void checkUpdatedConf( String addr,
            Map< String, String > expMap ) {
        Map actMap = getConfByRest( addr );
        for ( Map.Entry< String, String > entry : expMap.entrySet() ) {
            if ( actMap.containsKey( entry.getKey() ) ) {
                Assert.assertEquals( actMap.get( entry.getKey() ),
                        entry.getValue(), actMap.toString() );
            } else {
                Assert.fail( "failed to update configuration,actMap = "
                        + actMap.toString() + ",expMap = "
                        + expMap.toString() );
            }
        }
    }

    public static void checkDeletedConf( String addr, List< String > keys ) {
        Map actMap = getConfByRest( addr );
        for ( String key : keys ) {
            if ( actMap.containsKey( key ) ) {
                Assert.fail( "failed to delete configuration,keys = "
                        + keys.toString() + ",actMap = " + actMap.toString() );
            }
        }
    }

    private static void createScheuleAndCheck() throws Exception {
        WsWrapper wsp = ScmInfo.getWs();
        List< SiteWrapper > sites = ScmNetUtils.getCleanSites( wsp );
        SiteWrapper branSite = sites.get( 1 );
        ScmSession session = null;
        ScmId scheduleId = null;
        String scheName = TestTools.getClassName() + "_" + UUID.randomUUID();
        try {
            session = TestScmTools.createSession( branSite );
            ScmScheduleContent content = new ScmScheduleCleanFileContent(
                    branSite.getSiteName(), "0d", new BasicBSONObject() );
            String cron = "* * * * * ? 2022";
            ScmSchedule sche = ScmSystem.Schedule.create( session,
                    wsp.getName(), ScheduleType.CLEAN_FILE, scheName, scheName,
                    content, cron );
            scheduleId = sche.getId();
            // check audit
            boolean isLogged = checkAudit( session, new BasicBSONObject()
                    .append( ScmAttributeName.Audit.TYPE, "CREATE_SCHEDULE" ),
                    scheName );
            if ( !isLogged ) {
                List< ScmServiceInstance > instances = ScmSystem.ServiceCenter
                        .getServiceInstanceList( session, "schedule-server" );
                printCurrConf( instances.get( 0 ).getIp() + ":"
                        + instances.get( 0 ).getPort() );
                throw new Exception(
                        "scheName = " + scheName + " is not logged" );
            }
        } finally {
            if ( scheduleId != null ) {
                ScmSystem.Schedule.delete( session, scheduleId );
            }
            if ( session != null ) {
                session.close();
            }
        }
    }

    private static void createUserAndCheck() throws Exception {
        SiteWrapper site = ScmInfo.getSite();
        String userName = TestTools.getClassName() + "_" + UUID.randomUUID();
        ScmSession session = null;
        ScmUser user = null;
        try {
            session = TestScmTools.createSession( site );
            user = ScmFactory.User.createUser( session, userName,
                    ScmUserPasswordType.LOCAL, TestTools.getClassName() );
            // check audit
            boolean isLogged = checkAudit( session, new BasicBSONObject()
                    .append( ScmAttributeName.Audit.TYPE, "CREATE_USER" ),
                    userName );
            if ( !isLogged ) {
                List< ScmServiceInstance > instances = ScmSystem.ServiceCenter
                        .getServiceInstanceList( session, "auth-server" );
                printCurrConf( instances.get( 0 ).getIp() + ":"
                        + instances.get( 0 ).getPort() );
                throw new Exception(
                        "user = " + user.getUsername() + " is not logged" );
            }
        } finally {
            if ( user != null ) {
                ScmFactory.User.deleteUser( session, user );
            }
            if ( session != null ) {
                session.close();
            }
        }
    }

    public static boolean checkAudit( ScmSession session, BSONObject bson,
            String str ) throws ScmException {
        // check audit
        ScmCursor< ScmAuditInfo > infoCursor = null;
        boolean isLogged = false;
        try {
            infoCursor = ScmFactory.Audit.listInstance( session, bson );
            while ( infoCursor.hasNext() ) {
                ScmAuditInfo info = infoCursor.getNext();
                if ( info.getMessage().contains( str ) ) {
                    isLogged = true;
                    break;
                }
            }
        } finally {
            if ( infoCursor != null ) {
                infoCursor.close();
            }
        }
        return isLogged;
    }

    public static void createUser( WsWrapper wsp, String name,
            ScmUserPasswordType passwordType, ScmPrivilegeType[] privileges )
            throws Exception {
        List< SiteWrapper > allSites = ScmInfo.getAllSites();
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( ScmInfo.getRootSite() );
            ScmUser scmUser = null;
            if ( passwordType.equals( ScmUserPasswordType.LDAP ) ) {
                scmUser = ScmFactory.User.createUser( session, name,
                        passwordType, "" );
            } else {
                scmUser = ScmFactory.User.createUser( session, name,
                        passwordType, name );
            }
            ScmRole role = ScmFactory.Role.createRole( session, name, "desc" );
            ScmResource rs = ScmResourceFactory
                    .createWorkspaceResource( wsp.getName() );
            for ( ScmPrivilegeType privilege : privileges ) {
                ScmFactory.Role.grantPrivilege( session, role, rs, privilege );
            }
            ScmUserModifier modifier = new ScmUserModifier();
            modifier.addRole( role );
            modifier.addRole( "ROLE_AUTH_ADMIN" );
            ScmFactory.User.alterUser( session, scmUser, modifier );
            for ( int i = 0; i < allSites.size(); i++ ) {
                if ( !passwordType.equals( ScmUserPasswordType.LDAP ) ) {
                    ScmAuthUtils.checkPriority( allSites.get( i ), name, name,
                            role, wsp );
                } else {
                    ScmAuthUtils.checkPriority( allSites.get( i ),
                            TestScmBase.ldapUserName, TestScmBase.ldapPassword,
                            role, wsp );
                }
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    public static void deleteUserAndRole( String username, String rolename )
            throws ScmException {
        SiteWrapper site = ScmInfo.getSite();
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site );
            try {
                ScmFactory.User.deleteUser( session, username );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
            try {
                ScmFactory.Role.deleteRole( session, rolename );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private static boolean isInSiteList( ScmUpdateConfResult result,
            List< String > services ) {
        return services.contains( result.getServiceName().toLowerCase() );
    }

    private static Map< String, String > getConfByRest( String addr ) {
        return getConfByRest( addr, "" );
    }

    private static Map< String, String > getConfByRest( String addr,
            String para ) {
        String url = "http://" + addr + "/internal/v1/env" + para;
        ResponseEntity< ? > resp = rest.getForEntity( url, String.class,
                new HashMap< String, String >() );
        JSONObject json = ( JSONObject ) JSONObject
                .parse( resp.getBody().toString() );
        Map map = new HashMap< String, String >();
        for ( String key : json.keySet() ) {
            if ( key.contains( "applicationConfig" ) ) {
                JSONObject subjson = json.getJSONObject( key );
                for ( Map.Entry< String, Object > entry : subjson.entrySet() ) {
                    map.put( entry.getKey(), entry.getValue() );
                }
            }
        }
        return map;
    }

    private static void printCurrConf( String url ) {
        Map< String, String > map = getConfByRest( url );
        for ( Map.Entry< String, String > entry : map.entrySet() ) {
            if ( entry.getKey().contains( "scm.audit" ) ) {
                System.out.println( entry.getKey() + ":" + entry.getValue() );
            }
        }
    }

    /**
     * @descreption 校验审计日志的方法，更加轻量级
     * @param session
     * @param type
     * @param message
     * @throws ScmException
     */
    public static boolean checkAuditByType( ScmSession session, String type,
            String message ) throws ScmException {
        ScmCursor< ScmAuditInfo > scmAuditInfoScmCursor = ScmFactory.Audit
                .listInstance( session, new BasicBSONObject(
                        ScmAttributeName.Audit.TYPE, type ) );
        boolean flag = false;
        while ( scmAuditInfoScmCursor.hasNext() ) {
            if ( scmAuditInfoScmCursor.getNext().getMessage()
                    .contains( message ) ) {
                flag = true;
                break;
            }
        }
        scmAuditInfoScmCursor.close();
        return flag;
    }
}
