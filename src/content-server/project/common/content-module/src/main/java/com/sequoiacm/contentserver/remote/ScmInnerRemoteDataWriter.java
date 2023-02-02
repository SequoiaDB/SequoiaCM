package com.sequoiacm.contentserver.remote;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.bson.BSONObject;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.common.ScmRestClientUtils;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.config.PropertiesUtils;
import com.sequoiacm.contentserver.controller.RestExceptionHandler;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.common.ScmDataWriterContext;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.common.annotation.SlowLogExtra;
import com.sequoiacm.infrastructure.dispatcher.ScmURLConfig;

public class ScmInnerRemoteDataWriter {
    private static final Logger logger = LoggerFactory.getLogger(ScmInnerRemoteDataWriter.class);
    private final String dataId;
    private final int dataType;
    private final Date dataCreateTime;

    private String remoteSiteName;
    private HttpURLConnection conn;
    private OutputStream os;
    private String remoteTableName;
    private Integer remoteWorkspaceVersion;

    @SlowLog(operation = "openWriter", extras = {
            @SlowLogExtra(name = "writeFileId", data = "dataId"),
            @SlowLogExtra(name = "writeRemoteSiteName", data = "remoteSiteName") })
    public ScmInnerRemoteDataWriter(int remoteSiteId, final ScmWorkspaceInfo wsInfo,
            String dataId, int dataType, Date dataCreateTime)
            throws ScmServerException {
        this.dataId = dataId;
        this.dataType = dataType;
        this.dataCreateTime = dataCreateTime;
        ScmContentModule contentModule = ScmContentModule.getInstance();
        remoteSiteName = contentModule.getSiteInfo(remoteSiteId).getName();
        ServiceInstance instance = LoadBalancedUtil.chooseInstance(remoteSiteName);
        final String hostPort = instance.getHost() + ":" + instance.getPort();
        try {
            conn = ScmRestClientUtils.getScmRestClient().getHttpURLConnection(
                    getConfig(hostPort, wsInfo.getName(), dataId, dataType, dataCreateTime),
                    instance);
            os = conn.getOutputStream();
        }
        catch (IOException e) {
            closeOs(os);
            closeConn(conn);
            throw new ScmServerException(ScmError.NETWORK_IO, "write data to remote failed:remote="
                    + remoteSiteName + ",dataInfo=" + dataInfoDesc(), e);
        }
        catch (ScmServerException e) {
            logger.error("write data to remote failed:remote=" + remoteSiteName + ",dataInfo="
                    + dataInfoDesc());
            closeOs(os);
            closeConn(conn);
            throw e;
        }
        catch (Exception e) {
            closeOs(os);
            closeConn(conn);
            throw new ScmSystemException("write data to remote failed:remote=" + remoteSiteName
                    + ",dataInfo=" + dataInfoDesc(), e);
        }
    }

    @SlowLog(operation = "writeData")
    public void write(byte[] content, int offset, int len) throws ScmServerException {
        try {
            os.write(content, offset, len);
        }
        catch (IOException e) {
            ScmServerException remoteException = getExceptonFromRemote();
            if (remoteException != null) {
                logger.error("write data to remote failed:remote={},dataInfo={}", remoteSiteName,
                        dataInfoDesc(), e);
                throw remoteException;
            }
            throw new ScmServerException(ScmError.NETWORK_IO, "write data to remote failed:remote="
                    + remoteSiteName + ",dataInfo=" + dataInfoDesc(), e);
        }
        catch (Exception e) {
            throw new ScmSystemException("write data to remote failed:remote=" + remoteSiteName
                    + ",dataInfo=" + dataInfoDesc(), e);
        }
    }

    // remove the writing file & release writer's resources
    @SlowLog(operation = "cancelWriter")
    public void cancel() {
        closeConn(conn);
        closeOs(os);
    }

    public String getRemoteTableName() {
        return remoteTableName;
    }

    public Integer getRemoteWorkspaceVersion() {
        return remoteWorkspaceVersion;
    }

    private String dataInfoDesc() {
        return "{dataId=" + dataId + ", dataType=" + dataType + ", dataCreateTime=" + dataCreateTime
                + "}";
    }

