package com.sequoiacm.config.framework.metadata.operator;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.config.framework.common.DefaultVersionDao;
import com.sequoiacm.config.framework.metadata.dao.MetaDataConfCreatorDao;
import com.sequoiacm.config.framework.metadata.dao.MetaDataConfDeletorDao;
import com.sequoiacm.config.framework.metadata.dao.MetaDataConfUpdatorDao;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.config.framework.operator.ScmConfOperator;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdator;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersionFilter;
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

@Component
public class MetaDataConfOperator implements ScmConfOperator {

    @Autowired
    DefaultVersionDao versionDao;

    @Autowired
    MetaDataConfCreatorDao confCreatorDao;

    @Autowired
    MetaDataConfDeletorDao confDeletorDao;

    @Autowired
    MetaDataConfUpdatorDao confUpdatorDao;

    @Override
    public List<Config> getConf(ConfigFilter filter) throws ScmConfigException {
        throw new ScmConfigException(ScmConfError.UNSUPPORTED_OPTION,
                "unsupport to query meta data info");
    }

    @Override
    public List<Version> getConfVersion(VersionFilter filter) throws ScmConfigException {
        DefaultVersionFilter versionFilter = (DefaultVersionFilter) filter;
        return versionDao.getVerions(versionFilter);
    }

    @Override
    public ScmConfOperateResult updateConf(ConfigUpdator configUpdator) throws ScmConfigException {
        MetaDataConfigUpdator metaDataUpdator = (MetaDataConfigUpdator) configUpdator;
        MetaDataAttributeConfigUpdator attributeUpdator = metaDataUpdator.getAttributeUpdator();
        if (attributeUpdator != null) {
            return confUpdatorDao.updateAttribute(attributeUpdator);
        }
        MetaDataClassConfigUpdator classUpdator = metaDataUpdator.getClassUpdator();
        if (classUpdator != null) {
            return confUpdatorDao.updateClass(classUpdator);
        }
        return new ScmConfOperateResult();
    }

    @Override
    public ScmConfOperateResult deleteConf(ConfigFilter configFilter) throws ScmConfigException {
        MetaDataConfigFilter metaDataFilter = (MetaDataConfigFilter) configFilter;
        MetaDataAttributeConfigFilter attributeConfigFilter = metaDataFilter.getAttributeFilter();
        if (attributeConfigFilter != null) {
            return confDeletorDao.deleteAttribute(attributeConfigFilter);
        }

        MetaDataClassConfigFilter classConfigFilter = metaDataFilter.getClassFilter();
        if (classConfigFilter != null) {
            return confDeletorDao.deleteClass(classConfigFilter);
        }

        return new ScmConfOperateResult();
    }

    @Override
    public ScmConfOperateResult createConf(Config config) throws ScmConfigException {
        MetaDataConfig metaDataConfig = (MetaDataConfig) config;
        MetaDataAttributeConfig attributeConfig = metaDataConfig.getAttributeConfig();
        if (attributeConfig != null) {
            return confCreatorDao.createAttribute(attributeConfig);
        }

        MetaDataClassConfig classConfig = metaDataConfig.getClassConfig();
        if (classConfig != null) {
            return confCreatorDao.createClass(classConfig);
        }

        return new ScmConfOperateResult();
    }

}