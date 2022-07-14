package com.sequoiacm.contentserver.datasourcemgr;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.datasource.DatasourcePlugin;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.contentserver.common.ScmClassLoader;
import com.sequoiacm.infrastructure.slowlog.SlowLogManager;

public class DataSourcePluginMgr {
    private static DataSourcePluginMgr instance = new DataSourcePluginMgr();
    DatasourcePlugin plugin = null;

    private DataSourcePluginMgr() {
    }

    public static DataSourcePluginMgr getInstance() {
        return instance;
    }

    public DatasourcePlugin initPlugin(String pluginDir, String className) throws ScmServerException {
        if (null != plugin) {
            return plugin;
        }

        if (null != pluginDir) {
            ScmClassLoader.getInstance().addJar(pluginDir);
            String packageName = className.substring(0, className.lastIndexOf("."));
            SlowLogManager.addPackage(packageName, ScmClassLoader.getInstance().getInnerLoader());
        }

        Class<?> clazz = ScmClassLoader.getInstance().loadClass(className);
        Object tmpObj = null;
        try {
            tmpObj = clazz.newInstance();
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "newInstance failed:className=" + className, e);
        }

        plugin = (DatasourcePlugin)tmpObj;
        return plugin;
    }

    public DatasourcePlugin getPlugin() {
        return plugin;
    }
}
