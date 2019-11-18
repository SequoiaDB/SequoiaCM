package com.sequoiacm.net.readcachefile.concurrent;

import com.sequoiacm.client.common.ScmType.InputStreamType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine.SeekType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

/**
 * @FileName SCM-726 : 多种方式并发读取相同文件
 * @Author linsuqiang
 * @Date 2017-08-08
 * @Version 1.00
 */

/*
 * 1、在分中心A写入多个文件； 2、在分中心B并发读取相同文件，如下几种读取方式并发读取文件： 1）getContent(OutputSteam
 * arg0); 2）getContent(String arg0); 3）ScmInputStream.seek(int seekType, long
 * size); 4）ScmInputStream.read(OutputStream out); 5）ScmInputStream.read(byte[]
 * b, int off, int len); 3、检查每个读取操作读取到的文件内容正确性；
 */

public class ReadSameFileByAllWay726 extends TestScmBase {
	private boolean runSuccess = false;
	private SiteWrapper rootSite = null;
	private List<SiteWrapper> branSites = null;
	private final int branSitesNum = 2;
	private WsWrapper wsp = null;

	private File localPath = null;
	private String filePath = null;
	private ScmId fileId = null;
	private final int fileSize = 5 * 1024 * 1024;
	private final String author = "file726";

	// for offset read
	private int seekSize = 1;
	private int off = 1024 * 1024 - 1;
	private int len = 1024 * 1024;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
			filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";

			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			TestTools.LocalFile.createFile(filePath, fileSize);

			rootSite = ScmInfo.getRootSite();
			branSites = ScmInfo.getBranchSites(branSitesNum);
			wsp = ScmInfo.getWs();

