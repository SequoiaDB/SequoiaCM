package com.sequoiacm.definemeta.concurrent;

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
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @FileName SCM-1620:多线程并发添加/更新相同标签
 * @Author huangxioni
 * @Date 2018/6/26
 */

public class DefineAttr_tags_addAndSetTags1620_1 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String name = "definemeta1620_1";
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
    }

    @Test(enabled = false) //SEQUOIACM-439
    private void test() throws ScmException {
        //Random random = new Random();
        AddTags addTags = new AddTags();
        SetTags setTags = new SetTags();
        addTags.start( 20 );
        setTags.start( 20 );
        if ( !( addTags.isSuccess() && setTags.isSuccess() ) ) {
            Assert.fail( addTags.getErrorMsg() + setTags.getErrorMsg() );
        }
        // check results
        ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
        ScmTags tags = batch.getTags();
        Set< String > actSet = tags.toSet();

        Set< String > expSet1 = new HashSet<>();
        expSet1.add( "3" );
        expSet1.add( "4" );

        Set< String > expSet2 = new HashSet<>();
        expSet2.add( "3" );
        expSet2.add( "4" );
        expSet2.add( "5" );
        expSet2.add( "6" );

//		Set<String> expSet3 = new HashSet<>();
//		expSet3.add("3");
//		expSet3.add("6");

        if ( !actSet.equals( expSet1 ) && /*!actMap.equals(expMap2) && */
                !actSet.equals( expSet2 ) ) {
            Assert.fail(
                    "check results failed. actMap = " + actSet.toString() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Batch.deleteInstance( ws, batchId );
            }
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
    }

    private void prepareScmFile() throws ScmException {
        // define tags
        Set< String > set = new HashSet< String >();
        set.add( "1" );
        set.add( "2" );
        ScmTags tags = new ScmTags();
        tags.addTags( set );

        // upload batch and set tags
        ScmBatch batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( name );
        batchId = batch.save();
    }

    private class SetTags extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
                Set< String > tagsSet = new HashSet<>();
                tagsSet.add( "3" );
                tagsSet.add( "4" );
                ScmTags tags = new ScmTags();
                tags.addTags( tagsSet );
                batch.setTags( tags );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class AddTags extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
                batch.addTag( "5" );
                batch.addTag( "6" );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
