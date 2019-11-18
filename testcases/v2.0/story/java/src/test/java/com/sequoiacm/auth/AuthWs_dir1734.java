package com.sequoiacm.auth;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @FileName SCM-1734:查询到的目录权限精确匹配到末尾
 * @Author huangxioni
 * @Date 2018/6/7
 */

public class AuthWs_dir1734 extends TestScmBase {
    private static final Logger logger = Logger.getLogger(AuthWs_dir1734.class);
    private boolean runSuccess = false;

    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession ss = null;
    private ScmWorkspace ws = null;
    private ScmSession newSS = null;
    private ScmWorkspace newWS = null;

    private static final String NAME = "authws1734";
    private static final String PASSWORD = NAME;
    private static ScmRole role = null;

    private static final String[] DIR_PATH_ARRAY1 = { "/" + NAME + "_a/", "/" + NAME + "_a/" + NAME + "_a1/",
	    "/" + NAME + "_b/", "/" + NAME + "_b/" + NAME + "_b1/", "/" + NAME + "_b/" + NAME + "_b1/" + NAME + "_b2/",
	    "/" + NAME + "_c/", "/" + NAME + "_c/" + NAME + "_c1/",
	    "/" + NAME + "_c/" + NAME + "_c1/" + NAME + "_c2/" };

    private static final String[] DIR_PATH_ARRAY2 = { "/" + NAME + "_a/" + NAME + "_a1/",
	    "/" + NAME + "_b/" + NAME + "_b1/", "/" + NAME + "_b/" + NAME + "_b1/" + NAME + "_b2/",
	    "/" + NAME + "_c/" + NAME + "_c1/" + NAME + "_c2/" };

    private static List<ScmResource> resources = new ArrayList<>();

    private int fileSize = 0;
    private File localPath = null;
    private String filePath = null;
    private List<ScmId> fileIds = new ArrayList<>();

    @BeforeClass
    private void setUp() throws Exception {
	localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
	filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
	TestTools.LocalFile.removeFile(localPath);
	TestTools.LocalFile.createDir(localPath.toString());
	TestTools.LocalFile.createFile(filePath, fileSize);

	site = ScmInfo.getSite();
	wsp = ScmInfo.getWs();
	ss = TestScmTools.createSession(site);
	ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), ss);

	// clean scmFile
	BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(NAME).get();
	ScmFileUtils.cleanFile(wsp, cond);

	// clean users and roles
	try {
	    ScmFactory.User.deleteUser(ss, NAME);
	} catch (ScmException e) {
	    logger.info("clean users in setUp, errorMsg = [" + e.getError() + "]");
	}
	try {
	    ScmFactory.Role.deleteRole(ss, NAME);
	} catch (ScmException e) {
	    logger.info("clean roles in setUp, errorMsg = [" + e.getError() + "]");
	}

	// clean director
	logger.info("DIR_PATH_ARRAY1 info \n" + Arrays.toString(DIR_PATH_ARRAY1));
	for (int i = DIR_PATH_ARRAY1.length - 1; i >= 0; i--) {
	    try {
		ScmFactory.Directory.deleteInstance(ws, DIR_PATH_ARRAY1[i]);
	    } catch (ScmException e) {
		logger.info("clean dirPath in setUp, errorMsg = [" + e.getError() + "]");
	    }
	}

	// prepare user
	this.createUserAndRole();

	// prepare multiple director
	for (int i = 0; i < DIR_PATH_ARRAY1.length; i++) {
	    ScmFactory.Directory.createInstance(ws, DIR_PATH_ARRAY1[i]);
	}

	// prepare resource
	logger.info("DIR_PATH_ARRAY2 info \n" + Arrays.toString(DIR_PATH_ARRAY2));
	for (int i = 0; i < DIR_PATH_ARRAY2.length; i++) {
	    ScmResource resource = ScmResourceFactory.createDirectoryResource(wsp.getName(), DIR_PATH_ARRAY2[i]);
	    resources.add(resource);
	}

	// prepare privilege
	for (ScmResource resource : resources) {
	    ScmFactory.Role.grantPrivilege(ss, role, resource, ScmPrivilegeType.READ);
	}
	ScmFactory.Role.grantPrivilege(ss, role, resources.get(3), ScmPrivilegeType.CREATE);

	ScmAuthUtils.checkPriority(site, NAME, NAME, role, wsp.getName());

	// create new session by new user
	newSS = TestScmTools.createSession(site, NAME, PASSWORD);
	newWS = ScmFactory.Workspace.getWorkspace(wsp.getName(), newSS);
    }

    @Test
    private void test() throws ScmException {
	// match middle[DIR_PATH_ARRAY2[3], CREATE + READ]
	try {
	    String dirPath = DIR_PATH_ARRAY2[3];
	    ScmId fileId = this.createScmFile(newWS, dirPath);
	    ScmFile file = ScmFactory.File.getInstance(newWS, fileId);
	    try {
		file.setTitle(NAME);
		Assert.fail("expect fail but succ.");
	    } catch (ScmException e) {
		logger.info("not update privilege in test, errorMsg = [" + e.getError() + "]");
	    }
	    file = ScmFactory.File.getInstance(ws, fileId);
	    Assert.assertEquals(file.getTitle(), "");
	} catch (ScmException e) {
	    e.printStackTrace();
	    throw e;
	}

	runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException, InterruptedException {
	try {
	    if (runSuccess || TestScmBase.forceClear) {
		for (int i = 0; i < DIR_PATH_ARRAY2.length; i++) {
		    ScmFactory.Role.revokePrivilege(ss, role, resources.get(i), ScmPrivilegeType.READ);
		}
		ScmFactory.Role.revokePrivilege(ss, role, resources.get(3), ScmPrivilegeType.CREATE);

		ScmFactory.User.deleteUser(ss, NAME);
		ScmFactory.Role.deleteRole(ss, NAME);

		for (ScmId fileId : fileIds) {
		    ScmFactory.File.deleteInstance(ws, fileId, true);
		}

		for (int i = DIR_PATH_ARRAY1.length - 1; i >= 0; i--) {
		    ScmFactory.Directory.deleteInstance(ws, DIR_PATH_ARRAY1[i]);
		}

		TestTools.LocalFile.removeFile(localPath);
	    }
	} finally {
	    if (null != ss) {
		ss.close();
	    }
	    if (null != newSS) {
		newSS.close();
	    }
	}
    }

    private void createUserAndRole() throws ScmException {
	ScmUser scmUser = ScmFactory.User.createUser(ss, NAME, ScmUserPasswordType.LOCAL, PASSWORD);
	role = ScmFactory.Role.createRole(ss, NAME, "");
	ScmUserModifier modifier = new ScmUserModifier();
	modifier.addRole(role);
	ScmFactory.User.alterUser(ss, scmUser, modifier);
    }

    private ScmId createScmFile(ScmWorkspace scmWS, String dirPath) throws ScmException {
	ScmDirectory scmDir = ScmFactory.Directory.getInstance(scmWS, dirPath);

	ScmFile file = ScmFactory.File.createInstance(scmWS);
	file.setFileName(NAME + UUID.randomUUID());
	file.setAuthor(NAME);
	file.setDirectory(scmDir);
	ScmId fileId = file.save();
	fileIds.add(fileId);

	return fileId;
    }

}
