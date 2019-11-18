package com.sequoiacm.testcommon;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.common.ScmType.ServerScope;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.dsutils.CephS3Utils;
import com.sequoiacm.testcommon.dsutils.CephSwiftUtils;
import com.sequoiacm.testcommon.dsutils.HdfsUtils;
import com.sequoiadb.base.*;
import com.sequoiadb.exception.BaseException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;
import org.bson.util.JSON;
import org.testng.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class TestSdbTools {
	private static final Logger logger = Logger.getLogger(TestSdbTools.class);

	// current time
	private static SimpleDateFormat yearFm = new SimpleDateFormat("yyyy");
	private static SimpleDateFormat monthFm = new SimpleDateFormat("MM");

	// SCMSYSTEM
	public static final String SCM_CS = "SCMSYSTEM";
	public static final String SCM_CL_SITE = "SITE";
	public static final String SCM_CL_USER = "USER";
	public static final String SCM_CL_SESSION = "SESSIONS";
	public static final String SCM_CL_TASK = "TASK";
	public static final String SCM_CL_CONTENTSEVER = "CONTENTSERVER";
	public static final String SCM_CL_WORKSPACE = "WORKSPACE";

	// META
	public static final String WK_CS_FILE = "_META";
	public static final String WK_CL_FILE = "FILE";
	public static final String WK_CL_FILE_HISTORY = "FILE_HISTORY";
	public static final String WK_CL_TRANSACTION_LOG = "TRANSACTION_LOG";
	public static final String WK_CL_CLASS_DEFINE = "CLASS_DEFINE";
	public static final String WK_CL_CLASS_ATTR_DEFINE = "CLASS_ATTR_DEFINE";
	public static final String WK_CL_CLASS_ATTR_REL = "CLASS_ATTR_REL";

	public static Sequoiadb getSdb(String sdbUrl) {
		Sequoiadb sdb = new Sequoiadb(sdbUrl, TestScmBase.sdbUserName, TestScmBase.sdbPassword);
		sdb.setSessionAttr(new BasicBSONObject("PreferedInstance", "M"));
		return sdb;
	}
	
	public static List<String> getDomainNames(String sdbUrl) {
		Sequoiadb sdb = null;
		DBCursor cursor = null;
		List<String> domainNames = new ArrayList<>();
		try {
			sdb = getSdb(sdbUrl);
			cursor = sdb.listDomains(null, null, null, null);
			while (cursor.hasNext()) {
				String name = (String) cursor.getNext().get("Name");
				domainNames.add(name);
			}
		} finally {
			if (null != cursor) {
				cursor.close();
			}
			if (null != sdb) {
				sdb.close();
			}
		}
		return domainNames;
	}

	public static long count(String urls,String user,String password,String csName,String clName,Object match)
			throws Exception {
		Sequoiadb db = null;
		long num = 0;
		try{
			db = new Sequoiadb(urls,user,password, new ConfigOptions());
			DBCollection cl = db.getCollectionSpace(csName).getCollection(clName);
			if (match instanceof BasicBSONObject) {
				num = cl.getCount((BasicBSONObject)match);
			} else if (match instanceof String) {
				num = cl.getCount((String)match);
			} else {
				throw new Exception("invalid type!!!");
			}
		}finally {
			if(db != null){
				db.close();
			}
		}
		return num;
	}

	public static void insert(String urls, String user,String password,String csName, String clName, Object records)
			throws Exception {
		Sequoiadb db = null;
		try {
			db = new Sequoiadb(urls, user, password, new ConfigOptions());
			DBCollection cl = db.getCollectionSpace(csName).getCollection(clName);
			if (records instanceof BasicBSONObject) {
				cl.insert((BasicBSONObject) records);
			} else if (records instanceof ArrayList) {
				cl.insert((ArrayList<BSONObject>) records);
			} else if (records instanceof String) {
				cl.insert((String) records);
			} else {
				throw new Exception("invalid type!!!");
			}
		} finally {
			if (db != null) {
				db.close();
			}
		}
	}

	public static void update(String urls, String user,String password, String csName, String clName, BSONObject matcher,
							   BSONObject modify) throws Exception {
		Sequoiadb db = null;
		try {
			db = new Sequoiadb(urls, user, password, new ConfigOptions());
			DBCollection cl = db.getCollectionSpace(csName).getCollection(clName);
			cl.update(matcher, modify, new BasicBSONObject());
		} finally {
			if (db != null) {
				db.close();
			}
		}
	}

	public static void delete(String urls, String user,String password, String csName, String clName, BSONObject matcher)
			throws Exception {
		Sequoiadb db = null;
		try {
			db = new Sequoiadb(urls,user, password, new ConfigOptions());
			DBCollection cl = db.getCollectionSpace(csName).getCollection(clName);
			cl.delete(matcher);
		} finally {
			if (db != null) {
				db.close();
			}
		}
	}

	public static List<BSONObject> query(String urls, String user,String password, String csName, String clName,
										 BSONObject matcher) {
		Sequoiadb db = null;
		DBCursor cursor = null;
		List<BSONObject> objects = new ArrayList<>();
		try {
			db = new Sequoiadb(urls, user, password, new ConfigOptions());
			DBCollection cl = db.getCollectionSpace(csName).getCollection(clName);
			cursor = cl.query(matcher, null, null, null);
			while (cursor.hasNext()) {
				objects.add((BasicBSONObject) cursor.getNext());
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
			if (db != null) {
				db.close();
			}
		}
		return objects;
	}

	public static void createCSCL(String urls, String user,String password,String csName, String clName) {
		Sequoiadb db = null;
		try {
			db = new Sequoiadb(urls, user, password, new ConfigOptions());
			if (!db.isCollectionSpaceExist(csName)) {
				db.createCollectionSpace(csName);
			}
			CollectionSpace cs = db.getCollectionSpace(csName);
			if (!cs.isCollectionExist(clName)) {
				cs.createCollection(clName);
			}
		} finally {
			if (db != null) {
				db.close();
			}
		}
	}

	public static Connection getHbaseConnect(SiteWrapper site) throws IOException {
		// get hbase's host and port
		String dsUrl = (String)site.getDataDsConf().get("hbase.zookeeper.quorum");
		String[] obj = dsUrl.split(":"); // e.g: suse113-2:2181
		String host = obj[0];
		String port = obj[1];
		// connect hbase
		Configuration config = HBaseConfiguration.create();
		config.set("hbase.zookeeper.quorum", host);
		config.set("hbase.zookeeper.property.clientPort", port);
		Connection connection = ConnectionFactory.createConnection(config);
		return connection;
	}

	/**
	 * get scmFile meta csName by connect SDB
	 */
	public static String getFileMetaCsName(WsWrapper ws) {
		return getFileMetaCsName(ws.getName());
	}

	public static String getFileMetaCsName(String wsName) {
		return wsName + "_META";
	}

	/**
	 * get scmFile data csName by connect SDB
	 * 
	 * @throws ScmException
	 */
	public static String getFileDataCsName(int siteId, String wsName) throws ScmException {
		String prefix = wsName + "_LOB";
		String shardType = null;
		BSONObject dataShardingType = getDataShardingTypeForSdb(siteId, wsName);
		if (null == dataShardingType) {
			shardType = "year";
		} else {
			shardType = (String) dataShardingType.get("collection_space");
			if (null == shardType) {
				shardType = "year"; // default year
			}
		}

		if (!shardType.equals("none")) {
			prefix += "_";
		}

		String postfix = getCsClPostfix(shardType);

		return prefix + postfix;
	}

	public static String getFileDataClName(int siteId, String wsName) throws ScmException {
		String prefix = "LOB_";
		String shardType = null;
		BSONObject dataShardingType = getDataShardingTypeForSdb(siteId, wsName);
		if (null == dataShardingType) {
			shardType = "month";
		} else {
			shardType = (String) dataShardingType.get("collection");
			if (null == shardType) {
				shardType = "month"; // default month
			}
		}
		String postfix = getCsClPostfix(shardType);

		return prefix + postfix;
	}

	public static String getCsClPostfix(String shardType) {
		Date currTime = new Date();
		String currY = yearFm.format(currTime);
		String currM = monthFm.format(currTime);
		String postfix = null;
		if (shardType.equals("none")) {
			postfix = "";
		} else if (shardType.equals("year")) {
			postfix = currY;
		} else if (shardType.equals("quarter")) {
			int quarter = (int) Math.ceil(Double.parseDouble(currM) / 3);
			postfix = currY + "Q" + quarter;
		} else if (shardType.equals("month")) {
			postfix = currY + currM;
		}
		return postfix;
	}
	
	private static BSONObject getDataShardingTypeForSdb(int siteId, String wsName) throws ScmException {
		Object dataShardingType = getWsProperties(siteId, wsName, "data_sharding_type");
		return (BSONObject) dataShardingType;
	}
	
	public static String getDataShardingTypeForOtherDs(int siteId, String wsName) throws ScmException {
		Object dataShardingType = getWsProperties(siteId, wsName, "data_sharding_type");
		return (String) dataShardingType;
	}

	public static Object getContainerPrefix(int siteId, String wsName) throws ScmException {
		return getWsProperties(siteId, wsName, "container_prefix");
	}

	private static Object getWsProperties(int siteId, String wsName, String key) throws ScmException {
		ScmSession session = null;
		Object dataShardingType = null;
		ScmCursor<ScmWorkspaceInfo> cursor = null;
		try {
			session = TestScmTools.createSession(ScmInfo.getRootSite());
			cursor = ScmFactory.Workspace.listWorkspace(session);
			while (cursor.hasNext()) {
				ScmWorkspaceInfo info = cursor.getNext();
				if (info.getName().equals(wsName)) {
					List<BSONObject> dataLocation = info.getDataLocation();
					for (BSONObject obj : dataLocation) {
						int localSiteId = (int) obj.get("site_id");
						if (siteId == localSiteId) {
							dataShardingType = obj.get(key);
							break;
						}
					}
					break;
				}
			}
		} catch (ScmException e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (null != session) {
				session.close();
			}
			if (null != cursor) {
				cursor.close();
			}
		}
		return dataShardingType;
	}

	public static String getDataTableNameInHbase(SiteWrapper site, WsWrapper ws) throws ScmException {
		return getDataTableNameInHbase(site.getSiteId(), ws.getName());
	}

	public static String getDataTableNameInHbase(int siteId, String wsName) throws ScmException {
		String prefix = wsName + "_SCMFILE";

		String dataShardingType = getDataShardingTypeForOtherDs(siteId, wsName);
		if (null == dataShardingType) {
			dataShardingType = "month";
		}
		String postfix = getCsClPostfix(dataShardingType);

		if (!dataShardingType.equals("none")) {
			prefix += "_";
		}

		return prefix + postfix;
	}

	public static class Lob {

		/**
		 * special cases are used, such as analog LOB remain, by connect DB
		 */
		public static void putLob(SiteWrapper site, WsWrapper ws, ScmId fileId, String filePath) throws Exception {
			DatasourceType dsType = site.getDataType();
			if (dsType.equals(DatasourceType.SEQUOIADB)) {
				putDataInSdb(site, ws, fileId, filePath);
			} else if (dsType.equals(DatasourceType.HBASE)) {
				putDataInHbase(site, ws, fileId, filePath);
			} else if (dsType.equals(DatasourceType.CEPH_S3)) {
				CephS3Utils.putObject(site, ws, fileId, filePath);
			} else if (dsType.equals(DatasourceType.CEPH_SWIFT)) {
				CephSwiftUtils.createObject(site, ws, fileId, filePath);
			}else if(dsType.equals(DatasourceType.HDFS)){
				HdfsUtils.upload(site, ws, fileId, filePath);
		    }else {
				throw new Exception(dsType + ",dataSourceType is not invalid");
			}
		}

		private static void putDataInSdb(SiteWrapper site, WsWrapper ws, ScmId fileId, String lobPath)
				throws IOException, ScmException {
			Sequoiadb db = null;
			DBLob lobDB = null;
			InputStream ism = null;
			try {
				String dsUrl = site.getDataDsUrl();
				db = getSdb(dsUrl);

				String csName = getFileDataCsName(site.getSiteId(), ws.getName());
				String clName = getFileDataClName(site.getSiteId(), ws.getName());

				if (!db.isCollectionSpaceExist(csName)) {
					writeTmpFileInScm(site, ws);
				}

				if (db.isCollectionSpaceExist(csName)) {
					DBCollection clDB = db.getCollectionSpace(csName).getCollection(clName);

					lobDB = clDB.createLob(new ObjectId(fileId.get()));
					ism = new FileInputStream(new File(lobPath));
					lobDB.write(ism);
				}
			} finally {
				if (lobDB != null) {
					lobDB.close();
				}
				if (ism != null) {
					ism.close();
				}
				if (db != null) {
					db.close();
				}
			}
		}

		private static void putDataInHbase(SiteWrapper site, WsWrapper ws, ScmId fileId, String lobPath)
				{
			Connection conn = null;
			try {
				conn = getHbaseConnect(site);
				String tableName = getDataTableNameInHbase(site.getSiteId(), ws.getName());
				HBaseAdmin hAdmin = (HBaseAdmin) conn.getAdmin();
				if (!hAdmin.tableExists(tableName)) {
					writeTmpFileInScm(site, ws);
				}

				if (hAdmin.tableExists(tableName)) {
					Table table = conn.getTable(TableName.valueOf(tableName));
					Put put;
					byte[] buffer = TestTools.getBuffer(lobPath);
					int num = (int) Math.ceil(Double.valueOf(buffer.length) / (1024 * 1024));
					for (int i = 0; i < num; i++) {
						int len = 1024 * 1024;
						if (i == num - 1) {
							len = buffer.length - 1024 * 1024 * i;
						}
						byte[] fileblock = new byte[len];
						System.arraycopy(buffer, 1024 * 1024 * i, fileblock, 0, len);

						put = new Put(Bytes.toBytes(fileId.get()));
						put.addColumn(Bytes.toBytes("SCM_FILE_DATA"), Bytes.toBytes("PIECE_NUM_" + i), fileblock);
						table.put(put);
					}

					put = new Put(Bytes.toBytes(fileId.get()));
					put.addColumn(Bytes.toBytes("SCM_FILE_META"), Bytes.toBytes("FILE_SIZE"),
							Bytes.toBytes(String.valueOf(new File(lobPath).length())));
					table.put(put);

					put = new Put(Bytes.toBytes(fileId.get()));
					put.addColumn(Bytes.toBytes("SCM_FILE_META"), Bytes.toBytes("FILE_STATUS"),
							Bytes.toBytes("Available"));
					table.put(put);

					// put is success?
					Get get = new Get(Bytes.toBytes(fileId.get()));
					Result res = table.get(get);
					if (res.isEmpty()) {
						throw new Exception("error, insert data of hbase failed");
					}
					table.close();
				}
			}catch(Exception e)
			{
				Assert.fail(e.getMessage());
			}finally {
				if (null != conn) {
					try {
						conn.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		private static void writeTmpFileInScm(SiteWrapper site, WsWrapper ws) throws ScmException {
			ScmSession session = null;
			try {
				session = TestScmTools.createSession(site);
				ScmWorkspace scmWs = ScmFactory.Workspace.getWorkspace(ws.getName(), session);
				ScmFile file = ScmFactory.File.createInstance(scmWs);
				file.setAuthor("TestSdbTools.putDataInSdb");
				file.setFileName("TestSdbTools.putDataInSdb"+UUID.randomUUID());
				ScmId tmpFileId = file.save();

				ScmFactory.File.deleteInstance(scmWs, tmpFileId, true);
			} finally {
				if (null != session) {
					session.close();
				}
			}
		}

		/**
		 * remove LOB connect DB
		 */
		public static void removeLob(SiteWrapper site, WsWrapper ws, ScmId fileId) throws Exception {
			DatasourceType dsType = site.getDataType();
			if (dsType.equals(DatasourceType.SEQUOIADB)) {
				deleteDataInSdb(site, ws, fileId);
			} else if (dsType.equals(DatasourceType.HBASE)) {
			    deleteDataInHbase(site, ws, fileId);
			} else if (dsType.equals(DatasourceType.CEPH_S3)) {
				CephS3Utils.deleteObject(site, ws, fileId);
			} else if (dsType.equals(DatasourceType.CEPH_SWIFT)) {
				CephSwiftUtils.deleteObject(site, ws, fileId);
			} else if(dsType.equals(DatasourceType.HDFS)){
				HdfsUtils.delete(site, ws, fileId);
			}else {
				throw new Exception(dsType + ",dataSourceType is not invalid");
			}
		}

		private static void deleteDataInSdb(SiteWrapper site, WsWrapper ws, ScmId fileId) throws ScmException {
			Sequoiadb db = null;
			try {
				String dsUrl = site.getDataDsUrl();
				db = getSdb(dsUrl);

				String csName = getFileDataCsName(site.getSiteId(), ws.getName());
				String clName = getFileDataClName(site.getSiteId(), ws.getName());
				DBCollection clDB = db.getCollectionSpace(csName).getCollection(clName);

				ObjectId lobObjId = new ObjectId(fileId.get());
				clDB.removeLob(lobObjId);
			} finally {
				if (null != db) {
					db.close();
				}
			}
		}

		private static void deleteDataInHbase(SiteWrapper site, WsWrapper ws, ScmId fileId) throws Exception {
			Connection conn = null;
			try {
				conn = getHbaseConnect(site);
				String tableName = getDataTableNameInHbase(site.getSiteId(), ws.getName());
				Table table = conn.getTable(TableName.valueOf(tableName));

				Delete del = new Delete(Bytes.toBytes(fileId.get()));
				table.delete(del);

				// delete is success?
				Get get = new Get(Bytes.toBytes(fileId.get()));
				Result res = table.get(get);
				if (!res.isEmpty()) {
					throw new Exception("error, delete data of hbase failed");
				}
				table.close();
			} finally {
				if (null != conn) {
					conn.close();
				}
			}
		}
	}

	/**
	 * mainly for reloadbizconf
	 */
	public static class Workspace {
		// TODO createws使用scmadmin.sh工具添加
		// FIXME: createWorkspace()是临时测试的方法，待scm实现了该接口，要替换
		// 写死了大量信息，但我能怎么办呢？我也很绝望啊
		// update to com.sequoiacm.testcommon.scmutils.ScmWsUtils.java
		
		@Deprecated
		public static ScmWorkspace create(String wsName, ScmSession ss) throws ScmException {
			Sequoiadb sdb = null;
			try {
				sdb = new Sequoiadb(TestScmBase.mainSdbUrl, TestScmBase.sdbUserName, TestScmBase.sdbPassword);
				DBCollection wsCL = sdb.getCollectionSpace(TestSdbTools.SCM_CS)
						.getCollection(TestSdbTools.SCM_CL_WORKSPACE);

				int wsId = getUniqueWsId(wsCL);
				BSONObject newWsMeta = cloneWsMeta(wsCL.queryOne(), wsName, wsId);
				wsCL.insert(newWsMeta);
				createMetaCSCL(sdb, wsName);
			} finally {
				if (sdb != null) {
					sdb.close();
				}
			}

			List<BSONObject> infoList = ScmSystem.Configuration.reloadBizConf(ServerScope.ALL_SITE,
					ScmInfo.getRootSite().getSiteId(), ss);
			logger.info("infoList after reloadbizconf: \n" + infoList);
			return ScmFactory.Workspace.getWorkspace(wsName, ss);
		}

		/**
		 * delete workspace by connect SDB
		 */
		public static void delete(String wsName, ScmSession session) throws Exception {
			try {
				ScmFactory.Workspace.deleteWorkspace(session, wsName, true);
			} catch (ScmException e) {
				if(e.getError() != ScmError.WORKSPACE_NOT_EXIST){
					throw e;
				}
			}
			for (int i = 0; i < 10; i++) {
				Thread.sleep(1000);
				try{
					ScmFactory.Workspace.getWorkspace(wsName, session);
				}catch(ScmException e){
					if(e.getError() != ScmError.WORKSPACE_NOT_EXIST){
						throw e;
					}
					break;
				}
			}
		}

		public static void checkWsCs(String wsName, ScmSession session) throws ScmException {
			Sequoiadb rSdb = null;
			try {
				rSdb = new Sequoiadb(TestScmBase.mainSdbUrl, TestScmBase.sdbUserName, TestScmBase.sdbPassword);
				// check workspace's cs
				String metaCSName = wsName+"_META";
				rSdb.getCollectionSpace(metaCSName);
				Assert.fail("ws "+wsName+" already deleted, meta cs should not exist");
			} catch (BaseException e) {
				if(e.getErrorCode()!=-34){
					throw e;
				}
			} finally {
				if (rSdb != null) {
					rSdb.close();
				}
			}
		}
		

		
		@Deprecated
		private static int getUniqueWsId(DBCollection wsCL) {
			DBCursor cursor = wsCL.query(null, "{ id: {$include: 1} }", null, null);
			int idMax = 0;
			while (cursor.hasNext()) {
				int currId = (int) cursor.getNext().get("id");
				if (currId > idMax) {
					idMax = currId;
				}
			}
			int uniqueId = idMax + 1;
			return uniqueId;
		}

		@Deprecated
		private static BSONObject cloneWsMeta(BSONObject oldWsMeta, String wsName, int wsId) {
			BSONObject newWsMeta = oldWsMeta;
			newWsMeta.put("_id", new ObjectId());
			newWsMeta.put("name", wsName);
			newWsMeta.put("id", wsId);
			return newWsMeta;
		}

		@Deprecated
		private static void createMetaCSCL(Sequoiadb sdb, String wsName) {
			CollectionSpace cs = sdb.createCollectionSpace(wsName + "_META", new BasicBSONObject("Domain", "domain1"));
			cs.createCollection("FILE", (BSONObject) JSON
					.parse("{ 'ShardingKey': { 'create_month': 1 }, ShardingType: 'range', IsMainCL: true }"));
			cs.createCollection("FILE_HISTORY", (BSONObject) JSON
					.parse("{ 'ShardingKey': { 'create_month': 1 }, ShardingType: 'range', IsMainCL: true }"));
			cs.createCollection("TRANSACTION_LOG");
		}
	}

	public static class Task {

		/**
		 * delete task related records
		 */
		public static void deleteMeta(ScmId taskId) {
			Sequoiadb sdb = null;
			try {
				sdb = getSdb(TestScmBase.mainSdbUrl);
				DBCollection cl = sdb.getCollectionSpace(SCM_CS).getCollection(SCM_CL_TASK);
				if (null != taskId) {
					BSONObject obj = new BasicBSONObject();
					obj.put("id", taskId.get());
					cl.delete(obj);
				}
			} finally {
				if (null != sdb) {
					sdb.close();
				}
			}
		}

		public static void printlnTaskInfos(){
			List<BSONObject> bsonObjects = TestSdbTools.query(TestScmBase.mainSdbUrl,TestScmBase.sdbUserName,
					TestScmBase.sdbPassword,SCM_CS,SCM_CL_TASK,new BasicBSONObject());
			for(BSONObject bsonObject : bsonObjects){
				System.out.println("taskInfo : " + bsonObject.toString());
			}
		}
	}
}
