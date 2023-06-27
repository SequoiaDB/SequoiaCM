package com.sequoiacm.infrastructure.config.client.config;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;

import com.sequoiacm.infrastructure.config.util.ScmConfigPropsModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

@Component
public class ScmConfigPropsModifierFactory {
    private static final Logger logger = LoggerFactory
            .getLogger(ScmConfigPropsModifierFactory.class);
    private String configPropsPath;
    private final String myDir;

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${server.port}")
    private int serverPort;

    public ScmConfigPropsModifierFactory() throws ScmConfigException {
        myDir = getMyDir();
    }

    public ScmConfigPropsModifier createConfigPropsDao() throws ScmConfigException {
        if (configPropsPath == null) {
            this.configPropsPath = myDir + File.separator + ".." + File.separator + "conf"
                    + File.separator + applicationName + File.separator + serverPort
                    + File.separator + "application.properties";
        }
        return new ScmConfigPropsModifier(configPropsPath);
    }

    public void setConfigPropsPath(String configFileRelativePath) throws ScmConfigException {
        this.configPropsPath = myDir + File.separator + configFileRelativePath;
    }

    public String getMyDir() throws ScmConfigException {
        URL url = ScmConfClient.class.getProtectionDomain().getCodeSource().getLocation();
        String filePath = null;
        try {
            String jar = ".jar";
            filePath = URLDecoder.decode(url.getPath(), "utf-8");
            logger.info("jarDir={}", filePath);
            int endIdx = filePath.indexOf(jar);
            endIdx += jar.length();
            int startIdx = filePath.indexOf(":") + 1;
            if (-1 == startIdx || startIdx >= endIdx) {
                startIdx = 0;
            }

            filePath = filePath.substring(startIdx, endIdx);
            logger.info("jarDir={}", filePath);
        }
        catch (Exception e) {
            throw new ScmConfigException(ScmConfError.SYSTEM_ERROR,
                    "get path failed:className=" + ScmConfClient.class.toString(), e);
        }
        return new File(filePath).getAbsoluteFile().getParent();
    }

}
