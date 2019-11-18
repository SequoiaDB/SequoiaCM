package com.sequoiacm.testcommon.scmutils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sequoiacm.client.core.*;
import org.apache.log4j.Logger;
import org.bson.BSONObject;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

public class ScmFileUtils extends TestScmBase {
	private static final Logger logger = Logger.getLogger(ScmFileUtils.class);

	public static ScmId create(ScmWorkspace ws, String fileName, String filePath) throws ScmException {
		ScmId fileId = null;
		try {
			ScmFile file = ScmFactory.File.createInstance(ws);
			file.setFileName(fileName);
			file.setAuthor(fileName);
			file.setContent(filePath);
			fileId = file.save();
		} catch (ScmException e) {
			logger.error("[test] create scmfile, fileName=" + fileName);
			e.printStackTrace();
			throw e;
		}
		return fileId;
	}

	public static void cleanFile(WsWrapper ws, BSONObject condition) throws ScmException {
		ScmSession session = null;
		SiteWrapper site = ScmInfo.getSite();
		ScmCursor<ScmFileBasicInfo> cursor = null;
		ScmId fileId = null;
		try {
			session = TestScmTools.createSession(site);
			ScmWorkspace work = ScmFactory.Workspace.getWorkspace(ws.getName(), session);

			cursor = ScmFactory.File.listInstance(work, ScopeType.SCOPE_CURRENT, condition);
			while (cursor.hasNext()) {
				ScmFileBasicInfo fileInfo = cursor.getNext();
				fileId = fileInfo.getFileId();
				ScmFactory.File.deleteInstance(work, fileId, true);
			}
		} catch (ScmException e) {
			logger.error("[test] clean scmfile, siteName = " + site.getSiteName() + ", fileId=" + fileId);
			e.printStackTrace();
			throw e;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
			if (session != null) {
				session.close();
			}
		}
	}

	/**
	 * one file, default workspace, check file's meta and data
	 */
	public static void checkMetaAndData(WsWrapper ws, ScmId fileId, SiteWrapper[] expSites, java.io.File localPath,
			String filePath) throws Exception {
		List<ScmId> fileIdList = new ArrayList<>();
		fileIdList.add(fileId);
		checkMetaAndData(ws, fileIdList, expSites, localPath, filePath);
	}

	/**
	 * multiple file, specify workspace, check file's meta and data
	 * 
	 * @param wsName
	 * @param fileIdList
	 * @param expSites
	 * @param localPath
	 * @param filePath
	 * @throws Exception
	 */
	public static void checkMetaAndData(WsWrapper ws, List<ScmId> fileIdList, SiteWrapper[] expSites,
			java.io.File localPath, String filePath) throws Exception {
		boolean medaChecked = false;
		for (SiteWrapper site : expSites) {
			ScmSession session = null;
			ScmWorkspace work = null;
			ScmId fileId = null;
			try {
				session = TestScmTools.createSession(site);
				work = ScmFactory.Workspace.getWorkspace(ws.getName(), session);

				for (int i = 0; i < fileIdList.size(); i++) {
					fileId = fileIdList.get(i);
					if (!medaChecked) {
						checkMeta(work, fileId, expSites);
					}
					checkData(work, fileId, localPath, filePath);
				}

				medaChecked = true;
			} finally {
				if (session != null) {
					session.close();
				}
			}
		}
	}

	/**
	 * check scmfile's meta
	 * 
	 * @param ws
	 * @param fileId
	 * @param expSiteIdArr
	 * @throws Exception
	 */
	public static void checkMeta(ScmWorkspace ws, ScmId fileId, SiteWrapper[] expSites) throws Exception {
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);

		// sort the actual siteId
		int actSiteNum = file.getLocationList().size();
		List<Integer> actIdList = new ArrayList<>();
		for (int i = 0; i < actSiteNum; i++) {
			int siteId = file.getLocationList().get(i).getSiteId();
			actIdList.add(siteId);
		}
		Collections.sort(actIdList);

		// sort the expect siteId
		List<Integer> expIdList = new ArrayList<>();
		for (int i = 0; i < expSites.length; i++) {
			expIdList.add(expSites[i].getSiteId());
		}
		Collections.sort(expIdList);

		// check site number
		int expSiteNum = expSites.length;
		if (actSiteNum != expSiteNum) {
			throw new Exception("Failed to check siteNum, ws = " + ws.getName() + ", fileId = " + fileId.get()
					+ ", expSiteNum = " + expSiteNum + ", actSiteNum = " + actSiteNum + ", expSiteIds = "
					+ expIdList + ", actSiteIds = " + actIdList);
		}

		// check site id
		for (int i = 0; i < actSiteNum; i++) {
			int expSiteId = expIdList.get(i);
			int actSiteId = actIdList.get(i).intValue();
			if (actSiteId != expSiteId) {
				throw new Exception("Failed to check siteId, ws = " + ws.getName() + ", fileId = " + fileId.get()
						+ ", expSiteId = " + expSiteId + ", actSiteId = " + actSiteId + ", expSiteIds = "
						+ expIdList + ", actSiteIds = " + actIdList);
			}
		}
	}

	/**
	 * check scmfile's data
	 */
	public static void checkData(ScmWorkspace ws, ScmId fileId, java.io.File localPath, String filePath)
			throws Exception {
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
				Thread.currentThread().getId());
		file.getContentFromLocalSite(downloadPath);
		String expMd5 = TestTools.getMD5(filePath);
		String actMd5 = TestTools.getMD5(downloadPath);
		if (!expMd5.equals(actMd5)) {
			throw new Exception("Failed to check data, " + "expMd5=" + expMd5 + ", actMd5=" + actMd5);
		}
		TestTools.LocalFile.removeFile(downloadPath);
	}
}