			writeFileOnA();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "fourSite" })
	private void test() throws Exception {
		try {
			GetContentThd1 gThd1 = new GetContentThd1();
			GetContentThd2 gThd2 = new GetContentThd2();
			ScmInputStreamThd1 sThd1 = new ScmInputStreamThd1();
			ScmInputStreamThd3 sThd3 = new ScmInputStreamThd3();

			gThd1.start();
			gThd2.start();
			sThd1.start();
			sThd3.start();

			Assert.assertTrue(gThd1.isSuccess(), gThd1.getErrorMsg());
			Assert.assertTrue(gThd2.isSuccess(), gThd2.getErrorMsg());
			Assert.assertTrue(sThd1.isSuccess(), sThd1.getErrorMsg());
			Assert.assertTrue(sThd3.isSuccess(), sThd3.getErrorMsg());

			SiteWrapper[] expSites = { branSites.get(0), branSites.get(1) };
			ScmFileUtils.checkMetaAndData(wsp, fileId, expSites, localPath, filePath);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		ScmSession ss = null;
		try {
			ss = TestScmTools.createSession(rootSite);
			ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), ss);
			if (runSuccess || TestScmBase.forceClear) {
				ScmFactory.File.deleteInstance(ws, fileId, true);
				TestTools.LocalFile.removeFile(localPath);
			}
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
           if(ss != null){
        	   ss.close();
           }
		}
	}

	// getContent(OutputStream arg);
	private class GetContentThd1 extends TestThreadBase {

		@Override
		public void exec() throws Exception {
			ScmSession ss = null;
			OutputStream fos = null;
			try {
				// login
				ss = TestScmTools.createSession(branSites.get(1));
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), ss);

				// read content
				ScmFile scmfile = ScmFactory.File.getInstance(ws, fileId);
				String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
						Thread.currentThread().getId());
				fos = new FileOutputStream(new File(downloadPath));
				scmfile.getContent(fos);

				// check content
				Assert.assertEquals(TestTools.getMD5(filePath), TestTools.getMD5(downloadPath));
			} finally {
				if (fos != null)
					fos.close();
				if (ss != null)
					ss.close();
			}
		}

	}

	// getContent(String arg);
	private class GetContentThd2 extends TestThreadBase {

		@Override
		public void exec() throws Exception {
			ScmSession ss = null;
			try {
				// login
				ss = TestScmTools.createSession(branSites.get(1));
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), ss);

				// read content
				ScmFile scmfile = ScmFactory.File.getInstance(ws, fileId);
				String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
						Thread.currentThread().getId());
				scmfile.getContent(downloadPath);

				// check content
				Assert.assertEquals(TestTools.getMD5(filePath), TestTools.getMD5(downloadPath));
			} finally {
				if (ss != null)
					ss.close();
			}
		}

	}

	// ScmInputStream.read(byte[] b, int off, int len);
	private class ScmInputStreamThd1 extends TestThreadBase {

		@Override
		public void exec() throws Exception {
			ScmSession ss = null;
			OutputStream fos = null;
			ScmInputStream sis = null;
			try {
				// login
				ss = TestScmTools.createSession(branSites.get(1));
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), ss);

				// read content
				ScmFile scmfile = ScmFactory.File.getInstance(ws, fileId);
				String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
						Thread.currentThread().getId());

				// FIXME: seek is forbidden. testcase has to be designed again.
			    sis = ScmFactory.File.createInputStream(InputStreamType.SEEKABLE, scmfile);
				sis.seek(SeekType.SCM_FILE_SEEK_SET, seekSize);
				fos = new FileOutputStream(new File(downloadPath));
				byte[] buffer = new byte[off + len];
				int curOff = 0;
				int curExpReadLen = 0;
				int curActReadLen = 0;
				int readSize = 0;
				while (readSize < len) {
					curOff = off + readSize;
					curExpReadLen = len - readSize;
					curActReadLen = sis.read(buffer, curOff, curExpReadLen);
					if (curActReadLen <= 0) {
						break;
					}
					fos.write(buffer, off + readSize, curActReadLen);

					readSize += curActReadLen;
//					System.out.println("---curOff=" + curOff + ", curExpReadLen=" + curExpReadLen + ", curActReadLen="
//							+ curActReadLen + ", readSize=" + readSize);
				}
				fos.flush();

				// check content
				String tmpPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
						Thread.currentThread().getId());
				TestTools.LocalFile.readFile(filePath, seekSize, len, tmpPath);

				Assert.assertEquals(TestTools.getMD5(downloadPath), TestTools.getMD5(tmpPath));
			} finally {
				if (fos != null)
					fos.close();
				if (sis != null)
					sis.close();
				if (ss != null)
					ss.close();
			}
		}

	}

	// ScmInputStream.read(OutputStream out);
	private class ScmInputStreamThd3 extends TestThreadBase {

		@Override
		public void exec() throws Exception {
			ScmSession ss = null;
			OutputStream fos = null;
			ScmInputStream sis = null;
			try {
				// login
				ss = TestScmTools.createSession(branSites.get(1));
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), ss);

				// read content
				ScmFile scmfile = ScmFactory.File.getInstance(ws, fileId);
				String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
						Thread.currentThread().getId());

				sis = ScmFactory.File.createInputStream(scmfile);
				fos = new FileOutputStream(new File(downloadPath));
				sis.read(fos);

				// check content
				Assert.assertEquals(TestTools.getMD5(filePath), TestTools.getMD5(downloadPath));
			} finally {
				if (fos != null)
					fos.close();
				if (sis != null)
					sis.close();
				if (ss != null)
					ss.close();
			}
		}

	}

	private void writeFileOnA() throws ScmException {
		ScmSession ss = null;
		try {
			ss = TestScmTools.createSession(branSites.get(0));
			ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), ss);
			ScmFile file = ScmFactory.File.createInstance(ws);
			file.setAuthor(author);
			file.setFileName(author+"_"+UUID.randomUUID());
			file.setContent(filePath);
			fileId = file.save();
		} finally {
			if (null != ss) {
				ss.close();
			}
		}
	}
}