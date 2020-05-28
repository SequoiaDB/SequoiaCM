package com.sequoiacm.batch;

import org.junit.Assert;
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
 * @FileName SCM-1296: 更新批次所有属性
 * @Author linsuqiang
 * @Date 2018-04-19
 * @Version 1.00
 */

public class UpdateBatchAttr1296 extends TestScmBase {
    private boolean runSuccess = false;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId batchId = null;
    private String oldClassName = "class1284_old";
    private String newClassName = "class1284_new";
    private String oldAttrName = "attr1284_old";
    private String newAttrName = "attr1284_new";
    private ScmId oldScmClassId = null;
    private ScmId newScmClassId = null;
    private ScmId oldAttrId = null;
    private ScmId newAttrId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        SiteWrapper site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( ScmInfo.getWs().getName(),
                session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        ScmTags oldTags = new ScmTags();
        oldTags.addTag( "oldTag1296" );
        String oldBatchName = "batch1296_old";

        ScmAttributeConf oldAttr = new ScmAttributeConf();
        oldAttr.setName( oldAttrName );
        oldAttr.setType( AttributeType.STRING );
        oldAttr.setDescription( "I am a Attribute1296" );
        oldAttr.setDisplayName( oldAttrName + "_display" );
        ScmAttribute oldScmAttribute = ScmFactory.Attribute.createInstance( ws,
                oldAttr );
        ScmClass oldScmClass = ScmFactory.Class.createInstance( ws,
                oldClassName, "i am a old class1296" );
        oldScmClass.attachAttr( oldScmAttribute.getId() );

        oldScmClassId = oldScmClass.getId();
        oldAttrId = oldScmAttribute.getId();

        ScmClassProperties oldProps = new ScmClassProperties(
                oldScmClassId.toString() );
        oldProps.addProperty( oldAttrName, "I am a old properties" );

        ScmBatch batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( oldBatchName );
        batch.setTags( oldTags );
        batch.setClassProperties( oldProps );
        batchId = batch.save();

        batch = ScmFactory.Batch.getInstance( ws, batchId );
        Assert.assertEquals( batch.getName(), oldBatchName );
        Assert.assertEquals( batch.getClassId(), oldScmClassId );
        Assert.assertEquals( batch.getTags().toString(), oldTags.toString() );
        Assert.assertEquals( batch.getClassProperties().toString(),
                oldProps.toString() );

        ScmTags newTags = new ScmTags();
        newTags.addTag( "newTag1296" );
        String newBatchName = "batch1296_new";

        ScmAttributeConf newAttr = new ScmAttributeConf();
        newAttr.setName( newAttrName );
        newAttr.setType( AttributeType.STRING );
        newAttr.setDescription( "I am a Attribute1296" );
        newAttr.setDisplayName( newAttrName + "_display" );
        ScmAttribute newScmAttribute = ScmFactory.Attribute.createInstance( ws,
                newAttr );
        ScmClass newScmClass = ScmFactory.Class.createInstance( ws,
                newClassName, "i am a new class1296" );
        newScmClass.attachAttr( newScmAttribute.getId() );

        newScmClassId = newScmClass.getId();
        newAttrId = newScmAttribute.getId();

        ScmClassProperties newProps = new ScmClassProperties(
                newScmClassId.toString() );
        newProps.addProperty( newAttrName, "I am a new properties" );
        batch.setName( newBatchName );
        batch.setTags( newTags );
        batch.setClassProperties( newProps );

        batch = ScmFactory.Batch.getInstance( ws, batchId );
        Assert.assertEquals( batch.getName(), newBatchName );
        Assert.assertEquals( batch.getClassId(), newScmClassId );
        Assert.assertEquals( batch.getTags().toString(), newTags.toString() );
        Assert.assertEquals( batch.getClassProperties().toString(),
                newProps.toString() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Batch.deleteInstance( ws, batchId );
                ScmFactory.Class.deleteInstance( ws, oldScmClassId );
                ScmFactory.Class.deleteInstance( ws, newScmClassId );
                ScmFactory.Attribute.deleteInstance( ws, oldAttrId );
                ScmFactory.Attribute.deleteInstance( ws, newAttrId );
            }
        } finally {
            if ( session != null )
                session.close();
        }
    }
}