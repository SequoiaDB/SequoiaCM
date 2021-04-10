package com.sequoiacm.infrastructure.tool.element;

import java.util.*;

import com.sequoiacm.infrastructure.tool.exception.ScmExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public class ScmNodeRequiredParamGroup {
    private List<ScmNodeRequiredParam> requiredParams = new ArrayList<>();

    ScmNodeRequiredParamGroup() {
    }

    public void addParam(ScmNodeRequiredParam param) {
        requiredParams.add(param);
    }

    public void check(Properties clientKeyValue) throws ScmToolsException {
        Set<String> clientKeys = clientKeyValue.stringPropertyNames();
        Set<String> lostKeys = new HashSet<>(); // createnode时缺少的参数集合

        for (ScmNodeRequiredParam param : requiredParams) {
            if (!param.isPrefixKey()) {
                // param 是一个完整 key，直接查询命令行是否携带该 key
                if (!clientKeys.contains(param.getKey())) {
                    lostKeys.add(param.getKey());
                }
                continue;
            }
            // param 是一个 preKey（如 -Dk1）, 拿到它关联 key 的 value （如 v1）
            // 然后再校验命令行是否携带 -Dk1.v1
            String bindingKeyValue = clientKeyValue.getProperty(param.getBindingKey());
            if (bindingKeyValue == null) {
                lostKeys.add(param.getBindingKey());
                continue;
            }
            String completedKey = param.getKey() + bindingKeyValue;
            if (!clientKeys.contains(completedKey)) {
                lostKeys.add(completedKey);
            }
        }

        if (lostKeys.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (String lostKey : lostKeys) {
                sb.append("-D" + lostKey).append(", ");
            }
            throw new ScmToolsException(
                    "missing properties:key=" + sb.delete(sb.length() - 2, sb.length()).toString(),
                    ScmExitCode.INVALID_ARG);
        }
    }

    public void check(Map<String, String> clientKeyValue) throws ScmToolsException {
        Properties nodeConf = new Properties();
        for (Map.Entry<String, String> entry : clientKeyValue.entrySet()) {
            nodeConf.setProperty(entry.getKey(), entry.getValue());
        }
        check(nodeConf);
    }

    public List<String> getExample() {
        List<String> example = new ArrayList<>();
        for (ScmNodeRequiredParam param : requiredParams) {
            example.add(param.getExample());
        }
        return example;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private final ScmNodeRequiredParamGroup paramGroup;

        Builder() {
            this.paramGroup = new ScmNodeRequiredParamGroup();
        }

        public Builder addCloudParam() {
            paramGroup.addParam(
                    ScmNodeRequiredParam.keyParamInstance("eureka.instance.metadata-map.zone",
                            "-Deureka.instance.metadata-map.zone=zone1"));
            paramGroup.addParam(ScmNodeRequiredParam.keyParamInstance("eureka.client.region",
                    "-Deureka.client.region=beijing"));

            paramGroup.addParam(ScmNodeRequiredParam.preKeyParamInstance(
                    "eureka.client.availability-zones.",
                    "-Deureka.client.availability-zones.beijing=zone1", "eureka.client.region"));
            paramGroup
                    .addParam(ScmNodeRequiredParam.preKeyParamInstance("eureka.client.service-url.",
                            "-Deureka.client.service-url.zone1=http://localhost:8800/eureka/",
                            "eureka.instance.metadata-map.zone"));
            return this;
        }

        public Builder addSdbParam() {
            paramGroup.addParam(ScmNodeRequiredParam.keyParamInstance("scm.store.sequoiadb.urls",
                    "-Dscm.store.sequoiadb.urls=localhost:11810"));
            paramGroup.addParam(ScmNodeRequiredParam.keyParamInstance(
                    "scm.store.sequoiadb.username", "-Dscm.store.sequoiadb.username=sdbadmin"));
            paramGroup
                    .addParam(ScmNodeRequiredParam.keyParamInstance("scm.store.sequoiadb.password",
                            "-Dscm.store.sequoiadb.password=/home/scm/sdb.passwd"));

            return this;
        }

        public Builder addZkParam() {
            paramGroup.addParam(ScmNodeRequiredParam.keyParamInstance("scm.zookeeper.urls",
                    "-Dscm.zookeeper.urls=localhost:2181"));

            return this;
        }

        public Builder addServerPortParam(int examplePort) {
            paramGroup.addParam(ScmNodeRequiredParam.keyParamInstance("server.port",
                    "-Dserver.port=" + examplePort));
            return this;
        }

        public Builder addParam(ScmNodeRequiredParam p) {
            paramGroup.addParam(p);
            return this;
        }

        public ScmNodeRequiredParamGroup get() {
            return paramGroup;
        }
    }
}