package com.sequoiacm.definemeta;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description: SCM-2628:设置/清空/删除/添加标签
 * @author fanyu
 * @Date:2019年09月27日
 * @version:1.0
 */

public class DefineAttr_tags_crudTags_2628 extends TestScmBase {
    private boolean runSuccess = false;
    private String name = "defineTags2628";
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private ScmId batchId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( name ).get();
        ScmFileUtils.cleanFile( wsp, cond );
    }

    @Test
    private void test() throws Exception {
        testFileTag();
        testBatchTag();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                ScmFactory.Batch.deleteInstance( ws, batchId );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void testFileTag() throws ScmException {
        //upload file
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( name );
        fileId = file.save();

        // define tags
        Set< String > tagSet = new HashSet<>();
        tagSet.add( "test" );
        tagSet.add( "123" );
        ScmTags tags = new ScmTags();
        tags.addTags( tagSet );

        //set tags
        file = ScmFactory.File.getInstance( ws, fileId );
        file.setTags( tags );
        file = ScmFactory.File.getInstance( ws, fileId );
        ScmTags fileTags = file.getTags();
        Assert.assertEquals( fileTags.toSet(), tags.toSet(),
                "fileTags = " + fileTags.toString() + ",tagSet = " +
                        tags.toString() );

        //set null
        file.setTags( null );
        file = ScmFactory.File.getInstance( ws, fileId );
        Assert.assertEquals( file.getTags().toString(), "[]",
                "fileTags = " + file.getTags().toString() + ",tagSet = " +
                        tags.toString() );
        //remove tag
        file.removeTag( "test" );
        file = ScmFactory.File.getInstance( ws, fileId );
        Assert.assertEquals( file.getTags().toString(), "[]",
                "fileTags = " + file.getTags().toString() + ",tagSet = " +
                        tags.toString() );

        //add tag
        file.addTag( "test" );
        file = ScmFactory.File.getInstance( ws, fileId );
        Assert.assertEquals( file.getTags().toSet().size(), 1,
                file.getTags().toString() );
        Assert.assertTrue( file.getTags().toSet().contains( "test" ),
                file.getTags().toString() );
    }

    private void testBatchTag() throws ScmException {
        // upload batch
        ScmBatch batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( name );
        batchId = batch.save();

        // define tags
        Set< String > tagSet = new HashSet<>();
        tagSet.add( "test" );
        tagSet.add( "123" );
        ScmTags tags = new ScmTags();
        tags.addTags( tagSet );

        //set tags
        batch = ScmFactory.Batch.getInstance( ws, batchId );
        batch.setTags( tags );
        batch = ScmFactory.Batch.getInstance( ws, batchId );
        Assert.assertEquals( batch.getTags().toSet(), tags.toSet(),
                "actTags = " + batch.toString() + ",expTags = " +
                        tags.toString() );
        //set null
        batch.setTags( null );
        batch = ScmFactory.Batch.getInstance( ws, batchId );
        Assert.assertEquals( batch.getTags().toString(), "[]",
                "actTags = " + batch.getTags().toString() );

        //remove tag
        batch.removeTag( "test" );
        batch = ScmFactory.Batch.getInstance( ws, batchId );
        Assert.assertEquals( batch.getTags().toString(), "[]",
                "actTags = " + batch.getTags().toString() );

        //add tag
        batch.addTag( "test" );
        batch = ScmFactory.Batch.getInstance( ws, batchId );
        Assert.assertEquals( batch.getTags().toSet().size(), 1,
                batch.getTags().toString() );
        Assert.assertTrue( batch.getTags().toSet().contains( "test" ),
                batch.getTags().toString() );
    }
}