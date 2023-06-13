package com.sequoiacm.contentserver.service.impl;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.ScmServer;
import com.sequoiacm.contentserver.config.PropertiesUtils;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.metadata.MetaDataManager;
import com.sequoiacm.contentserver.remote.ContentServerClient;
import com.sequoiacm.contentserver.remote.ContentServerClientFactory;
import com.sequoiacm.contentserver.service.ISystemService;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.contentserver.site.ScmContentServerInfo;
import com.sequoiacm.contentserver.site.ScmSite;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;

@Service
public class SystemServiceImpl implements ISystemService {
    private static final Logger logger = LoggerFactory.getLogger(SystemServiceImpl.class);

    @Autowired
    private Environment environment;

    @Override
    public BSONObject getConfs(String[] keys) throws ScmServerException {
        BSONObject respBSON = new BasicBSONObject();
        for (String key : keys) {
            String value = PropertiesUtils.getProperty(key);
            if (value == null) {
                value = environment.getProperty(key);
            }
            respBSON.put(key, value);
        }
        return respBSON;
    }

    private List<ScmContentServerInfo> getOtherContentServers(int siteId)
            throws ScmServerException {
        List<ScmContentServerInfo> contentServerList = ScmContentModule.getInstance()
                .getContentServerList(siteId);
        if (siteId != ScmContentModule.getInstance().getLocalSite()) {
            return contentServerList;
        }
        Iterator<ScmContentServerInfo> it = contentServerList.iterator();
        while (it.hasNext()) {
            ScmContentServerInfo scmContentServerInfo = it.next();
            if (scmContentServerInfo.getId() == ScmServer.getInstance().getContentServerInfo().getId()) {
                it.remove();
                break;
            }
        }
        return contentServerList;
    }

    private List<ScmContentServerInfo> getOtherContentServers() throws ScmServerException {
        List<ScmContentServerInfo> contentServerList = ScmContentModule.getInstance().getContentServerList();
        Iterator<ScmContentServerInfo> it = contentServerList.iterator();
        while (it.hasNext()) {
            ScmContentServerInfo scmContentServerInfo = it.next();
            if (scmContentServerInfo.getId() == ScmServer.getInstance().getContentServerInfo().getId()) {
                it.remove();
                break;
            }
        }
        return contentServerList;
    }

    @Override
    public BSONObject reloadSiteBizConf(int siteId, boolean isMetadataOnly)
            throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();

