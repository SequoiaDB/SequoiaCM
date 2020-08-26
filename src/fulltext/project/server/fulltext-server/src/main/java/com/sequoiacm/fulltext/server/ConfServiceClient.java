package com.sequoiacm.fulltext.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.infrastructure.config.client.EnableConfClient;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.client.ScmConfSubscriber;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteConfig;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteFilter;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceConfig;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceFilter;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceUpdator;
import com.sequoiacm.infrastructure.fulltext.common.ScmWorkspaceFulltextExtData;

@EnableConfClient
@Component
public class ConfServiceClient {
    @Autowired
    private ScmConfClient confClient;

    public ConfServiceClient() {

    }

    public void registerSubscriber(ScmConfSubscriber subscriber) throws FullTextException {
        try {
            confClient.subscribeWithAsyncRetry(subscriber);
        }
        catch (ScmConfigException e) {
            throw new FullTextException(ScmError.CONFIG_SERVER_ERROR,
                    "failed to subscribe config in config server", e);
        }
    }

    public void updateWsExternalData(WsFulltextExtDataModifier modifier) throws FullTextException {
        WorkspaceUpdator updator = new WorkspaceUpdator(modifier.getWs(), modifier.getMatcher());
        updator.setExternalData(modifier.getModifier());
        try {
            confClient.updateConfig(ScmConfigNameDefine.WORKSPACE, updator, false);
        }
        catch (ScmConfigException e) {
            throw new FullTextException(ScmError.CONFIG_SERVER_ERROR,
                    "failed to update workspace info in config server:ws=" + modifier.getWs()
                            + ", extDataModifier=" + modifier,
                    e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<WorkspaceConfig> getWorkspaceList() throws FullTextException {
        try {
            List<Config> configs = confClient.getConf(ScmConfigNameDefine.WORKSPACE,
                    new WorkspaceFilter());
            return (List<WorkspaceConfig>) (Object) configs;
        }
        catch (ScmConfigException e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to get Workspace list from config-server");
        }
    }

    public WorkspaceConfig getWorkspace(String name) throws FullTextException {
        try {
            List<Config> configs = confClient.getConf(ScmConfigNameDefine.WORKSPACE,
                    new WorkspaceFilter(name));
            if (configs == null || configs.size() == 0) {
                return null;
            }
            return (WorkspaceConfig) configs.get(0);
        }
        catch (ScmConfigException e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to get workspace from config-server:" + name);
        }

    }

    @SuppressWarnings("unchecked")
    public List<SiteConfig> getSiteList() throws FullTextException {
        try {
            List<Config> configs = confClient.getConf(ScmConfigNameDefine.SITE, new SiteFilter());
            return (List<SiteConfig>) (Object) configs;
        }
        catch (ScmConfigException e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to get site list from config-server", e);
        }

    }

    public SiteConfig getSite(String name) throws FullTextException {
        try {
            List<Config> configs = confClient.getConf(ScmConfigNameDefine.SITE,
                    new SiteFilter(name));
            if (configs == null || configs.size() == 0) {
                return null;
            }
            return (SiteConfig) configs.get(0);
        }
        catch (ScmConfigException e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to get site from config-server:" + name, e);
        }

    }

    public ScmWorkspaceFulltextExtData getWsExternalData(String ws) throws FullTextException {
        WorkspaceFilter filter = new WorkspaceFilter(ws);
        WorkspaceConfig wsInfo;
        try {
            wsInfo = (WorkspaceConfig) confClient.getOneConf(ScmConfigNameDefine.WORKSPACE, filter);
        }
        catch (ScmConfigException e) {
            throw new FullTextException(ScmError.CONFIG_SERVER_ERROR,
                    "failed to get workspace info from config server:" + ws, e);
        }
        if (wsInfo == null) {
            throw new FullTextException(ScmError.WORKSPACE_NOT_EXIST, "workspace not exist:" + ws);
        }
        return new ScmWorkspaceFulltextExtData(wsInfo.getWsName(), wsInfo.getWsId(),
                wsInfo.getExternalData());
    }
}
