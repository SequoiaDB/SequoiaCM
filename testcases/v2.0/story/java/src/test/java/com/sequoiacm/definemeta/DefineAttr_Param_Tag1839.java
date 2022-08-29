package com.sequoiacm.definemeta;

import java.util.Set;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-1839:addTag/removeTag/setTags接口校验
 * @author fanyu
 * @Date:2018年7月7日
 * @version:1.0
 */
public class DefineAttr_Param_Tag1839 extends TestScmBase {
    private String name = "Param1839";
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private ScmId batchId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        this.prepareScmFile();
        this.prepareBatch();
    }

    @Test
    private void test() throws ScmException {
        ScmTags tags = new ScmTags();
        // testValueIsDot
        tags.addTag( "." );
        // testValueIsDollar
        tags.addTag( "$1" );
        // testValueWithDot
        tags.addTag( "18..39" );
        // testValueWithDollar
        tags.addTag( " 18$39$" );
        // testValueIsAll
        tags.addTag(
                "1234567890 qertyuiopasdfghjkwzxcvbnml !@#$%^&*(){}|_+:\"<>?" );
        // testValueIsChinese
        tags.addTag( "标签1" );
        Set< String > expTags = tags.toSet();

        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
        // test scm file
        file.setTags( tags );
        file = ScmFactory.File.getInstance( ws, fileId );
        Set< String > actFileTags = file.getTags().toSet();
        Assert.assertEquals( actFileTags, expTags, file.getTags().toString() );

        // test scm batch
        batch.setTags( tags );
        batch = ScmFactory.Batch.getInstance( ws, batch.getId() );
        Set< String > actBatchTags = batch.getTags().toSet();
        Assert.assertEquals( actBatchTags, expTags,
                batch.getTags().toString() );

        // test remove
        Set< String > tagSet = tags.toSet();
        for ( String tag : tagSet ) {
            file.removeTag( tag );
            batch.removeTag( tag );
        }
        // check result
        file = ScmFactory.File.getInstance( ws, fileId );
        batch = ScmFactory.Batch.getInstance( ws, batchId );
        Assert.assertEquals( file.getTags().toSet().size(), 0,
                file.getTags().toString() );
        Assert.assertEquals( batch.getTags().toSet().size(), 0,
                batch.getTags().toString() );
    }

    @Test
    private void testTagIsNull1() throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
        ScmTags scmTags = new ScmTags();
        try {
            scmTags.addTag( null );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
        // test scm file
        try {
            file.addTag( null );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
        // test scm batch
        try {
            batch.addTag( null );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @Test
    private void testValueIsBlankStr() throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId );
        ScmTags scmTags = new ScmTags();
        try {
            scmTags.addTag( "" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
        // test scm file
        try {
            file.addTag( "" );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
        // test scm batch
        try {
            batch.addTag( "" );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ScmFactory.File.deleteInstance( ws, fileId, true );
        ScmFactory.Batch.deleteInstance( ws, batchId );
        if ( session != null ) {
            session.close();
        }
    }

    private void prepareScmFile() throws ScmException {
        // upload file
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( name );
        fileId = file.save();
    }

    private void prepareBatch() throws ScmException {
        // upload batch
        ScmBatch scmBatch = ScmFactory.Batch.createInstance( ws );
        scmBatch.setName( name );
        batchId = scmBatch.save();
    }
}
