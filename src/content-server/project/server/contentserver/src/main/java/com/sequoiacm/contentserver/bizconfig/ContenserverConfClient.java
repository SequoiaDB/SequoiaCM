package com.sequoiacm.contentserver.bizconfig;

import java.util.List;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.client.ScmConfSubscriber;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataAttributeConfig;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataAttributeConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataAttributeConfigUpdator;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataClassConfig;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataClassConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataClassConfigUpdator;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataConfig;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataConfigUpdator;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceConfig;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceFilter;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceUpdator;

public class ContenserverConfClient {
    private static ContenserverConfClient instance = new ContenserverConfClient();
    private ScmConfClient client;

    public static ContenserverConfClient getInstance() {
        return instance;
    }

    public ContenserverConfClient() {

    }

    public ContenserverConfClient init(ScmConfClient client) {
        this.client = client;
        return this;
    }

    public void subscribeWithAsyncRetry(ScmConfSubscriber subscriber) throws ScmServerException {
        try {
            client.subscribeWithAsyncRetry(subscriber);
        }
        catch (ScmConfigException e) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR, e.getMessage(), e);
        }
    }

    public WorkspaceConfig createWorkspace(WorkspaceConfig config) throws ScmServerException {
        try {
            return (WorkspaceConfig) client.createConf(ScmConfigNameDefine.WORKSPACE, config,
                    false);
        }
        catch (ScmConfigException e) {
            if (e.getError() == ScmConfError.WORKSPACE_EXIST) {
                throw new ScmServerException(ScmError.WORKSPACE_EXIST, e.getMessage(), e);
            }
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR, e.getMessage(), e);
        }
    }

    public List<Version> getConfigVersion(String configName, VersionFilter filter)
            throws ScmServerException {
        try {
            return client.getConfVersion(configName, filter);
        }
        catch (ScmConfigException e) {
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR, e.getMessage(), e);
        }
    }

    public Config deleteWorkspace(WorkspaceFilter filter) throws ScmServerException {
        try {
            return client.deleteConf(ScmConfigNameDefine.WORKSPACE, filter, false);
        }
        catch (ScmConfigException e) {
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR, e.getMessage(), e);
        }
    }

    public WorkspaceConfig updateWorkspaceConf(WorkspaceUpdator updator) throws ScmServerException {
        try {
            return (WorkspaceConfig) client.updateConfig(ScmConfigNameDefine.WORKSPACE, updator,
                    false);
        }
        catch (ScmConfigException e) {
            if (e.getError() == ScmConfError.CLIENT_WROKSPACE_CACHE_EXPIRE) {
                throw new ScmServerException(ScmError.WORKSPACE_CACHE_EXPIRE, e.getMessage(), e);
            }
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR, e.getMessage(), e);
        }
    }

    public MetaDataAttributeConfig createAttribute(MetaDataAttributeConfig attribute)
            throws ScmServerException {
        MetaDataConfig config = new MetaDataConfig(attribute);
        try {
            MetaDataConfig resp = (MetaDataConfig) client.createConf(ScmConfigNameDefine.META_DATA,
                    config, false);
            return resp.getAttributeConfig();
        }
        catch (ScmConfigException e) {
            if (e.getError() == ScmConfError.ATTRIBUTE_EXIST) {
                throw new ScmServerException(ScmError.METADATA_ATTR_EXIST, e.getMessage(), e);
            }
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR, e.getMessage(), e);
        }
    }

    public MetaDataClassConfig createClass(MetaDataClassConfig classConfig)
            throws ScmServerException {
        MetaDataConfig config = new MetaDataConfig(classConfig);
        try {
            MetaDataConfig resp = (MetaDataConfig) client.createConf(ScmConfigNameDefine.META_DATA,
                    config, false);
            return resp.getClassConfig();
        }
        catch (ScmConfigException e) {
            if (e.getError() == ScmConfError.CLASS_EXIST) {
                throw new ScmServerException(ScmError.METADATA_CLASS_EXIST, e.getMessage(), e);
            }
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR, e.getMessage(), e);
        }
    }

    public void deleteClass(MetaDataClassConfigFilter classFilter) throws ScmServerException {
        MetaDataConfigFilter filter = new MetaDataConfigFilter(classFilter);
        try {
            client.deleteConf(ScmConfigNameDefine.META_DATA, filter, false);
        }
        catch (ScmConfigException e) {
            if (e.getError() == ScmConfError.CLASS_NOT_EXIST) {
                throw new ScmServerException(ScmError.METADATA_CLASS_NOT_EXIST, e.getMessage(), e);
            }
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR, e.getMessage(), e);
        }
    }

    public void deleteAttribute(MetaDataAttributeConfigFilter attributeFilter)
            throws ScmServerException {
        MetaDataConfigFilter filter = new MetaDataConfigFilter(attributeFilter);
        try {
            client.deleteConf(ScmConfigNameDefine.META_DATA, filter, false);
        }
        catch (ScmConfigException e) {
            if (e.getError() == ScmConfError.ATTRIBUTE_NOT_EXIST) {
                throw new ScmServerException(ScmError.METADATA_ATTR_NOT_EXIST, e.getMessage(), e);
            }
            if (e.getError() == ScmConfError.ATTRIBUTE_IN_CLASS) {
                throw new ScmServerException(ScmError.METADATA_ATTR_DELETE_FAILED, e.getMessage(),
                        e);
            }
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR, e.getMessage(), e);
        }
    }

    public MetaDataAttributeConfig updateAttribute(MetaDataAttributeConfigUpdator attributeUpdator)
            throws ScmServerException {
        MetaDataConfigUpdator updator = new MetaDataConfigUpdator(attributeUpdator);
        try {
            MetaDataConfig resp = (MetaDataConfig) client
                    .updateConfig(ScmConfigNameDefine.META_DATA, updator, false);
            return resp.getAttributeConfig();
        }
        catch (ScmConfigException e) {
            if (e.getError() == ScmConfError.ATTRIBUTE_NOT_EXIST) {
                throw new ScmServerException(ScmError.METADATA_ATTR_NOT_EXIST, e.getMessage(), e);
            }
            if (e.getError() == ScmConfError.ATTRIBUTE_EXIST) {
                throw new ScmServerException(ScmError.METADATA_ATTR_EXIST, e.getMessage(), e);
            }
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR, e.getMessage(), e);
        }
    }

    public MetaDataClassConfig updateClass(MetaDataClassConfigUpdator classUpdator)
            throws ScmServerException {
        MetaDataConfigUpdator updator = new MetaDataConfigUpdator(classUpdator);
        try {
            MetaDataConfig resp = (MetaDataConfig) client
                    .updateConfig(ScmConfigNameDefine.META_DATA, updator, false);
            return resp.getClassConfig();
        }
        catch (ScmConfigException e) {
            if (e.getError() == ScmConfError.CLASS_NOT_EXIST) {
                throw new ScmServerException(ScmError.METADATA_CLASS_NOT_EXIST, e.getMessage(), e);
            }
            if (e.getError() == ScmConfError.ATTRIBUTE_NOT_EXIST) {
                throw new ScmServerException(ScmError.METADATA_ATTR_NOT_EXIST, e.getMessage(), e);
            }
            if (e.getError() == ScmConfError.CLASS_EXIST) {
                throw new ScmServerException(ScmError.METADATA_CLASS_EXIST, e.getMessage(), e);
            }
            if (e.getError() == ScmConfError.ATTRIBUTE_ALREADY_IN_CLASS) {
                throw new ScmServerException(ScmError.METADATA_ATTR_ALREADY_IN_CLASS,
                        e.getMessage(), e);
            }
            if (e.getError() == ScmConfError.ATTRIBUTE_NOT_IN_CLASS) {
                throw new ScmServerException(ScmError.METADATA_ATTR_NOT_IN_CLASS, e.getMessage(),
                        e);
            }
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR, e.getMessage(), e);
        }
    }
}
