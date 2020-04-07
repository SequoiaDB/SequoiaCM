package com.sequoiacm.net.scheduletask;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmClass;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmClassProperties;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;

/**
 * @Description:SCM-1944 :: SCM-1946 :: 通过断点文件更新文件，添加/更新/删除自定义属性和标签 
 * @author fanyu
 * @Date:2018年7月11日
 * @version:1.0
 */
public class ScheduleTransfer1955 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String name = "ScheduleTransfer1955";
    private int fileNum = 50;
    private int fileSize = 1024 * 1;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private List< String > tagList = new ArrayList< String >();
    private List< String > propertiesList = new ArrayList< String >();
    private ScmClass class1 = null;
    private ScmAttribute attr = null;
    private ScmId scheduleId = null;
    private ScmScheduleContent content = null;
    private String cron = null;
    private File localPath = null;
    private String filePath = null;
    private Calendar calendar = Calendar.getInstance();

    @BeforeClass
    private void setUp() throws IOException, ScmException, ParseException {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        wsp = ScmInfo.getWs();
        List< SiteWrapper > sites = ScmNetUtils.getSortSites( wsp );
        rootSite = sites.get( 1 );
        site = sites.get( 0 );
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( name ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        calendar.set( Calendar.HOUR, calendar.get( Calendar.HOUR ) - 3 );
        createAllFile( fileNum );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        BSONObject queryCond = ScmQueryBuilder
                .start( ScmAttributeName.File.PROPERTIES + "." + name )
                .lessThan( fileNum / 2 )
                .and( "tags" )
                .in( tagList.subList( 0, fileNum / 2 ) ).get();
        createScheduleTask( queryCond );
        SiteWrapper[] expSites = { rootSite, site };
        ScmScheduleUtils.checkScmFile( ws, fileIdList.subList( 0, fileNum / 2 ),
                expSites );
        SiteWrapper[] expSites1 = { site };
        ScmScheduleUtils
                .checkScmFile( ws, fileIdList.subList( fileNum / 2, fileNum ),
                        expSites1 );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( !runSuccess || TestScmBase.forceClear ) {
                if ( fileIdList != null ) {
                    for ( ScmId fileId : fileIdList ) {
                        System.out.println( "fileId = " + fileId.get() );
                    }
                }
                if ( scheduleId != null ) {
                    System.out.println( "scheduleId = " + scheduleId.get() );
                }
            }
            for ( ScmId fileId : fileIdList ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
            ScmFactory.Class.deleteInstance( ws, class1.getId() );
            ScmFactory.Attribute.deleteInstance( ws, attr.getId() );
            TestTools.LocalFile.removeFile( localPath );
            ScmSystem.Schedule.delete( session, scheduleId );
            ScmScheduleUtils.cleanTask( session, scheduleId );
            TestTools.LocalFile.removeFile( localPath );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void createScheduleTask( BSONObject queryCond )
            throws ScmException {
        String maxStayTime = "0d";
        content = new ScmScheduleCopyFileContent(
                site.getSiteName(), rootSite.getSiteName(), maxStayTime,
                queryCond );
        cron = "* * * * * ?";
        ScmSchedule sche = ScmSystem.Schedule.create( session, wsp.getName(),
                ScheduleType.COPY_FILE, name, "ScheduleTransfer1955", content,
                cron );
        scheduleId = sche.getId();
    }

    private void createAllFile( int num ) throws ScmException, ParseException {
        attr = craeteAttr( name );
        class1 = ScmFactory.Class.createInstance( ws, name, name + "_desc" );
        class1.attachAttr( attr.getId() );
        for ( int i = 0; i < num; i++ ) {
            ScmTags tags = new ScmTags();
            tags.addTag( String.valueOf( i ) );
            tagList.add( String.valueOf( i ) );
            ScmClassProperties properties = createProperties( class1, i );
            ScmId fileId = createFile( name, properties, tags );
            propertiesList.add( properties.toString() );
            fileIdList.add( fileId );
        }
    }

    private ScmId createFile( String name, ScmClassProperties properties,
            ScmTags tag ) throws ScmException, ParseException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( name + "_" + UUID.randomUUID() );
        file.setAuthor( name );
        file.setTags( tag );
        file.setClassProperties( properties );
        file.setCreateTime( calendar.getTime() );
        file.setContent( filePath );
        ScmId fileId = file.save();
        return fileId;
    }

    private ScmClassProperties createProperties( ScmClass class1, int value ) {
        ScmClassProperties properties = new ScmClassProperties(
                class1.getId().get() );
        for ( int i = 0; i < class1.listAttrs().size(); i++ ) {
            properties.addProperty( class1.listAttrs().get( i ).getName(),
                    value );
        }
        return properties;
    }

    private ScmAttribute craeteAttr( String name ) throws ScmException {
        ScmAttributeConf conf = new ScmAttributeConf();
        conf.setName( name );
        conf.setDescription( name + "_desc" );
        conf.setDisplayName( name + "_display" );
        conf.setRequired( false );
        conf.setType( AttributeType.INTEGER );
        ScmAttribute attr = ScmFactory.Attribute.createInstance( ws, conf );
        return attr;
    }
}
