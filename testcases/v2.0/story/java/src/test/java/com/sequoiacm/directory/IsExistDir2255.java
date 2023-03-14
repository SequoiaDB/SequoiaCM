package com.sequoiacm.directory;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-2255:存在与文件夹同名的文件，查看文件夹是否存在
 * @author fanyu
 * @Date:2018年09月25日
 * @version:1.0
 */
public class IsExistDir2255 extends TestScmBase {
    private boolean runSuccess;
    private ScmSession session;
    private ScmWorkspace ws;
    private SiteWrapper site;
    private WsWrapper wsp;
    private String dirBasePath = "/IsExistDir2255/";
    private String path = dirBasePath + "IsExistDir2255_A";
    private String name = "IsExistDir2255";
    private ScmId fileId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = ScmSessionUtils.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { GroupTags.base })
    private void test() throws ScmException {
        // create directory
        ScmDirectory dir = ScmFactory.Directory.createInstance( ws,
                dirBasePath );
        ScmDirectory subdir = ScmFactory.Directory.createInstance( ws, path );
        subdir.createSubdirectory( name );

        // create file
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( name );
        file.setDirectory( dir );
        fileId = file.save();

        boolean flag = ScmFactory.Directory.isInstanceExist( ws,
                dirBasePath + name );
        Assert.assertFalse( flag );

        ScmFactory.File.deleteInstance( ws, fileId, true );

        // create directory
        ScmFactory.Directory.createInstance( ws, dirBasePath + name );
        boolean flag1 = ScmFactory.Directory.isInstanceExist( ws,
                dirBasePath + name );
        Assert.assertTrue( flag1 );

        // clear
        ScmFactory.Directory.deleteInstance( ws, dirBasePath + name );
        ScmFactory.Directory.deleteInstance( ws, path + "/" + name );
        ScmFactory.Directory.deleteInstance( ws, path );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Directory.deleteInstance( ws, dirBasePath );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
