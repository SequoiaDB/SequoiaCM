package com.sequoiacm.infrastructure.monitor.endpoint;

import com.sequoiacm.infrastructure.monitor.model.ScmProcessInfo;
import org.springframework.boot.actuate.endpoint.Endpoint;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class ScmProcessInfoEndpoint implements Endpoint<ScmProcessInfo> {

    private static final List<String> OPERATING_SYSTEM_BEAN_CLASS_NAMES = Arrays.asList(
            "com.sun.management.OperatingSystemMXBean", // HotSpot
            "com.ibm.lang.management.OperatingSystemMXBean" // J9
    );

    private final OperatingSystemMXBean operatingSystemBean;

    private final Class<?> operatingSystemBeanClass;

    private final Method systemCpuUsage;

    private final Method processCpuUsage;

    public ScmProcessInfoEndpoint() {
        this.operatingSystemBean = ManagementFactory.getOperatingSystemMXBean();
        this.operatingSystemBeanClass = getFirstClassFound(OPERATING_SYSTEM_BEAN_CLASS_NAMES);
        this.systemCpuUsage = detectMethod("getSystemCpuLoad");
        this.processCpuUsage = detectMethod("getProcessCpuLoad");
    }

    @Override
    public String getId() {
        return "process_info";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean isSensitive() {
        return false;
    }

    @Override
    public ScmProcessInfo invoke() {
        String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        double systemCpuUsage = invokeMethod(this.systemCpuUsage);
        double processCpuUsage = invokeMethod(this.processCpuUsage);
        int cpus = Runtime.getRuntime().availableProcessors();
        ScmProcessInfo scmProcessInfo  = new ScmProcessInfo();
        scmProcessInfo.setPid(pid);
        scmProcessInfo.setUptime(uptime);
        scmProcessInfo.setProcessCpuUsage(processCpuUsage);
        scmProcessInfo.setSystemCpuUsage(systemCpuUsage);
        scmProcessInfo.setCpus(cpus);
        return scmProcessInfo;
    }

    private double invokeMethod(Method method) {
        try {
            return method != null ? (double) method.invoke(operatingSystemBean) : Double.NaN;
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            return Double.NaN;
        }
    }

    private Method detectMethod(String name) {
        if (operatingSystemBeanClass == null) {
            return null;
        }
        try {
            // ensure the Bean we have is actually an instance of the interface
            operatingSystemBeanClass.cast(operatingSystemBean);
            return operatingSystemBeanClass.getDeclaredMethod(name);
        }
        catch (ClassCastException | NoSuchMethodException | SecurityException e) {
            return null;
        }
    }

    private Class<?> getFirstClassFound(List<String> classNames) {
        for (String className : classNames) {
            try {
                return Class.forName(className);
            }
            catch (ClassNotFoundException ignore) {
            }
        }
        return null;
    }
}
