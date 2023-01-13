package com.sequoiacm.infrastructure.common;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

import java.io.File;
import java.io.IOException;

// listen to all scm nodes
public class ScmApplicationCloseListener implements ApplicationListener<ContextClosedEvent> {
    private Logger logger = LoggerFactory.getLogger(ScmApplicationCloseListener.class);
    private boolean closed = false;
    @Value("${server.tomcat.basedir}")
    private String tomcatBasedir;

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        if (closed) {
            return;
        }
        closed = true;
        File file = new File(tomcatBasedir);
        if (file.exists()) {
            try {
                logger.info("delete tomcat basedir " + tomcatBasedir);
                FileUtils.forceDelete(file);
            }
            catch (IOException e) {
                logger.warn("delete tomcat basedir " + tomcatBasedir + " failed", e);
            }
        }
    }
}
