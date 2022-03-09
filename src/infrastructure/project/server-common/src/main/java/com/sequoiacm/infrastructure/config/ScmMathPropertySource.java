package com.sequoiacm.infrastructure.config;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

/**
 * 提供在配置文件中对配置项进行数学计算的能力
 * 示例:
 *  1. management.port=${ScmMath.addInt(server.port, 1)} 将 management.port 设为服务端口加 1
 *  2. my.port=${ScmMath.addInt(server.port, management.port)} 将 my.port 设为 server.port + management.port
 */
public class ScmMathPropertySource extends PropertySource<Object> {

    private static final String PREFIX = "ScmMath.";

    private static final String OPERATION_ADD_INT = "addInt";

    private ConfigurableEnvironment environment;

    public ScmMathPropertySource(String name, ConfigurableEnvironment environment) {
        super(name);
        this.environment = environment;
    }

    @Override
    public Object getProperty(String name) {
        if (!name.startsWith(PREFIX)) {
            return null;
        }
        String operation = null;
        String param1 = null;
        String param2 = null;
        try {
            // ScmMath.add(server.port,1) => operation:add, param1:server.port, param2:1
            operation = name.substring(name.indexOf(".") + 1, name.indexOf("("));
            param1 = name.substring(name.indexOf("(") + 1, name.indexOf(",")).trim();
            param2 = name.substring(name.indexOf(",") + 1, name.lastIndexOf(")")).trim();
        }
        catch (Exception e) {
            throw new IllegalArgumentException("failed to parse ScmMath property:" + name);
        }
        return calculate(operation, param1, param2);
    }

    private Object calculate(String operation, String param1, String param2) {
        if (OPERATION_ADD_INT.equals(operation)) {
            return getIntValue(param1) + getIntValue(param2);
        }
        throw new IllegalArgumentException("unsupported calculation operation:" + operation);
    }

    private int getIntValue(String param) {
        Integer value = null;
        try {
            value = Integer.parseInt(param);
        }
        catch (Exception e) {

        }
        if (value == null) {
            String property = environment.getProperty(param);
            if (property == null) {
                throw new IllegalArgumentException("property is not exist:" + param);
            }
            return Integer.parseInt(property);
        }
        return value;
    }
}
