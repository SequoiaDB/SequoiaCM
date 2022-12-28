package com.sequoiacm.deploy.installer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequoiacm.deploy.common.RefUtil;
import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.InstallPackType;

public class ServiceInstallerMgr {
    private Map<HostInfo, Map<InstallPackType, String>> installHistory = new HashMap<>();
    private Map<InstallPackType, ServiceInstaller> installersMap = new HashMap<>();

    private static volatile ServiceInstallerMgr instance;

    public static ServiceInstallerMgr getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (ServiceInstallerMgr.class) {
            if (instance != null) {
                return instance;
            }
            instance = new ServiceInstallerMgr();
            return instance;
        }
    }

    private ServiceInstallerMgr() {
        // 优先注册 @Installer 注解修饰的 ServiceInstaller 实现类
        List<ServiceInstaller> installers = RefUtil.initInstancesAnnotatedWith(Installer.class);
        for (ServiceInstaller installer : installers) {

            installersMap.put(installer.getType(), installer);
        }

        // 缺省使用 ServiceInstallerBase 实例
        for (InstallPackType installType : InstallPackType.values()) {
            if (installersMap.get(installType) == null) {
                installersMap.put(installType, new ServiceInstallerBase(installType));
            }
        }
    }

    public void install(HostInfo host, InstallPackType installPackType) throws Exception {
        Map<InstallPackType, String> historyOnHost = installHistory.get(host);
        if (historyOnHost == null) {
            historyOnHost = new HashMap<>();
            installHistory.put(host, historyOnHost);
        }

        ServiceInstaller installer = installersMap.get(installPackType);
        if (installer == null) {
            throw new IllegalArgumentException("no such installer:" + installPackType);
        }
        String installPath = installer.install(host);
        if (installPath != null) {
            historyOnHost.put(installPackType, installPath);
        }
    }

    public String getInstallPath(HostInfo host, InstallPackType type) {
        Map<InstallPackType, String> services = installHistory.get(host);
        if (services == null) {
            throw new IllegalArgumentException(
                    "did not install any service on host:" + host.getHostName());
        }

        String path = services.get(type);
        if (path == null) {
            throw new IllegalArgumentException(
                    "did not install service on host:" + host.getHostName() + ", service=" + type);
        }

        return path;
    }
}
