package com.sequoiacm.infrastructure.tool.element;

import java.util.ArrayList;
import java.util.List;

public enum ScmServerScriptEnum {
    // 枚举类型根据每个节点工具的 list 命令输出定
    // CONTENT-SERVER(15000) (31890) => CONTENTSERVER
    CONTENTSERVER("sequoiacm-content", "CONTENT-SERVER", "scmctl.sh"),
    GATEWAY("sequoiacm-cloud", "GATEWAY", "scmcloudctl.sh"),
    AUTHSERVER("sequoiacm-cloud", "AUTH-SERVER", "scmcloudctl.sh"),
    SERVICECENTER("sequoiacm-cloud", "SERVICE-CENTER", "scmcloudctl.sh"),
    ADMINSERVER("sequoiacm-cloud", "ADMIN-SERVER", "scmcloudctl.sh"),
    SERVICETRACE("sequoiacm-cloud", "SERVICE-TRACE", "scmcloudctl.sh"),
    CONFIGSERVER("sequoiacm-config", "CONFIG-SERVER", "confctl.sh"),
    SCHEDULESERVER("sequoiacm-schedule", "SCHEDULE-SERVER", "schctl.sh"),
    FULLTEXTSERVER("sequoiacm-fulltext", "FULLTEXT-SERVER", "ftctl.sh"),
    MQSERVER("sequoiacm-mq", "MQ-SERVER", "mqctl.sh"),
    OMSERVER("sequoiacm-om", "OM-SERVER", "omctl.sh"),
    S3SERVER("sequoiacm-s3", "S3-SERVER", "s3ctl.sh"),
    ZOOKEEPER("zookeeper", "ZOOKEEPER", "zkServer.sh");

    private final String dirName;
    private final String type;
    private final String shellName;

    ScmServerScriptEnum(String dirName, String type, String shellName) {
        this.dirName = dirName;
        this.type = type;
        this.shellName = shellName;
    }

    public String getDirName() {
        return dirName;
    }

    public String getType() {
        return type;
    }

    public String getShellName() {
        return shellName;
    }

    public static String getShellNameByDirName(String dirName) {
        ScmServerScriptEnum[] enums = ScmServerScriptEnum.values();
        for (ScmServerScriptEnum e : enums) {
            if (e.getDirName().equals(dirName)) {
                return e.getShellName();
            }
        }
        return null;
    }

    public static List<String> getAllType() {
        List<String> typeList = new ArrayList<>();
        ScmServerScriptEnum[] enums = ScmServerScriptEnum.values();
        for (ScmServerScriptEnum e : enums) {
            typeList.add(e.getType());
        }
        return typeList;
    }

    public static ScmServerScriptEnum getEnumByType(String type) {
        ScmServerScriptEnum[] enums = ScmServerScriptEnum.values();
        for (ScmServerScriptEnum e : enums) {
            if (type.toUpperCase().equals(e.getType())) {
                return e;
            }
        }
        return null;
    }

    public static ScmServerScriptEnum getEnumByDirName(String dirName) {
        // zookeeper 的目录名带有版本号，版本号是变化的，所以只需目录名有包含关系即可
        if (dirName.contains(ZOOKEEPER.getDirName())) {
            return ZOOKEEPER;
        }
        ScmServerScriptEnum[] enums = ScmServerScriptEnum.values();
        for (ScmServerScriptEnum e : enums) {
            if (dirName.equals(e.getDirName())) {
                return e;
            }
        }
        return null;
    }
}
