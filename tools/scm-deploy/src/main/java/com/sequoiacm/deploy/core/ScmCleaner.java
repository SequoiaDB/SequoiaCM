package com.sequoiacm.deploy.core;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.deploy.cleaner.ServiceCleanerMgr;
import com.sequoiacm.deploy.common.SequoiadbTableInitializer;
import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.InstallPackType;
import com.sequoiacm.deploy.module.NodeInfo;
import com.sequoiacm.deploy.module.ServiceType;

public class ScmCleaner {
    private static final Logger logger = LoggerFactory.getLogger(ScmCleaner.class);

    private SequoiadbTableInitializer tableInitializer = SequoiadbTableInitializer.getInstance();

    private ServiceCleanerMgr cleanerMgr;

    private ScmPasswordFileSender passwordFileSender = ScmPasswordFileSender.getInstance();

    public ScmCleaner() throws Exception {
        cleanerMgr = ServiceCleanerMgr.getInstance();
    }

    public void clean(boolean dryRun) throws Exception {
        ScmDeployInfoMgr deployInfoMgr = ScmDeployInfoMgr.getInstance();
        List<CleanUnit> cleanUnits = new ArrayList<>();
        List<ServiceType> serviceTypes = ServiceType.getAllTyepSortByPriority();
        for (int i = serviceTypes.size() - 1; i >= 0; i--) {
            List<NodeInfo> nodes = deployInfoMgr.getNodesByServiceType(serviceTypes.get(i));
            if (nodes == null) {
                continue;
            }
            for (NodeInfo node : nodes) {
                CleanUnit c = new CleanUnit(node.getServiceType().getInstllPack(),
                        node.getHostName());
                if (!cleanUnits.contains(c)) {
                    cleanUnits.add(c);
                }
            }
        }

        // 2 for clean sdb and passwordfile
        int totalStep = cleanUnits.size() + 2;

        int currentStep = 0;
        logger.info("Cleaning service{}...({}/{})", dryRun ? "(Dry Run Mode)" : "", currentStep++,
                totalStep);
        for (CleanUnit c : cleanUnits) {
            logger.info("Cleaning service: removing {} on {} ({}/{})", c.getType(), c.getHost(),
                    currentStep++, totalStep);
            HostInfo hostInfo = deployInfoMgr.getHostInfoWithCheck(c.getHost());
            cleanerMgr.clean(hostInfo, c.getType(), dryRun);
        }

        logger.info("Cleaning service: uninitializing metasource and auditsource ({}/{})",
                currentStep++, totalStep);
        tableInitializer.doUninitialize(dryRun);

        logger.info("Cleaning service: removing password file ({}/{})", currentStep++, totalStep);
        List<HostInfo> hosts = deployInfoMgr.getHosts();
        for (HostInfo host : hosts) {
            passwordFileSender.cleanPasswordFile(host, dryRun);
        }
        logger.info("Clean service success");
    }
    //
    // public void displayCheckList() throws Exception {
    // ScmDeployInfoMgr deployInfoMgr = ScmDeployInfoMgr.getInstance();
    // logger.info("Clean Service Checklist:");
    // Map<HostInfo, List<InstallPackType>> services =
    // deployInfoMgr.getServiceOnHost();
    // for (Entry<HostInfo, List<InstallPackType>> entry : services.entrySet())
    // {
    // for (InstallPackType installPack : entry.getValue()) {
    // cleanerMgr.displayCleanCheckList(entry.getKey(), installPack);
    // }
    // passwordFileSender.displayCleanChecklist(entry.getKey());
    // }
    //
    // tableInitializer.displayUninitializeChecklist();
    // logger.info("Use option '" + ClusterSubCommand.OPT_CLEAN + "' to
    // confirm");
    // }

}

class CleanUnit {
    InstallPackType type;
    String host;

    public CleanUnit(InstallPackType type, String host) {
        super();
        this.type = type;
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public InstallPackType getType() {
        return type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CleanUnit other = (CleanUnit) obj;
        if (host == null) {
            if (other.host != null)
                return false;
        }
        else if (!host.equals(other.host))
            return false;
        if (type != other.type)
            return false;
        return true;
    }

}
