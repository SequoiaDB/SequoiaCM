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
 * @Testcase: SCM-1610:批量重复多次添加
 *				SCM-1611:批量多次添加不重复的标签
 * @author huangxiaoni init
 * @date 2017.6.22
 */

public class DefineAttr_tags_setTags_1610_1611 extends TestScmBase {
    private boolean runSuccess = false;
    private String name = "defineTags1610";
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
        this.prepareScmFile();
        this.prepareBatch();
    }

    @Test
    private void test() throws Exception {
        test_setTags01();
        test_setTags02();
        runSuccess = true;
    }

    //SCM-1610:批量重复多次添加
    private void test_setTags01() throws Exception {
        // define tags
        Set< String > tagSet = new HashSet<>();
        tagSet.add( "test" );
        tagSet.add( "123" );
        ScmTags tags1 = new ScmTags();
        tags1.addTags( tagSet );

        Set< String > tagSet2 = new HashSet<>();
        tagSet2.add( "test" );
        tagSet2.add( "123" );
        ScmTags tags2 = new ScmTags();
        tags2.addTags( tagSet2 );

        // test scm file upload file and set tags
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        file.setTags( tags1 );
        file.setTags( tags2 );
        // check results
        file = ScmFactory.File.getInstance( ws, fileId );
        ScmTags fileTags = file.getTags();
        Assert.assertEquals( fileTags.toSet().size(), tagSet2.size(),
                "fileTags = " + fileTags.toString() + ",tagSet2 = " +
                        tagSet2.toString() );
        Assert.assertTrue( fileTags.toSet().containsAll( tagSet2 ),
                "fileTags = " + fileTags.toString() + ",tagSet2 = " +
                        tagSet2.toString() );

        //test scm batch
        ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
        batch.setTags( tags1 );
        batch.setTags( tags2 );
        batch = ScmFactory.Batch.getInstance( ws, batchId );
        ScmTags batchTags = batch.getTags();
        Assert.assertEquals( batchTags.toSet().size(), tagSet2.size(),
                "fileTags = " + batchTags.toString() + ",tagSet2 = " +
                        tagSet2.toString() );
        Assert.assertTrue( batchTags.toSet().containsAll( tagSet2 ),
                "fileTags = " + batchTags.toString() + ",tagSet2 = " +
                        tagSet2.toString() );
    }

    //SCM-1611:批量多次添加不重复的标签
    private void test_setTags02() throws Exception {
        // define tags
        Set< String > tagSet = new HashSet<>();
        tagSet.add( "true" );
        tagSet.add( "false" );
        ScmTags scmTags = new ScmTags();
        scmTags.addTags( tagSet );

        // test scm file set tags
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        file.setTags( scmTags );
        // check results
        file = ScmFactory.File.getInstance( ws, fileId );
        ScmTags fileTags = file.getTags();
        Assert.assertEquals( fileTags.toSet().size(), tagSet.size(),
                "fileTags = " + fileTags.toString() + ",tagSet2 = " +
                        tagSet.toString() );
        Assert.assertTrue( fileTags.toSet().containsAll( tagSet ),
                "fileTags = " + fileTags.toString() + ",tagSet2 = " +
                        tagSet.toString() );

        //test scm batch set tags
        ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
        batch.setTags( scmTags );
        // check results
        batch = ScmFactory.Batch.getInstance( ws, batchId );
        ScmTags batchTags = batch.getTags();
        Assert.assertEquals( batchTags.toSet().size(), tagSet.size(),
                "fileTags = " + batchTags.toString() + ",tagSet2 = " +
                        tagSet.toString() );
        Assert.assertTrue( batchTags.toSet().containsAll( tagSet ),
                "fileTags = " + batchTags.toString() + ",tagSet2 = " +
                        tagSet.toString() );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void prepareScmFile() throws ScmException {
        // define tags
        ScmTags tags = new ScmTags();
        tags.addTag( "k1" );
        tags.addTag( "k2" );

        // upload file and set tags
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( name );
        file.setTags( tags );
        fileId = file.save();
    }

    private void prepareBatch() throws ScmException {
        // define tags
        Set< String > tagSet = new HashSet<>();
        tagSet.add( "k1" );
        tagSet.add( "k2" );
        ScmTags tags = new ScmTags();
        tags.addTags( tagSet );
        // upload file and set tags
        ScmBatch scmBatch = ScmFactory.Batch.createInstance( ws );
        scmBatch.setTags( tags );
        scmBatch.setName( name );
        batchId = scmBatch.save();
    }
}