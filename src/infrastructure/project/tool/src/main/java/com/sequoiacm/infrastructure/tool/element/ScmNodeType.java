package com.sequoiacm.infrastructure.tool.element;

public class ScmNodeType {
    private String type;
    private String name;
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
        this.type = scmNodeTypeEnum.getTypeNum();
        this.name = scmNodeTypeEnum.getName();
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
        return type;
    }

    public String getName() {
        return name;
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

    @Override
    public String toString() {
        return this.getType();
    }
}
