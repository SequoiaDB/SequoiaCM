package com.sequoiacm.infrastructure.tool.element;

import java.util.Objects;

public class ScmNodeType {
    private ScmNodeTypeEnum typeEnum;
    private String jarNamePrefix;
    private String confTemplateNamePrefix = "spring-app";
    // 该属性用于提醒用户，新添加一个工具时，要向 ScmServerScriptEnum 类中添加新工具的脚本信息
    private ScmServerScriptEnum serverScriptEnum;

    // 创建该节点时，是否需要生成Hystrix配置
    private boolean isNeedHystrixConf = true;

    public ScmNodeType(ScmNodeTypeEnum scmNodeTypeEnum, ScmServerScriptEnum serverScriptEnum) {
        this(scmNodeTypeEnum, serverScriptEnum, true);
    }

    public ScmNodeType(ScmNodeTypeEnum scmNodeTypeEnum, ScmServerScriptEnum serverScriptEnum,
            boolean isNeedHystrixConf) {
        this.typeEnum = scmNodeTypeEnum;
        this.jarNamePrefix = scmNodeTypeEnum.getJarNamePrefix();
        this.serverScriptEnum = serverScriptEnum;
        this.isNeedHystrixConf = isNeedHystrixConf;
    }

    public ScmNodeType(ScmNodeTypeEnum scmNodeTypeEnum, ScmServerScriptEnum serverScriptEnum,
            String confTemplateNamePrefix) {
        this(scmNodeTypeEnum, serverScriptEnum);
        this.confTemplateNamePrefix = confTemplateNamePrefix;
    }

    public String getType() {
        return typeEnum.getTypeNum();
    }

    public String getName() {
        return typeEnum.getName();
    }

    public ScmNodeTypeEnum getTypeEnum() {
        return typeEnum;
    }

    public String getJarNamePrefix() {
        return jarNamePrefix;
    }

    public String getConfTemplateNamePrefix() {
        return confTemplateNamePrefix;
    }

    public String getUpperName() {
        return getName().toUpperCase();
    }

    public boolean isNeedHystrixConf() {
        return isNeedHystrixConf;
    }

    public String getServiceDirName() {
        return serverScriptEnum.getDirName();
    }

    @Override
    public String toString() {
        return this.getType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ScmNodeType that = (ScmNodeType) o;
        return isNeedHystrixConf == that.isNeedHystrixConf && typeEnum == that.typeEnum
                && Objects.equals(jarNamePrefix, that.jarNamePrefix)
                && Objects.equals(confTemplateNamePrefix, that.confTemplateNamePrefix)
                && serverScriptEnum == that.serverScriptEnum;
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeEnum, jarNamePrefix, confTemplateNamePrefix, serverScriptEnum,
                isNeedHystrixConf);
    }
}
