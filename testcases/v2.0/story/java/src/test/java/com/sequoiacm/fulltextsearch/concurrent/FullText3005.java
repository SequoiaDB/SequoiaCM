package com.sequoiacm.fulltextsearch.concurrent;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmClassProperties;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextModifiler;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.element.metadata.ScmIntegerRule;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltexInfo;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-3005 :: 并发ws更新索引和更新文件
 * @author fanyu
 * @Date:2020/9/25
 * @version:1.0
 */
public class FullText3005 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList = new ArrayList<>();
    private String fileNameBase = "file3005-";
    private int fileNum = 20;
    private String className = "class3005";
    private String attrNameBase = "attr3005-";
    private ScmId classId = null;
    private List< ScmId > attrIdList = new ArrayList<>();

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        // 准备模型和自定义属性
        createClassAndAttr();
        // 准备文件
        prepareFile();
        // 创建索引
        // 0<a1 <300, 50<= a2<= 100, 25<a3<50
        BSONObject fileCond = ScmQueryBuilder
                .start( "class_properties.attr3005-0" ).lessThan( 300 )
                .greaterThan( 0 ).and( "class_properties.attr3005-1" )
                .lessThan( 100 ).greaterThan( 50 )
                .and( "class_properties.attr3005-2" ).lessThan( 50 )
                .greaterThan( 25 ).get();
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( fileCond, ScmFulltextMode.sync ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
    }

    @Test
    private void test() throws Throwable {
        // 20<a1 <50, 0<= a2<= 55, 49<=a3<100
        BSONObject fileCondition = ScmQueryBuilder
                .start( "class_properties.attr3005-0" ).lessThan( 50 )
                .greaterThan( 20 ).and( "class_properties.attr3005-1" )
                .lessThanEquals( 55 ).greaterThanEquals( 0 )
                .and( "class_properties.attr3005-2" ).lessThan( 100 )
                .greaterThanEquals( 49 ).get();
        ThreadExecutor threadExec = new ThreadExecutor();
        // a、工作区更新索引
        threadExec.addWorker(
                new UpdateIndex( ScmFulltextMode.async, fileCondition ) );
        // b、更新文件a，其中文件属性匹配旧file_matcher条件
        UpdateFileAttr updateFileAttr1 = new UpdateFileAttr(
                fileIdList.subList( 0, fileNum / 3 ), 25, 55, 45 );
        threadExec.addWorker( updateFileAttr1 );
        // c、更新文件b，其中文件属性匹配新file_matcher条件
        UpdateFileAttr updateFileAttr2 = new UpdateFileAttr(
                fileIdList.subList( fileNum / 3, ( 2 * fileNum ) / 3 ), 26, 5,
                66 );
        threadExec.addWorker( updateFileAttr2 );

        // d、更新文件c，其中文件属性匹配新旧file_matcher条件
        UpdateFileAttr updateFileAttr3 = new UpdateFileAttr(
                fileIdList.subList( ( 2 * fileNum ) / 3, fileNum ), 26, 27,
                49 );
        threadExec.addWorker( updateFileAttr3 );
        threadExec.run();

        // 获取工作区索引信息
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
        ScmFulltexInfo indexInfo = ScmFactory.Fulltext.getIndexInfo( ws );
        Assert.assertEquals( indexInfo.getFileMatcher(), fileCondition );
        // 检查结果
        FullTextUtils.searchAndCheckResults( ws,
                ScmType.ScopeType.SCOPE_CURRENT, fileCondition, fileCondition );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Class.deleteInstance( ws, classId );
                for ( ScmId attrId : attrIdList ) {
                    ScmFactory.Attribute.deleteInstance( ws, attrId );
                }
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
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

    private class UpdateIndex {
        private ScmFulltextMode mode;
        private BSONObject fileCondition;

        public UpdateIndex( ScmFulltextMode mode, BSONObject fileCondition ) {
            this.mode = mode;
            this.fileCondition = fileCondition;
        }

        @ExecuteOrder(step = 1)
        private void update() throws ScmException {
            ScmFactory.Fulltext.alterIndex( ws, new ScmFulltextModifiler()
                    .newMode( mode ).newFileCondition( fileCondition ) );
        }
    }

    private class UpdateFileAttr {
        private List< ScmId > fileIdList;
        private int attr1;
        private int attr2;
        private int attr3;

        public UpdateFileAttr( List< ScmId > fileIdList, int attr1, int attr2,
                int attr3 ) {
            this.fileIdList = fileIdList;
            this.attr1 = attr1;
            this.attr2 = attr2;
            this.attr3 = attr3;
        }

        @ExecuteOrder(step = 1)
        private void update() throws ScmException {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                for ( ScmId fileId : fileIdList ) {
                    ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                    file.setClassProperty( "attr3005-0", attr1 );
                    file.setClassProperty( "attr3005-1", attr2 );
                    file.setClassProperty( "attr3005-2", attr3 );
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private void prepareFile() throws ScmException {
        byte[] bytes = new byte[ 1024 * 200 ];
        new Random().nextBytes( bytes );
        // 准备文件，用于线程“b、更新文件a，其中文件属性匹配旧file_matcher条件”
        for ( int i = 0; i < fileNum / 3; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setMimeType( MimeType.PLAIN );
            file.setFileName( fileNameBase + i );
            file.setContent( new ByteArrayInputStream( bytes ) );
            ScmClassProperties properties = new ScmClassProperties(
                    classId.get() );
            file.setClassProperties( properties );
            file.setClassProperty( "attr3005-0", 1 );
            file.setClassProperty( "attr3005-1", 2 );
            file.setClassProperty( "attr3005-2", 26 );
            fileIdList.add( file.save() );
        }

        // 准备文件，用于线程“c、更新文件b，其中文件属性匹配新file_matcher条件”
        for ( int i = fileNum / 3; i < ( 2 * fileNum ) / 3; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setMimeType( MimeType.PLAIN );
            file.setFileName( fileNameBase + i );
            file.setContent( new ByteArrayInputStream( bytes ) );
            ScmClassProperties properties = new ScmClassProperties(
                    classId.get() );
            file.setClassProperties( properties );
            file.setClassProperty( "attr3005-0", 60 );
            file.setClassProperty( "attr3005-1", 25 );
            file.setClassProperty( "attr3005-2", 60 );
            fileIdList.add( file.save() );
        }

        // 准备文件，用于线程“d、更新文件c，其中文件属性匹配新旧file_matcher条件”
        for ( int i = ( 2 * fileNum ) / 3; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setMimeType( MimeType.PLAIN );
            file.setFileName( fileNameBase + i );
            file.setContent( new ByteArrayInputStream( bytes ) );
            ScmClassProperties properties = new ScmClassProperties(
                    classId.get() );
            file.setClassProperties( properties );
            file.setClassProperty( "attr3005-0", 60 );
            file.setClassProperty( "attr3005-1", 50 );
            file.setClassProperty( "attr3005-2", 49 );
            fileIdList.add( file.save() );
        }
    }

    private void createClassAndAttr() throws ScmException {
        try {
            ScmFactory.Class.deleteInstanceByName( ws, className );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.METADATA_CLASS_NOT_EXIST ) {
                throw e;
            }
        }
        ScmClass scmClass = ScmFactory.Class.createInstance( ws, className,
                className );
        classId = scmClass.getId();
        for ( int i = 0; i < 3; i++ ) {
            ScmAttributeConf conf = new ScmAttributeConf();
            String attrName = attrNameBase + i;
            conf.setName( attrName );
            conf.setDescription( attrName );
            conf.setDisplayName( attrName + "_display" );
            conf.setRequired( false );
            conf.setType( AttributeType.INTEGER );
            ScmIntegerRule rule = new ScmIntegerRule();
            rule.setMinimum( 0 );
            rule.setMaximum( 100 );
            conf.setCheckRule( rule );
            ScmAttribute attr = ScmFactory.Attribute.createInstance( ws, conf );
            scmClass.attachAttr( attr.getId() );
            attrIdList = new ArrayList<>();
            attrIdList.add( attr.getId() );
        }
    }
}
