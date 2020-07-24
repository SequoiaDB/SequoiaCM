package com.sequoiacm.cloud.tools.command;

import com.sequoiacm.cloud.tools.exception.ScmExitCode;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.sequoiacm.cloud.tools.common.ScmSysTableCleaner;
import com.sequoiacm.cloud.tools.common.ScmSysTableProcessorFactory;

import java.util.List;

public class ScmCleanSysTableToolImpl extends ScmTool {
    private final String OPT_LONG_SDB_URL = "url";
    private final String OPT_LONG_SDB_USER = "user";
    private final String OPT_LONG_SDB_PWD_FILE = "password-file";
    private Options ops;
    private ScmHelpGenerator hp;
    private ScmNodeTypeList nodeTypes;


    public ScmCleanSysTableToolImpl(ScmNodeTypeList nodeTypes) throws ScmToolsException {
        super("cleansystable");
        ops = new Options();
        hp = new ScmHelpGenerator();
        this.nodeTypes = nodeTypes;

        StringBuilder typeOptDesc = new StringBuilder();
        typeOptDesc.append("specify node type, arg:[ 3 | 21 ], 3:auth-server,\r\n");
        typeOptDesc.append("21:admin-server");
        Option op = hp.createOpt(ScmCommandUtil.OPT_SHORT_NODE_TYPE,
                ScmCommandUtil.OPT_LONG_NODE_TYPE, typeOptDesc.toString(), true, true, false);
        ops.addOption(op);

        ops.addOption(hp.createOpt(null, OPT_LONG_SDB_URL, "sequoiadb url", true, true,
                false));
        ops.addOption(hp.createOpt(null, OPT_LONG_SDB_USER, "sequoiadb user", false,
                true, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_SDB_PWD_FILE,
                "sequoiadb password file path", false, true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        String nodeTypeStr = cl.getOptionValue(ScmCommandUtil.OPT_SHORT_NODE_TYPE);

        
        String sdbUrl = cl.getOptionValue(OPT_LONG_SDB_URL);
        String sdbUser = cl.hasOption(OPT_LONG_SDB_USER) ? cl.getOptionValue(OPT_LONG_SDB_USER)
                : "";
        String sdbPwdFile = cl.hasOption(OPT_LONG_SDB_PWD_FILE)
                ? cl.getOptionValue(OPT_LONG_SDB_PWD_FILE)
                : "";

        ScmNodeType nodeType = this.nodeTypes.getNodeTypeByStr(nodeTypeStr);
        ScmSysTableCleaner sysTableCleaner = ScmSysTableProcessorFactory
                .getSysTableCleaner(nodeType, sdbUrl, sdbUser, sdbPwdFile);
        if (sysTableCleaner == null) {
            throw new ScmToolsException("unknown type:" + nodeTypeStr, ScmExitCode.INVALID_ARG);
        }
        sysTableCleaner.clean();
    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        hp.printHelp(isFullHelp);
    }
}
