package com.sequoiacm.contentserver.remote;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import com.sequoiacm.contentserver.common.ScmRestClientUtils;
import com.sequoiacm.infrastructure.dispatcher.ScmURLConfig;
import org.bson.BSONObject;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.config.PropertiesUtils;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.exception.ScmError;
import org.springframework.cloud.client.ServiceInstance;

public class ScmInnerRemoteDataWriter {
    private static final Logger logger = LoggerFactory.getLogger(ScmInnerRemoteDataWriter.class);

    private ScmDataInfo dataInfo;
    private String remoteSiteName;
    private HttpURLConnection conn;
    private OutputStream os;

    public ScmInnerRemoteDataWriter(int remoteSiteId, final ScmWorkspaceInfo wsInfo, final ScmDataInfo dataInfo)
            throws ScmServerException {
        this.dataInfo = dataInfo;
        ScmContentModule contentModule = ScmContentModule.getInstance();
        remoteSiteName = contentModule.getSiteInfo(remoteSiteId).getName();
        ServiceInstance instance = LoadBalancedUtil.chooseInstance(remoteSiteName);
        final String hostPort = instance.getHost() + ":" + instance.getPort();
        try {
            conn = ScmRestClientUtils.getScmRestClient().getHttpURLConnection(
                    getConfig(hostPort, wsInfo.getName(), dataInfo), instance);
            os = conn.getOutputStream();
        }
        catch (IOException e) {
            closeOs(os);
            closeConn(conn);
            throw new ScmServerException(ScmError.NETWORK_IO, "write data to remote failed:remote="
                    + remoteSiteName + ",dataInfo=" + dataInfo, e);
        }
        catch (ScmServerException e) {
            logger.error("write data to remote failed:remote=" + remoteSiteName + ",dataInfo="
                    + dataInfo);
            closeOs(os);
            closeConn(conn);
            throw e;
        }
        catch (Exception e) {
            closeOs(os);
            closeConn(conn);
            throw new ScmSystemException("write data to remote failed:remote=" + remoteSiteName
                    + ",dataInfo=" + dataInfo, e);
        }
    }

    public void write(byte[] content, int offset, int len) throws ScmServerException {
        try {
            os.write(content, offset, len);
        }
        catch (IOException e) {
            ScmServerException remoteException = getExceptonFromRemote();
            if (remoteException != null) {
                logger.error("write data to remote failed:remote={},dataInfo={}", remoteSiteName,
                        dataInfo, e);
                throw remoteException;
            }
            throw new ScmServerException(ScmError.NETWORK_IO, "write data to remote failed:remote="
                    + remoteSiteName + ",dataInfo=" + dataInfo, e);
        }
        catch (Exception e) {
            throw new ScmSystemException("write data to remote failed:remote=" + remoteSiteName
                    + ",dataInfo=" + dataInfo, e);
        }
    }

    // remove the writing file & release writer's resources
    public void cancel() {
        closeConn(conn);
        closeOs(os);
    }

    // close and commit this file & release writer's resources
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
                        + ",dataInfo=" + dataInfo + ",repstatus=" + respStatus);
            }
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (IOException e) {
            ScmServerException remoteException = getExceptonFromRemote();
            if (remoteException != null) {
                logger.error("write data to remote failed:remote={},dataInfo={}", remoteSiteName,
                        dataInfo, e);
                throw remoteException;
            }
            throw new ScmServerException(ScmError.NETWORK_IO, "write data to remote failed:remote="
                    + remoteSiteName + ",dataInfo=" + dataInfo, e);
        }
        catch (Exception e) {
            throw new ScmSystemException("write data to remote failed:remote=" + remoteSiteName
                    + ",dataInfo=" + dataInfo, e);
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
            return new ScmServerException(ScmError.getScmError(status), message);
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

    private ScmURLConfig getConfig(String hostPort, String wsName, ScmDataInfo dataInfo)
            throws ScmServerException {
        ScmURLConfig config = new ScmURLConfig();
        String addr = "http://" + hostPort + "/internal/v1/datasource/" + dataInfo.getId() + "?"
                + CommonDefine.RestArg.WORKSPACE_NAME + "=" + wsName + "&"
                + CommonDefine.RestArg.DATASOURCE_DATA_TYPE + "=" + dataInfo.getType() + "&"
                + CommonDefine.RestArg.DATASOURCE_DATA_CREATE_TIME + "="
                + dataInfo.getCreateTime().getTime();
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
                        dataInfo);
            }
        }
    }
}
