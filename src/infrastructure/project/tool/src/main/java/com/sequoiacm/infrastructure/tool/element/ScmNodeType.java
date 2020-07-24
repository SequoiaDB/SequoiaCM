package com.sequoiacm.infrastructure.tool.element;


public class ScmNodeType {
    private String type;
    private String name;
    private String jarNamePrefix;
    private String confTemplateNamePrefix = "spring-app";

    public ScmNodeType(String type, String name, String jarNamePrefix) {
        this.type = type;
        this.name = name;
        this.jarNamePrefix = jarNamePrefix;
    }

    public ScmNodeType(String type, String name, String jarNamePrefix,
                        String confTemplateNamePrefix) {
        this(type, name, jarNamePrefix);
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

    @Override
    public String toString() {
        return this.getType();
    }
}
