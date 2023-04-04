package com.sequoiacm.diagnose.datasource;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.dispatcher.CloseableFileDataEntity;
import com.sequoiacm.client.element.ScmSiteInfo;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.element.bizconf.ScmMetaLocation;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.client.util.Strings;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataOpFactory;
import com.sequoiacm.datasource.dataoperation.ScmDataReader;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.diagnose.config.WorkPathConfig;
import com.sequoiacm.diagnose.utils.CommonUtils;
import com.sequoiacm.diagnose.utils.ScmDataSourceUtils;
import com.sequoiacm.infrastructure.crypto.AuthInfo;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.datasource.SequoiadbDatasource;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.util.JSON;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// 数据源管理类，对外提供数据源操作接口
public class ScmDataSourceMgr {
    private static ScmDataSourceMgr INSTANCE = new ScmDataSourceMgr();
    private ReentrantReadWriteLock wsReadWriteLock = new ReentrantReadWriteLock();
    private Map<String, String> sitePasswdMap = new HashMap<>();
    private ScmSiteMgr siteMgr = new ScmSiteMgr();
    private ScmWorkspaceInfo wsInfo;
    private SequoiadbDatasource metaSdbDs;

    private Map<String, SequoiadbDatasource> datasourceMap = new HashMap<>();

    private ScmDataSourceMgr() {
    }

    public static ScmDataSourceMgr getInstance() {
        return INSTANCE;
    }

    // 初始化数据源管理类，需要初始化工作区和站点缓存以及数据源操作工厂
    public void init(String wsName, String url, String user, String passwd)
            throws ScmToolsException {
        ScmWorkspace workspace = getWs(wsName, url, user, passwd);
        loadSite(workspace, url, user, passwd);
        loadWorkspaceInfo(workspace.getName());
    }

    public void loadSite(ScmWorkspace workspace, String url, String user, String passwd)
            throws ScmToolsException {
        loadSiteInfo(url, user, passwd);
        ScmSession session = null;
        try {
            session = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, passwd));
            ScmMetaLocation metaLocation = workspace.getMetaLocation();
            ScmSiteInfo rootSiteInfo = siteMgr.getSite(metaLocation.getSiteName());
            String metaSdbPasswordFile = downloadMetaSecretFile(rootSiteInfo.getName(), url,
                        session.getSessionId());

            AuthInfo authInfo = ScmFilePasswordParser.parserFile(metaSdbPasswordFile);
            this.metaSdbDs = ScmDataSourceUtils.createSdbMetaSource(rootSiteInfo.getMetaUrl(),
                    rootSiteInfo.getMetaUser(), authInfo.getPassword());

