package com.sequoiacm.file;

import java.io.IOException;
import java.util.List;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;

public class ReadFileSiteListContainNull extends ScmTestMultiCenterBase {

    private ScmFile bScmFile;
    private String centerCoord;
    private ScmSession bSiteSs;
    private ScmWorkspace ws;

    @BeforeClass
    public void setUp() throws ScmException {

        centerCoord = getSdb1().getUrl();

        // site A createFile
        bSiteSs = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer2().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), bSiteSs);
        bScmFile = ScmTestTools.createScmFile(ws, null, ScmTestTools.getClassName(), "",
                "testTitle");
    }

    @Test
    public void readFile() throws ScmException, IOException {
        // push null to site_list
        Sequoiadb rootSdb = ScmTestTools.getSequoiadb(centerCoord, getSdbUser(), getSdbPasswd());
        String csName = getWorkspaceName() + "_META";
        String clName = "FILE";
        DBCollection cl = rootSdb.getCollectionSpace(csName).getCollection(clName);
        BasicBSONObject matcher = new BasicBSONObject(ScmAttributeName.File.FILE_ID,
                bScmFile.getFileId().get());

        BasicBSONObject nullEle = new BasicBSONObject(ScmAttributeName.File.SITE_LIST, null);
        BasicBSONObject modifier = new BasicBSONObject("$push", nullEle);
        cl.update(matcher, modifier, null);
        cl.update(matcher, modifier, null);

        ScmFile file = ScmFactory.File.getInstance(ws, bScmFile.getFileId());
        List<ScmFileLocation> locationList = file.getLocationList();
        Assert.assertEquals(locationList.size() == 1, true, locationList.toString());
        Assert.assertEquals(locationList.get(0) != null, true, locationList.toString());
    }

    @AfterClass
    public void tearDown() throws ScmException {
        bScmFile.delete(true);
        bSiteSs.close();
    }
}
