package com.sequoiacm.infrastructure.monitor.endpoint;

import com.sequoiacm.infrastructure.monitor.model.ScmConnectionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.List;

public class ScmTomcatConnectInfoEndpoint extends ScmAbstractConnectionInfoEndpoint {

    private static final Logger logger = LoggerFactory
            .getLogger(ScmTomcatConnectInfoEndpoint.class);

    private static final String ATTRIBUTE_CONNECTION_COUNT = "connectionCount";

    private ObjectName objectName;

    private MBeanServer mBeanServer;

    private static final String OBJECT_NAME_FORMAT = "Tomcat:type=ThreadPool,name=\"http-nio-%s\"";

    public ScmTomcatConnectInfoEndpoint(String serverPort) {
        try {
            this.objectName = new ObjectName(String.format(OBJECT_NAME_FORMAT, serverPort));
            this.mBeanServer = queryMbeanServer();
        }
        catch (MalformedObjectNameException e) {
            // should never happen
            logger.error("failed to init ScmTomcatConnectInfoEndpoint", e);
        }
    }

    private MBeanServer queryMbeanServer() {
        List<MBeanServer> mBeanServers = MBeanServerFactory.findMBeanServer(null);
        if (!mBeanServers.isEmpty()) {
            return mBeanServers.get(0);
        }

        return ManagementFactory.getPlatformMBeanServer();
    }

    @Override
    public ScmConnectionInfo invoke() {
        ScmConnectionInfo scmConnectionInfo = new ScmConnectionInfo();
        try {
            Long connectionCount = (Long) this.mBeanServer.getAttribute(this.objectName,
                    ATTRIBUTE_CONNECTION_COUNT);
            scmConnectionInfo.setConnectionCount(connectionCount);
        }
        catch (Exception e) {
           // should never happen
           logger.error("failed to get connection info", e);
           return scmConnectionInfo;
        }
        return scmConnectionInfo;
    }
}