            List<ScmDataLocation> dataLocations = workspace.getDataLocations();
            for (ScmDataLocation dataLocation : dataLocations) {
                String siteName = dataLocation.getSiteName();
                ScmSiteInfo info = siteMgr.getSite(siteName);
                if (null == info) {
                    throw new ScmToolsException(
                            "Failed to load data source,because workspace the site not exist,workspace:"
                                    + workspace.getName() + ", siteName:" + siteName,
                            ScmExitCode.SYSTEM_ERROR);
                }
                String datasourcePasswdFile = downloadDataSecretFile(info.getName(), url,
                        session.getSessionId());
                ScmSiteUrl siteUrl = ScmDataSourceUtils.createSiteUrl(info, datasourcePasswdFile);
                ScmSite scmSite = new ScmSite(info.getId(), siteUrl, info.getDataTypeStr());
                siteMgr.addScmSite(info.getId(), scmSite);
                sitePasswdMap.put(info.getName(), datasourcePasswdFile);
            }
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed to load site", ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            ScmCommon.closeResource(session);
        }
    }

    private ScmWorkspace getWs(String ws, String url, String user, String passwd)
            throws ScmToolsException {
        ScmSession session = null;
        try {
            session = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, passwd));
            return ScmFactory.Workspace.getWorkspace(ws, session);
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed to get workspace info", ScmExitCode.SYSTEM_ERROR,
                    e);
        }
        finally {
            ScmCommon.closeResource(session);
        }
    }

    public void loadWorkspaceInfo(String wsName) throws ScmToolsException {
        ReentrantReadWriteLock.WriteLock writeLock = wsReadWriteLock.writeLock();
        Sequoiadb db = null;
        DBCursor dbCursor = null;
        try {
            db = metaSdbDs.getConnection();
            // load workspace
            CollectionSpace sysCs = db.getCollectionSpace("SCMSYSTEM");
            DBCollection wsCl = sysCs.getCollection("WORKSPACE");
            BSONObject wsRecord = wsCl.queryOne(new BasicBSONObject("name", wsName), null, null,
                    null, 0);
            if (wsRecord == null) {
                throw new RuntimeException("workspace not exist:" + wsName);
            }
            ScmWorkspaceInfo scmWorkspaceInfo = new ScmWorkspaceInfo(wsRecord, this.siteMgr);

            // load history workspace
            DBCollection wsHistoryCl = sysCs.getCollection("WORKSPACE_HISTORY");
            dbCursor = wsHistoryCl.query(new BasicBSONObject("name", wsName), null, null, null);
            while (dbCursor.hasNext()) {
                BSONObject wsHistoryRecord = dbCursor.getNext();
                ScmWorkspaceItem wsHistoryItem = new ScmWorkspaceItem(wsHistoryRecord,
                        this.siteMgr);
                scmWorkspaceInfo.addHistoryWsItem(wsHistoryItem);
            }
            try {
                writeLock.lock();
                this.wsInfo = scmWorkspaceInfo;
            }
            finally {
                writeLock.unlock();
            }
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed to load workspace info", ScmExitCode.SYSTEM_ERROR,
                    e);
        }
        finally {
            ScmCommon.closeResource(dbCursor);
            if (null != db) {
                metaSdbDs.releaseConnection(db);
            }
        }
    }

    private void loadSiteInfo(String url, String user, String passwd) throws ScmToolsException {
        ScmSession session = null;
        ScmCursor<ScmSiteInfo> siteCursor = null;
        try {
            session = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, passwd));
            siteCursor = ScmFactory.Site.listSite(session);
            while (siteCursor.hasNext()) {
                ScmSiteInfo site = siteCursor.getNext();
                siteMgr.addScmSiteInfo(site);
            }
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed to list site info", ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            ScmCommon.closeResource(session, siteCursor);
        }
    }

    private String downloadDataSecretFile(String siteName, String url, String sessionId)
            throws ScmToolsException {
        return downloadSecretFile("http://" + replaceUrl(url, siteName)
                + "/api/v1/sites?action=get_datasource_secret", sessionId);
    }

    private String downloadMetaSecretFile(String siteName, String url, String sessionId)
            throws ScmToolsException {
        return downloadSecretFile("http://" + replaceUrl(url, siteName)
                + "/api/v1/sites?action=get_metasource_secret", sessionId);
    }

    private String downloadSecretFile(String requestUri, String sessionId)
            throws ScmToolsException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        try {
            HttpGet request = new HttpGet(requestUri);
            request.setHeader("x-auth-token", sessionId);
            CloseableHttpResponse response = httpClient.execute(request);
            handlerException(response);
            return save(response);
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed to download secret file", ScmExitCode.SYSTEM_ERROR,
                    e);
        }
        finally {
            ScmCommon.closeResource(httpClient);
        }
    }

    private String replaceUrl(String url, String siteName) {
        String[] split = url.split("/");
        return split[0] + "/" + siteName.toLowerCase();
    }

    private String save(CloseableHttpResponse response) throws ScmToolsException {
        InputStream is = null;
        OutputStream os = null;
        try {
            long dataLength = Long.parseLong(response.getHeaders("size")[0].getValue());
            if (dataLength == 0) {
                return null;
            }
            String fileName = response.getHeaders("fileName")[0].getValue();
            is = response.getEntity().getContent();
            CloseableFileDataEntity fileData = new CloseableFileDataEntity(dataLength, is);
            String secretFile = WorkPathConfig.getInstance().getSecretPath() + fileName;
            File file = new File(secretFile);
            if (!file.exists()) {
                file.createNewFile();
            }
            os = Files.newOutputStream(file.toPath());
            long contentLength = CommonUtils.writeContent(os, fileData);
            // 比较文件大小是否一致
            if (dataLength != contentLength) {
                throw new ScmToolsException("Can no get full file data", ScmExitCode.SYSTEM_ERROR);
            }
            return secretFile;
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed to download file", ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            ScmCommon.closeResource(is, os);
        }
    }

    public SequoiadbDatasource getMetaSdbDs() {
        return metaSdbDs;
    }

    public ScmSiteInfo getSiteInfo(int siteId) {
        return siteMgr.getSite(siteId);
    }

    public ScmSiteInfo getSiteInfo(String siteName) {
        return siteMgr.getSite(siteName);
    }

    public SequoiadbDatasource getSdbDatasource(String siteName) throws ScmToolsException {
        SequoiadbDatasource sequoiadbDatasource = datasourceMap.get(siteName);
        if (null == sequoiadbDatasource) {
            ScmSiteInfo siteInfo = getSiteInfo(siteName);
            if (null == siteInfo) {
                throw new ScmToolsException("site not exist,siteName:" + siteName,
                        ScmExitCode.SYSTEM_ERROR);
            }
            String passwdFile = sitePasswdMap.get(siteName);
            AuthInfo authInfo = ScmFilePasswordParser.parserFile(passwdFile);
            sequoiadbDatasource = ScmDataSourceUtils.createSdbMetaSource(siteInfo.getDataUrl(),
                    siteInfo.getDataUser(), authInfo.getPassword());
            datasourceMap.put(siteName, sequoiadbDatasource);
        }
        return sequoiadbDatasource;
    }

    private ScmLocation getScmLocation(int version, int siteId) throws ScmToolsException {
        ReentrantReadWriteLock.ReadLock readLock = wsReadWriteLock.readLock();
        readLock.lock();
        ScmLocation scmLocation;
        try {
            scmLocation = wsInfo.getScmLocation(version, siteId);
        }
        finally {
            readLock.unlock();
        }
        // 重新加载工作区所有版本信息
        if (null == scmLocation) {
            loadWorkspaceInfo(wsInfo.getName());
            scmLocation = wsInfo.getScmLocation(version, siteId);
        }
        return scmLocation;
    }

    public ScmDataReader getReader(int siteId, int wsVersion, String wsName, ScmDataInfo dataInfo)
            throws ScmToolsException, ScmDatasourceException {
        ScmService dataService = siteMgr.getScmDataScmService(siteId);
        if (null == dataService) {
            throw new ScmToolsException(
                    "Can not get data reader,because data service is null,siteId:" + siteId,
                    ScmExitCode.SYSTEM_ERROR);
        }
        ScmDataOpFactory opFactory = siteMgr.getScmDataOpFactory(siteId);
        if (null == opFactory) {
            throw new ScmToolsException(
                    "Can not get data reader,because data op factory is null,siteId:" + siteId,
                    ScmExitCode.SYSTEM_ERROR);
        }
        ScmLocation location = getScmLocation(wsVersion, siteId);
        if (null == location) {
            throw new ScmToolsException("Can not get data location,because location is null,siteId:"
                    + siteId + ", wsVersion:" + wsVersion, ScmExitCode.SYSTEM_ERROR);
        }
        return opFactory.createReader(siteId, wsName, location, dataService, dataInfo);
    }

    private void handlerException(CloseableHttpResponse response) throws ScmToolsException {
        int httpStatusCode = response.getStatusLine().getStatusCode();

        // 2xx Success
        if (httpStatusCode >= 200 && httpStatusCode < 300) {
            return;
        }
        int errcode = httpStatusCode;
        String message = null;

        String resp = getErrorResponse(response);
        if (Strings.hasText(resp)) {
            BSONObject error = (BSONObject) JSON.parse(resp);
            if (error.containsField("status")) {
                errcode = BsonUtils.getNumber(error, "status").intValue();
            }
            if (error.containsField("message")) {
                message = BsonUtils.getString(error, "message");
            }
        }

        throw new ScmToolsException("error code=" + errcode + ", message: " + message,
                ScmExitCode.SYSTEM_ERROR);
    }

    private String getErrorResponse(CloseableHttpResponse response) throws ScmToolsException {
        String error = null;

        HttpEntity entity = response.getEntity();
        if (null != entity) {
            try {
                error = EntityUtils.toString(entity);
            }
            catch (IOException e) {
                throw new ScmToolsException("an error occurs when read http body",
                        ScmExitCode.SYSTEM_ERROR, e);
            }
        }
        else {
            Header errorHeader = response.getFirstHeader("X-SCM-ERROR");
            if (errorHeader != null) {
                error = errorHeader.getValue();
                Header charsetHeader = response.getFirstHeader("X-SCM-ERROR-CHARSET");
                if (charsetHeader != null) {
                    String charset = charsetHeader.getValue();
                    try {
                        error = URLDecoder.decode(error, charset);
                    }
                    catch (UnsupportedEncodingException e) {
                        throw new ScmToolsException(charset, ScmExitCode.SYSTEM_ERROR, e);
                    }
                }
            }
        }
        return error;
    }

    public void releaseResource() {
        if (null != metaSdbDs) {
            metaSdbDs.close();
        }
        Collection<SequoiadbDatasource> datasources = datasourceMap.values();
        for (SequoiadbDatasource datasource : datasources) {
            if (null != datasource) {
                datasource.close();
            }
        }
    }
}
