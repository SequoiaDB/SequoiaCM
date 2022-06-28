package com.sequoiacm.config.tools.command;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.element.ScmUpdateConfResult;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.config.tools.common.ConfigType;
import com.sequoiacm.config.tools.common.ScmConfigCommandUtil;
import com.sequoiacm.config.tools.exception.ScmExitCode;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ScmUpdateConfigImpl extends AbstractScmRefreshConfig {

    private Logger logger = LoggerFactory.getLogger(ScmUpdateConfigImpl.class);

    public ScmUpdateConfigImpl() throws ScmToolsException {
        super("updateconfig");
    }

    @Override
    public void operation() throws ScmToolsException {
        ScmSession ss = null;
        try {
            ss = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION, new ScmConfigOption(
                    ScmConfigCommandUtil.parseListUrls(gatewayUrl), username, password));
            ScmUpdateConfResultSet resultSet = updateConfigProp(ss, type, name, config);
            List<ScmUpdateConfResult> failures = resultSet.getFailures();
            List<ScmUpdateConfResult> successes = resultSet.getSuccesses();
            if (failures.size() > 0) {
                for (ScmUpdateConfResult result : failures) {
                    System.out.println("update config failed: serviceName=" + result.getServiceName() + ", instance=" + result.getInstance() +
                            ", errorMessage=" + result.getErrorMessage());
                    logger.error("update config failed: serviceName={},nodeName={},config={},errorMessage={}", result.getServiceName(),
                            result.getInstance(), config, result.getErrorMessage());
                }
            }
            if (successes.size() > 0) {
                for (ScmUpdateConfResult result : successes) {
                    System.out.println("update config success: serviceName=" + result.getServiceName() + ", instance=" + result.getInstance());
                    logger.info("update config success: serviceName={},nodeName={},config={}", result.getServiceName(),
                            result.getInstance(), config);
                }
            }
        } catch (Exception e) {
            logger.error("update config failed: name={},config={}", name, config, e);
            ScmCommon.throwToolException("update config failed", e);
        } finally {
            if (null != ss) {
                ss.close();
            }
        }
    }

    private ScmUpdateConfResultSet updateConfigProp(ScmSession ss, Integer type, String name, String config) throws Exception {
        String[] nameArr = name.split(",");
        String key = "";
        String value = "";
        int index = config.indexOf('=');
        if (index != -1) {
            key = config.substring(0, index);
            value = config.substring(index + 1);
        } else {
            throw new ScmToolsException("invalid argument, config item only can contain one equal sign, please refer exam: key=value." +
                    " your origin value: " + config, ScmExitCode.INVALID_ARG);
        }
        ScmConfigProperties conf = null;
        if (type.equals(ConfigType.BY_SERVICE.getType())) {
            conf = ScmConfigProperties.builder().service(nameArr).updateProperty(key, value).build();
        }
        if (type.equals(ConfigType.BY_NODE.getType())) {
            conf = ScmConfigProperties.builder().instance(nameArr).updateProperty(key, value).build();
        }
        return ScmSystem.Configuration.setConfigProperties(ss, conf);
    }
}
