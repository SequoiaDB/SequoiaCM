package com.sequoiacm.deploy.installer;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.sequoiacm.deploy.common.CommonUtils;
import com.sequoiacm.deploy.config.CommonConfig;
import com.sequoiacm.deploy.module.InstallPackType;

public class ServicesInstallPackManager {
    private String servicesInstallPackPath;
    private Map<InstallPackType, FilenameFilter> installPackFileFilters;

    private static volatile ServicesInstallPackManager instance;

    public static ServicesInstallPackManager getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (ServicesInstallPackManager.class) {
            if (instance != null) {
                return instance;
            }
            instance = new ServicesInstallPackManager();
            return instance;
        }
    }

    private ServicesInstallPackManager() {
        CommonConfig commonConfig = CommonConfig.getInstance();
        String servicesInstallPackPath = commonConfig.getInstallPackPath();
        this.servicesInstallPackPath = servicesInstallPackPath;
        installPackFileFilters = new HashMap<>();

        for (final InstallPackType installPackType : InstallPackType.values()) {
            FilenameFilter installPackFileFilter = new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {
                    return Pattern.matches(installPackType.getPackNameRegexp(), name);
                }
            };
            installPackFileFilters.put(installPackType, installPackFileFilter);
        }
    }

    public File getServicePack(InstallPackType type) {
        File serviceInstallPackDir = new File(servicesInstallPackPath);
        FilenameFilter filter = installPackFileFilters.get(type);
        CommonUtils.assertTrue(filter != null, "unknown type:" + type);
        File[] files = serviceInstallPackDir.listFiles(filter);
        if (files == null || files.length <= 0) {
            throw new IllegalArgumentException("install pack not found:" + type);
        }

        if (files.length == 1) {
            return files[0];
        }

        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o2.getName().compareTo(o1.getName());
            }
        });

        return files[0];
    }
}