package com.sequoiacm.batch;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.util.JSON;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmClass;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmClassProperties;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @Description DefineAttr_class1388.java
 * @author luweikang
 * @date 2018年7月4日
 */
public class DefineAttr_class1936 extends TestScmBase {
    private final String batchName = "sameBatch1936";
    private boolean runSuccess = false;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId batchId = null;
    private String className = "class1936";
    private ScmId scmClassId = null;
    private ArrayList< ScmAttribute > attrList = new ArrayList< ScmAttribute >();

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        SiteWrapper site = ScmInfo.getRootSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( ScmInfo.getWs().getName(),
                session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        String attrStr1 = "{name:'attr1936_string', "
                + "display_name:'dispalyName1936_1', description:'I am a "
                + "Attribute 1936_1', type:'STRING', required:true}";
        String attrStr2 = "{name:'attr1936_date', "
                + "display_name:'dispalyName1936_2', description:'I am a "
                + "Attribute 1936_2', type:'STRING', required:false}";
        String attrStr3 = "{name:'attr1936_double', "
                + "display_name:'dispalyName1936_3', description:'中文', "
                + "type:'DOUBLE', required:true}";

        attrList.add( createAttr( attrStr1 ) );
        attrList.add( createAttr( attrStr2 ) );
        attrList.add( createAttr( attrStr3 ) );

        ScmClass scmClass = ScmFactory.Class.createInstance( ws, className,
                "i am a class1388" );
        scmClassId = scmClass.getId();

        for ( ScmAttribute attribute : attrList ) {
            scmClass.attachAttr( attribute.getId() );
        }

        ScmClassProperties props = new ScmClassProperties(
                scmClassId.toString() );
        props.addProperty( attrList.get( 0 ).getName(), "中文测试" );
        props.addProperty( attrList.get( 1 ).getName(),
                attrList.get( 1 ).getCreateTime().toString() );
        props.addProperty( attrList.get( 2 ).getName(), 1234567.7654321 );

        ScmTags tags = new ScmTags();
        tags.addTag( "我是一个标签1936" );

        ScmBatch batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( batchName );
        batch.setClassProperties( props );
        batch.setTags( tags );
        batchId = batch.save();
        checkResult( batch, attrList );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Class.deleteInstance( ws, scmClassId );
                for ( ScmAttribute attribute : attrList ) {
                    ScmFactory.Attribute.deleteInstance( ws,
                            attribute.getId() );
                }
                ScmFactory.Batch.deleteInstance( ws, batchId );
            }
        } finally {
            if ( session != null )
                session.close();
        }
    }

    private ScmAttribute createAttr( String attrObjString )
            throws ScmException {
        BSONObject attrObj = ( BSONObject ) JSON.parse( attrObjString );
        ScmAttributeConf attr = new ScmAttributeConf();
        attr.setName( attrObj.get( "name" ).toString() );
        attr.setType(
                AttributeType.getType( attrObj.get( "type" ).toString() ) );
        attr.setDescription( attrObj.get( "description" ).toString() );
        attr.setDisplayName( attrObj.get( "display_name" ).toString() );
        attr.setCheckRule( null );
        attr.setRequired( ( boolean ) attrObj.get( "required" ) );

        return ScmFactory.Attribute.createInstance( ws, attr );
    }

    private void checkResult( ScmBatch expBatch,
            List< ScmAttribute > expAttrList ) throws ScmException {
        ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
        Assert.assertEquals( batch.getClassProperties().toString(),
                expBatch.getClassProperties().toString() );
        Assert.assertEquals( batch.getTags().toString(),
                expBatch.getTags().toString() );
        Assert.assertEquals( batch.getClassId(), expBatch.getClassId() );

        ScmClass scmClass = ScmFactory.Class.getInstance( ws,
                batch.getClassId() );
        List< ScmAttribute > actAttrList = scmClass.listAttrs();

        Assert.assertEquals( actAttrList.size(), expAttrList.size() );

        for ( ScmAttribute actScmAttribute : actAttrList ) {
            boolean result = false;
            for ( ScmAttribute expScmAttribute : expAttrList ) {
                if ( actScmAttribute.toString()
                        .equals( expScmAttribute.toString() ) ) {
                    result = true;
                    break;
                }
            }
            Assert.assertTrue( result, "exp: " + expAttrList.toString()
                    + ";   act: " + actAttrList.toString() );
        }
    }

}