    // close and commit this file & release writer's resources
    @SlowLog(operation = "closeWriter")
    public void close() throws ScmServerException {
        try {
            os.flush();

            int respStatus = conn.getResponseCode();
            if (respStatus != 200) {
                ScmServerException remoteException = getExceptonFromRemote();
                if (remoteException != null) {
                    throw remoteException;
                }
                throw new ScmSystemException("write data to remote failed:remote=" + remoteSiteName
                        + ",dataInfo=" + dataInfoDesc() + ",repstatus=" + respStatus);
            }
            else {
                remoteTableName = conn
                        .getHeaderField(FieldName.FIELD_CLFILE_FILE_SITE_LIST_TABLE_NAME);
                String remoteWorkspaceVersionStr = conn
                        .getHeaderField(FieldName.FIELD_CLFILE_FILE_SITE_LIST_WS_VERSION);
                if (remoteWorkspaceVersionStr != null && !remoteWorkspaceVersionStr.isEmpty()) {
                    remoteWorkspaceVersion = Integer.valueOf(remoteWorkspaceVersionStr);
                }
            }
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (IOException e) {
            ScmServerException remoteException = getExceptonFromRemote();
            if (remoteException != null) {
                logger.error("write data to remote failed:remote={},dataInfo={}", remoteSiteName,
                        dataInfoDesc(), e);
                throw remoteException;
            }
            throw new ScmServerException(ScmError.NETWORK_IO, "write data to remote failed:remote="
                    + remoteSiteName + ",dataInfo=" + dataInfoDesc(), e);
        }
        catch (Exception e) {
            throw new ScmSystemException("write data to remote failed:remote=" + remoteSiteName
                    + ",dataInfo=" + dataInfoDesc(), e);
        }
        finally {
            closeOs(os);
            closeConn(conn);
        }
    }

    private ScmServerException getExceptonFromRemote() {
        InputStream errIs = null;
        BufferedReader reader = null;
        String errorBody = null;
        try {
            errIs = conn.getErrorStream();
            if (errIs == null) {
                return null;
            }

            StringBuilder strSB = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(errIs));
            String line;
            while ((line = reader.readLine()) != null) {
                strSB.append(line);
            }
            errorBody = strSB.toString();
            BSONObject bodyBSON = (BSONObject) JSON.parse(errorBody);
            int status = (int) bodyBSON.get("status");
            String message = (String) bodyBSON.get("message");
            String headerField = conn.getHeaderField(RestExceptionHandler.EXTRA_INFO_HEADER);
            BSONObject extraInfo =  (BSONObject) JSON.parse(headerField);
            return new ScmServerException(ScmError.getScmError(status), message, extraInfo);
        }
        catch (Exception e) {
            logger.error("failed to decode error response:body=" + errorBody, e);
        }
        finally {
            ScmSystemUtils.closeResource(reader);
            ScmSystemUtils.closeResource(errIs);
        }
        return null;
    }

    private ScmURLConfig getConfig(String hostPort, String wsName, String dataId, int dataType,
            Date dataCreateTime)
            throws ScmServerException {
        ScmURLConfig config = new ScmURLConfig();
        String addr = "http://" + hostPort + "/internal/v1/datasource/" + dataId + "?"
                + CommonDefine.RestArg.WORKSPACE_NAME + "=" + wsName + "&"
                + CommonDefine.RestArg.DATASOURCE_DATA_TYPE + "=" + dataType + "&"
                + CommonDefine.RestArg.DATASOURCE_DATA_CREATE_TIME + "="
                + dataCreateTime.getTime();
        config.setUrl(addr);
        config.setDoOutput(true);
        config.setDoInput(true);
        config.setChunkedStreamingMode(4096);
        config.setUseCaches(false);
        config.setRequestMethod("POST");
        Map<String, String> requestProperties = new HashMap<>();
        requestProperties.put("Connection", "Keep-Alive");
        requestProperties.put("Content-Type", "binary/octet-stream");
        config.setRequestProperties(requestProperties);
        config.setConnectTimeout(PropertiesUtils.getTransferConnectTimeout());
        config.setReadTimeout(PropertiesUtils.getTransferReadTimeout());
        return config;

    }

    private void closeConn(HttpURLConnection conn) {
        if (conn != null) {
            try {
                conn.disconnect();
            }
            catch (Exception e) {
                logger.warn("close connection failed:conn={}", conn, e);
            }
        }
    }

    private void closeOs(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            }
            catch (Exception e) {
                logger.warn("close resource failed:remote={}, dataInfo={}", remoteSiteName,
                        dataInfoDesc(), e);
            }
        }
    }
}