        ScmSite siteInfo = contentModule.getSiteInfo(siteId);
        if (siteInfo == null) {
            throw new ScmServerException(ScmError.SITE_NOT_EXIST,
                    "site not exist:serverId=" + siteId);
        }
        if (contentModule.getServerConnectionList(siteId).size() > 0) {
            if (contentModule.getLocalSite() == siteId) {
                BasicBSONList resultList = new BasicBSONList();
                List<ScmContentServerInfo> otherServers = getOtherContentServers(siteId);
                reloadMyself(resultList, isMetadataOnly);
                reloadOtherServers(otherServers, resultList, isMetadataOnly);
                return resultList;
            }
            else {
                int remoteSite;
                if (contentModule.getLocalSite() == contentModule.getMainSite()) {
                    remoteSite = siteId;
                }
                else {
                    remoteSite = contentModule.getMainSite();
                }
                return forwardToRemoteSite(contentModule.getSiteInfo(remoteSite).getName(),
                        CommonDefine.NodeScope.SCM_NODESCOPE_CENTER, siteId, isMetadataOnly);
            }
        }
        logger.info("node not exist:siteId={}", siteId);
        return new BasicBSONList();
    }

    @Override
    public BSONObject reloadNodeBizConf(int nodeId, boolean isMetadataOnly)
            throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        int serverId = nodeId;
        ScmContentServerInfo info = contentModule.getServerInfo(serverId);
        if (info == null) {
            throw new ScmServerException(ScmError.SERVER_NOT_EXIST,
                    "server not exist:serverId=" + serverId);
        }
        int mainSiteId = contentModule.getMainSite();
        if (ScmServer.getInstance().getContentServerInfo().getId() == serverId) {
            BasicBSONList resultList = new BasicBSONList();
            reloadMyself(resultList, isMetadataOnly);
            return resultList;
        }
        else if (contentModule.getLocalSite() == mainSiteId
                || info.getSite().getId() == contentModule.getLocalSite()) {
            return reloadRemoteBizConf(info, false, isMetadataOnly);
        }
        else {
            BSONObject res = forwardToRemoteSite(contentModule.getSiteInfo(mainSiteId).getName(),
                    CommonDefine.NodeScope.SCM_NODESCOPE_NODE, serverId, isMetadataOnly);
            return res;
        }
    }

    @Override
    public BSONObject reloadAllNodeBizConf(boolean isMetadataOnly) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        int mainSiteId = contentModule.getMainSite();
        if (contentModule.getLocalSite() == mainSiteId) {
            BasicBSONList resultList = new BasicBSONList();
            List<ScmContentServerInfo> otherServers = getOtherContentServers();
            reloadMyself(resultList, isMetadataOnly);
            reloadOtherServers(otherServers, resultList, isMetadataOnly);
            return resultList;
        }
        else {
            BSONObject res = forwardToRemoteSite(contentModule.getSiteInfo(mainSiteId).getName(),
                    CommonDefine.NodeScope.SCM_NODESCOPE_ALL, 0, isMetadataOnly);
            return res;
        }
    }

    private void reloadOtherServers(List<ScmContentServerInfo> serverInfoList,
            BasicBSONList resultList, boolean isMetadataOnly) throws ScmServerException {
        for (ScmContentServerInfo info : serverInfoList) {
            BasicBSONList result = reloadRemoteBizConf(info, true, isMetadataOnly);
            resultList.addAll(result);
        }
    }

    private BasicBSONList reloadRemoteBizConf(ScmContentServerInfo info, boolean isProcessException,
            boolean isMetadataOnly) throws ScmServerException {
        ReloadBizConfResult errRes;
        try {
            ContentServerClient client = ContentServerClientFactory
                    .getFeignClientByNodeUrl(info.getHostName() + ":" + info.getPort());
            BasicBSONList res = client.reloadBizConf(CommonDefine.NodeScope.SCM_NODESCOPE_NODE,
                    info.getId(), isMetadataOnly);
            return res;
        }
        // catch (ScmServerException e) {
        // if (isProcessException) {
        // errRes = new ReloadBizConfResult(info.getId(),
        // info.getSite().getId(),
        // info.getHostName(), info.getPort(), e.getError(),
        // e.getMessage());
        // }
        // else {
        // throw e;
        // }
        // }
        catch (Exception e) {
            if (isProcessException) {
                errRes = new ReloadBizConfResult(info.getId(), info.getSite().getId(),
                        info.getHostName(), info.getPort(), ScmError.SYSTEM_ERROR.getErrorCode(),
                        e.getMessage());
            }
            else {
                throw new ScmSystemException("reloadRemoteBizConf failed:remote="
                        + info.getHostName() + ":" + info.getPort(), e);
            }
        }
        BasicBSONList errList = new BasicBSONList();
        errList.add(errRes.toBsonObject());
        return errList;
    }

    private void reloadMyself(BasicBSONList resultList, boolean isMetadataOnly)
            throws ScmServerException {
        logger.info("start to reload business configure...");
        if (isMetadataOnly) {
            logger.debug("reload metadata configure only");
            MetaDataManager.reload();
        }
        else {
            ScmContentModule.reload();
        }
        ScmContentServerInfo info = ScmServer.getInstance().getContentServerInfo();
        ReloadBizConfResult result = new ReloadBizConfResult(info.getId(), info.getSite().getId(),
                info.getHostName(), info.getPort(), 0, "");
        logger.info("reload configure business success");
        resultList.add(result.toBsonObject());
    }

    private BasicBSONList forwardToRemoteSite(String siteName, int scope, int id,
            boolean isMetadataOnly) throws ScmServerException {
        ContentServerClient client = ContentServerClientFactory
                .getFeignClientByServiceName(siteName);
        return client.reloadBizConf(scope, id, isMetadataOnly);
    }

    @Override
    public MetaCursor getNodeList(BSONObject condition) throws ScmServerException {
        MetaAccessor serverAccessor = ScmContentModule.getInstance().getMetaService()
                .getMetaSource().getServerAccessor();
        try {
            return serverAccessor.query(condition, null, null);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "Failed to get node list: condition=" + condition, e);
        }
    }
}

class ReloadBizConfResult {
    private int serverId;
    private int siteId;
    private String hostName;
    private int port;
    private int flag;
    private String errorMsg;

    public ReloadBizConfResult(int serverId, int siteId, String hostName, int port, int flag,
            String errorMsg) {
        this.serverId = serverId;
        this.siteId = siteId;
        this.hostName = hostName;
        this.port = port;
        this.flag = flag;
        this.errorMsg = errorMsg;
    }

    public BSONObject toBsonObject() throws ScmServerException {
        BSONObject result = new BasicBSONObject();
        result.put(FieldName.ReloadBizConf.FIELD_SERVER_ID, serverId);
        result.put(FieldName.ReloadBizConf.FIELD_SITE_ID, siteId);
        result.put(FieldName.ReloadBizConf.FIELD_HOSTNAME, hostName);
        result.put(FieldName.ReloadBizConf.FIELD_PORT, port);
        result.put(FieldName.ReloadBizConf.FIELD_FLAG, flag);
        result.put(FieldName.ReloadBizConf.FIELD_ERRORMSG, errorMsg);

        return result;
    }
}
