package com.sequoiacm.batch;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmClass;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmClassProperties;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @FileName SCM-1290: 获取已存在的批次实例 SCM-1291: 获取不存在的批次实例
 * @Author linsuqiang
 * @Date 2018-04-19
 * @Version 1.00
 */

public class GetBatch1290 extends TestScmBase {
    private final String batchName = "batch1290";
    private final int fileNum = 5;
    private boolean runSuccess = false;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList = new ArrayList<>( fileNum );
    private ScmId batchId = null;
    private String className = "class1290";
    private String attrName = "attr1290";
    private ScmId scmClassId = null;
    private ScmId attrId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        SiteWrapper site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( ScmInfo.getWs().getName(),
                session );

        for ( int i = 0; i < fileNum; ++i ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( "file1290_" + i );
            file.setTitle( batchName );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        ScmAttributeConf attr = new ScmAttributeConf();
        attr.setName( attrName );
        attr.setType( AttributeType.STRING );
        attr.setDescription( "I am a Attribute1290" );
        attr.setDisplayName( attrName + "_display" );
        ScmAttribute scmAttribute = ScmFactory.Attribute.createInstance( ws,
                attr );
        ScmClass scmClass = ScmFactory.Class.createInstance( ws, className,
                "i am a class1290" );
        scmClass.attachAttr( scmAttribute.getId() );

        scmClassId = scmClass.getId();
        attrId = scmAttribute.getId();

        ScmClassProperties props = new ScmClassProperties(
                scmClassId.toString() );
        ScmBatch batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( batchName );
        ScmTags tags = new ScmTags();
        props.addProperty( attrName, "props1290" );
        batch.setTags( tags );
        batch.setClassProperties( props );
        batchId = batch.save();
        for ( ScmId fileId : fileIdList ) {
            batch.attachFile( fileId );
        }

        batch = ScmFactory.Batch.getInstance( ws, batchId );
        Assert.assertEquals( batch.getName(), batchName );
        Assert.assertEquals( batch.getId().get(), batchId.get() );
        Assert.assertEquals( batch.getTags().toString(), tags.toString() );
        Assert.assertEquals( batch.getClassProperties().toString(),
                props.toString() );
        Assert.assertEquals( batch.getClassId(), scmClassId );
        List< ScmFile > files = batch.listFiles();
        Assert.assertEquals( files.size(), fileNum );
        for ( ScmFile file : files ) {
            Assert.assertEquals( file.getTitle(), batchName );
        }

        ScmId inexistentId = new ScmId( "ffffffffffffffffffffffff" );
        try {
            ScmFactory.Batch.getInstance( ws, inexistentId );
            Assert.fail( "get not exist batch should not succeed" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.BATCH_NOT_FOUND );
        }

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Batch.deleteInstance( ws, batchId );
                ScmFactory.Class.deleteInstance( ws, scmClassId );
                ScmFactory.Attribute.deleteInstance( ws, attrId );
            }
        } finally {
            if ( session != null )
                session.close();
        }
    }
}