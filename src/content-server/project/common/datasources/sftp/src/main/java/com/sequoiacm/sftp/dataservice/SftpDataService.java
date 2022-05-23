package com.sequoiacm.sftp.dataservice;

import com.jcraft.jsch.*;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.datasource.metadata.ScmSiteUrlWithConf;
import com.sequoiacm.infrastructure.crypto.AuthInfo;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiacm.sftp.SftpDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SftpDataService extends ScmService {

    private static final Logger logger = LoggerFactory.getLogger(SftpDataService.class);

    private AuthInfo authInfo;
    private Session session;
    private final List<SftpUrl> sftpUrls = new ArrayList<>();

    public SftpDataService(int siteId, ScmSiteUrl siteUrl) throws SftpDataException {
        super(siteId, siteUrl);
        parseUrls(siteUrl.getUrls());
        SftpDatasourceConfig.init(((ScmSiteUrlWithConf) siteUrl).getDataConf());
        this.session = createSession();
    }

    private void parseUrls(List<String> urls) throws SftpDataException {
        this.authInfo = ScmFilePasswordParser.parserFile(siteUrl.getPassword());
        try {
            for (String url : urls) {
                int i = url.indexOf(":");
                String host = url.substring(0, url.lastIndexOf(":"));
                int port = Integer.parseInt(url.substring(i + 1));
                this.sftpUrls.add(new SftpUrl(host, port));
            }
        }
        catch (Exception e) {
            throw new SftpDataException("failed to parse sftp urls:" + urls, e);
        }
    }

    public Session createSession() throws SftpDataException {
        if (sftpUrls.isEmpty()) {
            throw new SftpDataException("sftp url is empty");
        }
        JSch jSch = new JSch();
        Session session = null;
        Exception firstException = null;
        SftpUrl firstConnectErrorUrl = null;
        int count = 0;
        for (SftpUrl url : sftpUrls) {
            try {
                count++;
                session = jSch.getSession(siteUrl.getUser(), url.getHost(), url.getPort());
                session.setPassword(authInfo.getPassword());
                session.setConfig("StrictHostKeyChecking", "no");
                session.setServerAliveInterval(SftpDatasourceConfig.getServerAliveInterval());
                session.setTimeout(SftpDatasourceConfig.getSocketTimeout());
                session.connect(SftpDatasourceConfig.getConnectTimeout());
                return session;
            }
            catch (JSchException e) {
                releaseSession(session);
                if (firstException == null) {
                    firstException = e;
                    firstConnectErrorUrl = url;
                }
                if (count < sftpUrls.size()) {
                    logger.warn("failed to connect sftp, host={} , port={} ,error={}",
                            url.getHost(), url.getPort(), e);
                    logger.warn("retry next host...");
                }
            }
        }
        throw new SftpDataException("failed to connect sftp, host=" + firstConnectErrorUrl.getHost()
                + ", port=" + firstConnectErrorUrl.getPort(), firstException);

    }

    @Override
    public String getType() {
        return CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SFTP_STR;
    }

    public ChannelSftp getSftp() throws SftpDataException {
        Channel channel = null;
        try {
            channel = this.session.openChannel("sftp");
            channel.connect(SftpDatasourceConfig.getConnectTimeout());
            return (ChannelSftp) channel;
        }
        catch (JSchException e) {
            if (!session.isConnected()) {
                reCreateSession();
                try {
                    channel = this.session.openChannel("sftp");
                    channel.connect(SftpDatasourceConfig.getConnectTimeout());
                    return (ChannelSftp) channel;
                }
                catch (JSchException e1) {
                    throw new SftpDataException("failed to get sftp channel.", e1);
                }
            }
            else {
                throw new SftpDataException("failed to get sftp channel.", e);
            }
        }
    }

    private void reCreateSession() throws SftpDataException {
        synchronized (this) {
            if (!this.session.isConnected()) {
                this.session = createSession();
            }
        }
    }

    public void closeSftp(ChannelSftp channelSftp) {
        if (channelSftp == null) {
            return;
        }
        try {
            channelSftp.disconnect();
        }
        catch (Exception e) {
            logger.warn("failed to close channelSftp.", e);
        }
    }

    @Override
    public void clear() {
        releaseSession(session);
    }

    private void releaseSession(Session session) {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    private static class SftpUrl {
        private String host;
        private int port;

        public SftpUrl(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }
}