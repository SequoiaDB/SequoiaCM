package com.sequoiacm.schedulePreferredZone;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @description SCM-3711:创建调度任务参数校验
 * @author YiPan
 * @createDate 2021.8.26
 * @updateUser YiPan
 * @updateDate 2021.8.26
 * @updateRemark
 * @version v1.0
 */
public class CreateSchedule3711A extends TestScmBase {
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private String tmpPath = null;
    private String fileName = "file3711A";
    private String region;
    private String zone;
    private WsWrapper wsp = null;
    private ScmSession sourceSiteSession;
    private ScmSession targetSiteSession;
    private ScmWorkspace sourceSiteWs;
    private SiteWrapper sourceSite;
    private SiteWrapper targetSite;
    private List< ScmId > fileIds = new ArrayList<>();
    private BSONObject queryCond;
    private boolean runSuccess = false;
    private final String type = "type";
    private final String workspace = "workspace";
    private final String name = "name";
    private final String content = "content";
    private final String cron = "cron";
    private final String enable = "enable";
    private final String preferredRegion = "preferredRegion";
    private final String preferredZone = "preferredZone";

    @BeforeClass
    public void setUp() throws Exception {
        region = TestScmBase.defaultRegion;
        zone = TestScmBase.zone1;
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        tmpPath = localPath + File.separator + "tmpFile_" + fileSize + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( tmpPath, fileSize / 2 );

        wsp = ScmInfo.getWs();
        sourceSite = ScmInfo.getBranchSite();
        targetSite = ScmInfo.getRootSite();
        sourceSiteSession = TestScmTools.createSession( sourceSite );
        targetSiteSession = TestScmTools.createSession( targetSite );
        sourceSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                sourceSiteSession );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        ScmId scmId = ScmFileUtils.create( sourceSiteWs, fileName, filePath );
        fileIds.add( scmId );
    }

    @Test
    public void test() throws Exception {
        HashMap< String, Object > options = initParameters();
        checkDefaultParameters( options );
        checkNullParameters( options );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                sourceSiteSession.close();
                targetSiteSession.close();
            }
        }
    }

    private HashMap< String, Object > initParameters() throws ScmException {
        String maxStayTime = "0d";
        String scheduleName = "testCopy" + fileName;
        ScmScheduleCopyFileContent copyContent = new ScmScheduleCopyFileContent(
                sourceSite.getSiteName(), targetSite.getSiteName(), maxStayTime,
                queryCond );
        HashMap< String, Object > option = new HashMap<>();
        option.put( type, ScheduleType.COPY_FILE );
        option.put( workspace, wsp.getName() );
        option.put( name, scheduleName );
        option.put( content, copyContent );
        option.put( cron, "* * * * * ?" );
        option.put( enable, true );
        option.put( preferredRegion, region );
        option.put( preferredZone, zone );
        return option;
    }

    private void checkDefaultParameters( HashMap< String, Object > option )
            throws NoSuchMethodException, ClassNotFoundException, ScmException,
            InvocationTargetException, IllegalAccessException {
        option.remove( enable );
        option.remove( preferredZone );
        option.remove( preferredRegion );
        List< Method > allMethods = new ArrayList<>();
        Class< ? > aClass = Class.forName( ScmScheduleBuilder.class.getName() );
        allMethods.add( aClass.getMethod( type, ScheduleType.class ) );
        allMethods.add( aClass.getMethod( workspace, String.class ) );
        allMethods.add( aClass.getMethod( name, String.class ) );
        allMethods.add( aClass.getMethod( content, ScmScheduleContent.class ) );
        allMethods.add( aClass.getMethod( cron, String.class ) );
        List< Method > runMethods = new ArrayList<>();
        for ( int i = 0; i < allMethods.size(); i++ ) {
            ScmScheduleBuilder schBuilder = ScmSystem.Schedule
                    .scheduleBuilder( sourceSiteSession );
            runMethods.clear();
            runMethods.addAll( allMethods );
            runMethods.remove( i );
            for ( Method method : runMethods ) {
                if ( method.getName().equals( type ) ) {
                    ScheduleType type = ( ScheduleType ) option
                            .get( method.getName() );
                    method.invoke( schBuilder, type );
                } else if ( method.getName().equals( content ) ) {
                    ScmScheduleContent content = ( ScmScheduleContent ) option
                            .get( method.getName() );
                    method.invoke( schBuilder, content );
                } else if ( method.getName().equals( enable ) ) {
                    boolean enable = ( boolean ) option.get( method.getName() );
                    method.invoke( schBuilder, enable );
                } else {
                    String string = ( String ) option.get( method.getName() );
                    method.invoke( schBuilder, string );
                }
            }
            try {
                schBuilder.build();
                Assert.fail( "excepted fail but succeed" );
            } catch ( ScmException e ) {
                if ( !( e.getError().equals( ScmError.INVALID_ARGUMENT ) ) ) {
                    throw e;
                }
            }
        }
    }

    private void checkNullParameters( HashMap< String, Object > options )
            throws ScmException {
        Object[] keys = options.keySet().toArray();
        for ( int i = 0; i < keys.length; i++ ) {
            HashMap< String, Object > op = options;
            op.put( keys[ i ].toString(), null );
            ScmScheduleBuilder schBuilder = ScmSystem.Schedule
                    .scheduleBuilder( sourceSiteSession );
            schBuilder.type( ( ScheduleType ) op.get( type ) )
                    .workspace( ( String ) op.get( workspace ) )
                    .name( ( String ) op.get( name ) )
                    .content( ( ScmScheduleContent ) op.get( content ) )
                    .cron( ( String ) op.get( cron ) )
                    .preferredRegion( ( String ) op.get( preferredRegion ) )
                    .preferredZone( ( String ) op.get( preferredZone ) );
            try {
                schBuilder.build();
                Assert.fail( "excepted fail but succeed" );
            } catch ( ScmException e ) {
                if ( !( e.getError().equals( ScmError.INVALID_ARGUMENT ) ) ) {
                    throw e;
                }
            }
        }
    }
}