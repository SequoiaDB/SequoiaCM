package com.sequoiacm.daemon.common;

import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.util.List;

public class CommandUtils {
    public static void addTypeOptionForChStatus(Options options, ScmHelpGenerator hp,
            boolean isRequire) throws ScmToolsException {
        StringBuilder typeOptDesc = new StringBuilder();
        typeOptDesc.append("pecify node type,arg:[\n");

        typeOptDesc.append(" all\n");
        addType(typeOptDesc);
        typeOptDesc.append("]");
        Option op = hp.createOpt(DaemonDefine.OPT_SHORT_TYPE, DaemonDefine.OPT_LONG_TYPE,
                typeOptDesc.toString(), isRequire, true, false);
        options.addOption(op);
    }

    public static void addTypeOptionForAdd(Options options, ScmHelpGenerator hp, boolean isRequire)
            throws ScmToolsException {
        StringBuilder typeOptDesc = new StringBuilder();
        typeOptDesc.append("specify node type,arg:[\n");
        addType(typeOptDesc);
        typeOptDesc.append("]");
        Option op = hp.createOpt(DaemonDefine.OPT_SHORT_TYPE, DaemonDefine.OPT_LONG_TYPE,
                typeOptDesc.toString(), isRequire, true, false);
        options.addOption(op);
    }

    private static void addType(StringBuilder typeOptDesc) {
        List<String> typeList = ScmServerScriptEnum.getAllType();
        for (String type : typeList) {
            typeOptDesc.append(String.format(" %s\n", type));
        }
    }
}
