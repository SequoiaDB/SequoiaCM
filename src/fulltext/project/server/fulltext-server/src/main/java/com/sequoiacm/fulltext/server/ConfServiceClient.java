package com.sequoiacm.fulltext.server;

import java.util.List;

import com.sequoiacm.infrastructure.config.client.NotifyCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.infrastructure.config.client.EnableConfClient;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteConfig;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteFilter;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceConfig;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceFilter;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceUpdater;
import com.sequoiacm.infrastructure.fulltext.common.ScmWorkspaceFulltextExtData;

@EnableConfClient
@Component
public class ConfServiceClient {
    @Autowired
    private ScmConfClient confClient;

    public ConfServiceClient() {

    }

    public void subscribe(String businessType, NotifyCallback notifyCallback)
            throws ScmConfigException {
        confClient.subscribe(businessType, notifyCallback);
    }

    public void updateWsExternalData(WsFulltextExtDataModifier modifier) throws FullTextException {
        WorkspaceUpdater updator = new WorkspaceUpdater(modifier.getWs(), modifier.getMatcher());
        updator.setExternalData(modifier.getModifier());
        try {
            confClient.updateConfig(ScmBusinessTypeDefine.WORKSPACE, updator, false);
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
            List<Config> configs = confClient.getConf(ScmBusinessTypeDefine.WORKSPACE,
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
            List<Config> configs = confClient.getConf(ScmBusinessTypeDefine.WORKSPACE,
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
            List<Config> configs = confClient.getConf(ScmBusinessTypeDefine.SITE, new SiteFilter());
            return (List<SiteConfig>) (Object) configs;
        }
        catch (ScmConfigException e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to get site list from config-server", e);
        }

    }

    public SiteConfig getSite(String name) throws FullTextException {
        try {
            List<Config> configs = confClient.getConf(ScmBusinessTypeDefine.SITE,
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
            wsInfo = (WorkspaceConfig) confClient.getOneConf(ScmBusinessTypeDefine.WORKSPACE, filter);
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
