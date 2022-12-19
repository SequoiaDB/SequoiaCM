package com.sequoiacm.config.tools.command;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.element.ScmUpdateConfResult;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.util.Strings;
import com.sequoiacm.config.tools.common.ConfigType;
import com.sequoiacm.config.tools.common.ScmConfigCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public class ScmDeleteConfigImpl extends AbstractScmRefreshConfig {

    private Logger logger = LoggerFactory.getLogger(ScmDeleteConfigImpl.class);

    public ScmDeleteConfigImpl() throws ScmToolsException {
        super("deleteconfig");
    }

    @Override
    public void operation() throws ScmToolsException {
        ScmSession ss = null;
        try {
            ss = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION,
                    new ScmConfigOption(ScmConfigCommandUtil.parseListUrls(gatewayUrl), username,
                            password));
            ScmUpdateConfResultSet resultSet = deleteConfigProp(ss, type, name, config,
                    acceptUnknownConfig);
            List<ScmUpdateConfResult> failures = resultSet.getFailures();
            List<ScmUpdateConfResult> successes = resultSet.getSuccesses();
            if (failures.size() > 0) {
                for (ScmUpdateConfResult result : failures) {
                    System.out.println("delete config failed: serviceName="
                            + result.getServiceName() + ", instance=" + result.getInstance()
                            + ", errorMessage=" + result.getErrorMessage());
                    logger.error(
                            "delete config failed: serviceName={},nodeName={},config={},errorMessage={}",
                            result.getServiceName(), result.getInstance(), config,
                            result.getErrorMessage());
                }
            }
            if (successes.size() > 0) {
                for (ScmUpdateConfResult result : successes) {
                    System.out.println("delete config success: serviceName="
                            + result.getServiceName() + ", instance=" + result.getInstance());
                    logger.info("delete config success: serviceName={},nodeName={},config={}",
                            result.getServiceName(), result.getInstance(), config);
                }
            }
            if (!resultSet.getRebootConf().isEmpty()) {
                System.out.println("config '" + Strings.join(resultSet.getRebootConf(), ",")
                        + "' require restart to take effect.");
                logger.info("config '" + Strings.join(resultSet.getRebootConf(), ",")
                        + "' require restart to take effect.");
            }
        }
        catch (Exception e) {
            logger.error("delete config failed: name={},config={}", name, config, e);
            ScmCommon.throwToolException("delete config failed", e);
        }
        finally {
            if (null != ss) {
                ss.close();
            }
        }
    }

    private ScmUpdateConfResultSet deleteConfigProp(ScmSession ss, Integer type, String name,
            String key, boolean acceptUnknownConfig) throws Exception {
        String[] nameArr = name.split(",");
        ScmConfigProperties conf = null;
        if (ConfigType.BY_SERVICE.getType().equals(type)) {
            conf = ScmConfigProperties.builder().service(nameArr).deleteProperty(key)
                    .acceptUnknownProperties(acceptUnknownConfig).build();
        }
        if (ConfigType.BY_NODE.getType().equals(type)) {
            conf = ScmConfigProperties.builder().instance(nameArr).deleteProperty(key)
                    .acceptUnknownProperties(acceptUnknownConfig).build();
        }
        return ScmSystem.Configuration.setConfigProperties(ss, conf);
    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        hp.printHelp(isFullHelp);
    }

    @Override
    protected String configOptionExample() {
        return "--" + OPT_LONG_CONFIG + " configName";
    }
}
