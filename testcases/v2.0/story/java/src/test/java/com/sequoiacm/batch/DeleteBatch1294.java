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
 * @FileName SCM-1294: 删除有正常文件的批次
 * @Author linsuqiang
 * @Date 2018-04-19
 * @Version 1.00
 */

public class DeleteBatch1294 extends TestScmBase {
    private final String batchName = "batch1294";
    private final int fileNum = 5;
    private boolean runSuccess = false;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList = new ArrayList<>( fileNum );
    private ScmId batchId = null;
    private String className = "class1294";
    private String attrName = "attr1294";
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
            file.setFileName( "file1294_" + i );
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
        attr.setDescription( "I am a Attribute1294" );
        attr.setDisplayName( attrName + "_display" );
        ScmAttribute scmAttribute = ScmFactory.Attribute.createInstance( ws,
                attr );
        ScmClass scmClass = ScmFactory.Class.createInstance( ws, className,
                "i am a class1294" );
        scmClass.attachAttr( scmAttribute.getId() );

        scmClassId = scmClass.getId();
        attrId = scmAttribute.getId();

        ScmClassProperties props = new ScmClassProperties(
                scmClassId.toString() );
        ScmBatch batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( batchName );
        ScmTags tags = new ScmTags();
        props.addProperty( attrName, "props1294" );
        batch.setTags( tags );
        batch.setClassProperties( props );
        batchId = batch.save();
        for ( ScmId fileId : fileIdList ) {
            batch.attachFile( fileId );
        }

        batch.delete();

        try {
            ScmFactory.Batch.getInstance( ws, batchId );
            Assert.fail( "get not exist batch should not succeed" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.BATCH_NOT_FOUND );
        }

        for ( ScmId fileId : fileIdList ) {
            try {
                ScmFactory.File.getInstance( ws, fileId );
                Assert.fail( "file should not exist" );
            } catch ( ScmException e ) {
                Assert.assertEquals( e.getError(), ScmError.FILE_NOT_FOUND );
            }
        }

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Class.deleteInstance( ws, scmClassId );
                ScmFactory.Attribute.deleteInstance( ws, attrId );
            }
        } finally {
            if ( session != null )
                session.close();
        }
    }
}