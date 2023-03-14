package com.sequoiacm.definemeta.concurrent;

import java.io.IOException;
import java.util.Collections;
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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-2713:多线程并发删除和添加标签（批次）
 * @author fanyu
 * @Date:2019年11月06日
 * @version:1.0
 */

public class DefineAttr_tags_removeAndAddTags2713B extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession session;
    private ScmWorkspace ws;
    private String name = "definemeta2613B";
    private String[] initTags = { "2613Ba", "2613Bb", "2613Bc" };
    private ScmId batchId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( name ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        prepareBatch();
    }

    @Test(enabled = false) // TODO:SEQUOIACM-439
    private void test() throws Exception {
        String commonTag = initTags[ 0 ];
        Set< String > addTags = new HashSet<>();
        addTags.add( commonTag );
        addTags.add( commonTag + "-1" );
        addTags.add( commonTag + "-2" );

        Set< String > removeTags = new HashSet<>();
        removeTags.add( commonTag );
        removeTags.add( initTags[ 1 ] );

        ThreadExecutor threadExec = new ThreadExecutor();
        threadExec.addWorker( new RemoveTags( removeTags ) );
        threadExec.addWorker( new AddTags( addTags ) );
        threadExec.run();

        ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
        Set< String > actSet = batch.getTags().toSet();
        Set< String > expSet = new HashSet<>();
        Collections.addAll( expSet, initTags );
        // check results
        if ( actSet.contains( commonTag ) ) {
            expSet.removeAll( removeTags );
            expSet.addAll( addTags );
        } else {
            expSet.addAll( addTags );
            expSet.removeAll( removeTags );
        }
        Assert.assertEquals( actSet, expSet );
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

    private void prepareBatch() throws ScmException {
        // define tags
        Set< String > tagsSet = new HashSet<>();
        for ( String tag : initTags ) {
            tagsSet.add( tag );
        }
        ScmTags tags = new ScmTags();
        tags.addTags( tagsSet );
        // upload batch and set tags
        ScmBatch batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( name );
        batch.setTags( tags );
        batchId = batch.save();
    }

    private class RemoveTags {
        private Set< String > tags;

        public RemoveTags( Set< String > tags ) {
            this.tags = tags;
        }

        @ExecuteOrder(step = 1)
        public void remove() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
                for ( String tag : tags ) {
                    batch.removeTag( tag );
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class AddTags {
        private Set< String > tags;

        public AddTags( Set< String > tags ) {
            this.tags = tags;
        }

        @ExecuteOrder(step = 1)
        public void add() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
                for ( String tag : tags ) {
                    batch.addTag( tag );
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
