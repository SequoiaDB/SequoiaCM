package com.sequoiacm.definemeta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmClass;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmClassProperties;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.element.metadata.ScmStringRule;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-1625:setClassProperty接口校验
 * 			   SCM-1839:setTag接口校验
 * @author huangxiaoni init
 * @date 2017.6.26
 */

public class DefineAttr_setPropAndTag_specialChar_1625_1839
        extends TestScmBase {
    private boolean runSuccess = false;
    private String name = "definemeta1625";
    private String classId = null;
    private ScmClass class1 = null;
    private List< ScmAttribute > attrList = new ArrayList< ScmAttribute >();
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( name ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        class1 = ScmFactory.Class.createInstance( ws, name, name + "_desc" );
        classId = class1.getId().get();
        // create class properties
        createModel( name );
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( name );
        fileId = file.save();
    }

    @Test // jira-312
    private void test_setMultiProNotRepeat() throws Exception {
        // define properties and tags
        String propStr1 = "test_attr_name_str_1625";
        Map< String, Object > propMap = new HashMap<>();
        propMap.put( propStr1, propStr1 );

        // set properties/property
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmClassProperties properties = new ScmClassProperties( classId );
        properties.addProperties( propMap );
        file.setClassProperties( properties );

        // set tags and tag
        String tagStr1 = "STR..中文1";
        String tagStr2 = "STR..中文2";
        Set< String > tagSet = new HashSet<>();
        tagSet.add( tagStr1 );
        tagSet.add( tagStr2 );
        ScmTags tags = new ScmTags();
        tags.addTags( tagSet );
        file.setTags( tags );

        // check results
        file = ScmFactory.File.getInstance( ws, fileId );
        Map< String, Object > actPropMap = file.getClassProperties().toMap();
        Map< String, Object > expPropMap = new HashMap<>();
        expPropMap.put( "test_attr_name_str_1625", "test_attr_name_str_1625" );
        Assert.assertEquals( actPropMap, expPropMap );

        Set< String > actTags = file.getTags().toSet();
        Assert.assertEquals( actTags.size(), tagSet.size(),
                "actTags = " + actTags.toString() + ",tagSet = " +
                        tagSet.toString() );
        Assert.assertEquals( actTags.containsAll( tagSet ), true );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                ScmFactory.Class.deleteInstance( ws, class1.getId() );
                for ( ScmAttribute attr : attrList ) {
                    ScmFactory.Attribute.deleteInstance( ws, attr.getId() );
                }
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void createModel( String name ) throws ScmException {
        // createattr
        ScmAttributeConf conf = new ScmAttributeConf();
        conf.setName( "test_attr_name_str_1625" );
        conf.setDescription( "test_attr_name_str_1625" );
        conf.setDisplayName( name + "_display" );
        conf.setRequired( true );
        conf.setType( AttributeType.STRING );
        ScmStringRule rule = new ScmStringRule( 100 );
        conf.setCheckRule( rule );
        ScmAttribute attr = ScmFactory.Attribute.createInstance( ws, conf );
        // attr attch class
        class1.attachAttr( attr.getId() );
        attrList.add( attr );
    }
}