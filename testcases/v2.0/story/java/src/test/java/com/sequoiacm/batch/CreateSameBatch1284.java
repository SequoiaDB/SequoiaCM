package com.sequoiacm.batch;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmBatchInfo;
import com.sequoiacm.client.core.ScmClass;
import com.sequoiacm.client.core.ScmCursor;
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
 * @FileName SCM-1284: 创建多个所有属性相同的批次
 * @Author linsuqiang
 * @Date 2018-04-19
 * @Version 1.00
 */

public class CreateSameBatch1284 extends TestScmBase {
    private final int batchNum = 5;
    private final String batchName = "sameBatch1284";
    private boolean runSuccess = false;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String className = "class1284";
    private String attrName = "attr1284";
    private ScmId scmClassId = null;
    private ScmId attrId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        SiteWrapper site = ScmInfo.getRootSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace
                .getWorkspace( ScmInfo.getWs().getName(), session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        ScmAttributeConf attr = new ScmAttributeConf();
        attr.setName( attrName );
        attr.setType( AttributeType.STRING );
        attr.setDescription( "I am a Attribute1284" );
        attr.setDisplayName( attrName + "_display" );
        ScmAttribute scmAttribute = ScmFactory.Attribute
                .createInstance( ws, attr );
        ScmClass scmClass = ScmFactory.Class
                .createInstance( ws, className, "i am a class1284" );
        scmClass.attachAttr( scmAttribute.getId() );

        scmClassId = scmClass.getId();
        attrId = scmAttribute.getId();

        ScmClassProperties props = new ScmClassProperties(
                scmClassId.toString() );
        props.addProperty( attrName, "I am a property1284" );
        ScmTags tags = new ScmTags();
        tags.addTag( "我是一个标签1284" );

        for ( int i = 0; i < batchNum; ++i ) {
            ScmBatch batch = ScmFactory.Batch.createInstance( ws );
            batch.setName( batchName );
            batch.setClassProperties( props );
            batch.setTags( tags );
            batch.save();
        }

        int actBatchNum = 0;
        ScmCursor< ScmBatchInfo > cursor = ScmFactory.Batch
                .listInstance( ws, new BasicBSONObject( "name", batchName ) );
        try {
            while ( cursor.hasNext() ) {
                ScmBatchInfo info = cursor.getNext();
                Assert.assertEquals( batchName, info.getName() );
                Assert.assertEquals( 0, info.getFilesCount() );

                ScmId batchId = info.getId();
                ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
                Assert.assertEquals( batch.getClassProperties().toString(),
                        props.toString() );
                Assert.assertEquals( batch.getTags().toString(),
                        tags.toString() );
                Assert.assertEquals( batch.getClassId(), scmClassId );
                ++actBatchNum;
            }
        } finally {
            cursor.close();
        }
        Assert.assertEquals( actBatchNum, batchNum );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Class.deleteInstance( ws, scmClassId );
                ScmFactory.Attribute.deleteInstance( ws, attrId );
                ScmCursor< ScmBatchInfo > cursor = ScmFactory.Batch
                        .listInstance( ws,
                                new BasicBSONObject( "name", batchName ) );
                while ( cursor.hasNext() ) {
                    ScmId batchId = cursor.getNext().getId();
                    ScmFactory.Batch.deleteInstance( ws, batchId );
                }
                cursor.close();
            }
        } finally {
            if ( session != null )
                session.close();
        }
    }
}