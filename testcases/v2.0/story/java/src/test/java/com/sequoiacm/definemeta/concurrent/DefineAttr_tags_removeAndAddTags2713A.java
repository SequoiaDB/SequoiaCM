package com.sequoiacm.definemeta.concurrent;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-2713:多线程并发删除和添加标签（文件）
 * @author fanyu
 * @Date:2019年11月06日
 * @version:1.0
 */

public class DefineAttr_tags_removeAndAddTags2713A extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession session;
    private ScmWorkspace ws;
    private String name = "definemeta2613";
    private String[] initTags = { "2613Aa", "2613Ab", "2613Ac" };
    private ScmId fileId;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( name ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        prepareScmFile();
    }

    @Test(enabled = false) // TODO:SEQUOIACM-439
    private void test() throws Exception {
        String commonTag = initTags[ 0 ];
        Set< String > addTags = new HashSet<>();
        addTags.add( commonTag );
        addTags.add( commonTag + UUID.randomUUID() );
        addTags.add( commonTag + UUID.randomUUID() );

        Set< String > removeTags = new HashSet<>();
        removeTags.add( commonTag );
        removeTags.add( initTags[ 1 ] );

        ThreadExecutor threadExec = new ThreadExecutor();
        threadExec.addWorker( new RemoveTags( removeTags ) );
        threadExec.addWorker( new AddTags( addTags ) );
        threadExec.run();

        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        Set< String > actSet = file.getTags().toSet();
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
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
    }

    private void prepareScmFile() throws ScmException {
        // define tags
        Set< String > tagsSet = new HashSet<>();
        for ( String tag : initTags ) {
            tagsSet.add( tag );
        }
        ScmTags tags = new ScmTags();
        tags.addTags( tagsSet );
        // upload file and set tags
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( name );
        file.setTags( tags );
        fileId = file.save();
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
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                for ( String tag : tags ) {
                    file.removeTag( tag );
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
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                for ( String tag : tags ) {
                    file.addTag( tag );
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
