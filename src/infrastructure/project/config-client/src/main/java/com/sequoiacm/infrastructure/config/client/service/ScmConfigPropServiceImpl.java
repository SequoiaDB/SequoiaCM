package com.sequoiacm.infrastructure.config.client.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.sequoiacm.infrastructure.config.client.core.ScmConfPropVerifiersMgr;
import com.sequoiacm.infrastructure.config.client.dao.ScmConfigPropsDao;
import com.sequoiacm.infrastructure.config.client.dao.ScmConfigPropsDaoFactory;
import com.sequoiacm.infrastructure.config.client.props.ScmCommonUtil;
import com.sequoiacm.infrastructure.config.client.props.ScmConfPropsScanner;
import com.sequoiacm.infrastructure.config.core.common.ScmServiceUpdateConfigResult;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

@Service
public class ScmConfigPropServiceImpl implements ScmConfigPropService {
    private static final Logger logger = LoggerFactory.getLogger(ScmConfigPropServiceImpl.class);
    @Autowired
    private ScmConfPropVerifiersMgr verfierMgr;

    @Autowired
    private ContextRefresher contextRefresher;

    @Autowired
    private ScmConfigPropsDaoFactory daoFactory;

    @Autowired
    private ScmConfPropsScanner scmConfPropsScanner;

    @Autowired
    private ScmCommonUtil commonUtil;

    @Autowired
    private Environment environment;

    @Override
    public synchronized ScmServiceUpdateConfigResult updateConfigProps(
            Map<String, String> updateProps, List<String> deleteProps, boolean acceptUnknownProps)
            throws ScmConfigException {
        logger.info("update node config: newProps={}, deleteProps={}, acceptUnknownProps={}",
                updateProps, deleteProps, acceptUnknownProps);
        verfierMgr.checkProps(updateProps, deleteProps, acceptUnknownProps);

        boolean hasTriggerRefreshContext = false;
        ScmConfigPropsDao dao = daoFactory.createConfigPropsDao();
        try {
            boolean isDifferentFromOld = dao.modifyPropsFile(updateProps, deleteProps);
            if (isDifferentFromOld) {
                hasTriggerRefreshContext = true;
                contextRefresher.refresh();
                // 触发 spring refresh scope bean 的加载，如果配置文件绑定 bean 有问题可以在这里触发绑定报错，从而回滚
                commonUtil.getRefreshableBeans();
            }
        }
        catch (Exception e) {
            try {
                dao.rollback();
            }
            catch (ScmConfigException ex) {
                logger.error("failed to rollback config file", ex);
                throw e;
            }

            if (!hasTriggerRefreshContext) {
                throw e;
            }

            try {
                contextRefresher.refresh();
            }
            catch (Exception ignoreException) {
                logger.error(
                        "failed to rollback context, this node status may be not normal, should be restart",
                        ignoreException);
                throw new ScmConfigException(ScmConfError.SYSTEM_ERROR,
                        "failed to update conf, and rollback context failed, this node status may be not normal, should be restart",
                        e);
            }
            throw e;
        }

        Set<String> rebootConf = new HashSet<>();
        Map<String, String> adjustedConfMap = new HashMap<>();
        for (Map.Entry<String, String> entry : updateProps.entrySet()) {
            if (!scmConfPropsScanner.isRefreshableConfProp(entry.getKey())) {
                rebootConf.add(entry.getKey());
            }
            else {
                String adjustedConf = environment.getProperty(entry.getKey());
                if (!Objects.equals(adjustedConf, entry.getValue())) {
                    adjustedConfMap.put(entry.getKey(), adjustedConf);
                }
            }
        }
        for (String key : deleteProps) {
            if (!scmConfPropsScanner.isRefreshableConfProp(key)) {
                rebootConf.add(key);
            }
        }

        ScmServiceUpdateConfigResult res = new ScmServiceUpdateConfigResult();
        res.setAdjustedConf(adjustedConfMap);
        res.setRebootConf(rebootConf);
        return res;
    }

}
