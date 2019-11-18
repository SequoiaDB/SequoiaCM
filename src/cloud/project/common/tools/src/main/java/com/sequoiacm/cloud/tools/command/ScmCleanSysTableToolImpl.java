package com.sequoiacm.cloud.tools.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.sequoiacm.cloud.tools.ScmAdmin;
import com.sequoiacm.cloud.tools.common.ScmCommandUtil;
import com.sequoiacm.cloud.tools.common.ScmHelpGenerator;
import com.sequoiacm.cloud.tools.common.ScmSysTableCleaner;
import com.sequoiacm.cloud.tools.common.ScmSysTableProcessorFactory;
import com.sequoiacm.cloud.tools.element.ScmNodeType;
import com.sequoiacm.cloud.tools.exception.ScmToolsException;

public class ScmCleanSysTableToolImpl implements ScmTool {
    private final String OPT_LONG_SDB_URL = "url";
    private final String OPT_LONG_SDB_USER = "user";
    private final String OPT_LONG_SDB_PWD_FILE = "password-file";
    private Options ops;
    private ScmHelpGenerator hp;

    public ScmCleanSysTableToolImpl() throws ScmToolsException {
        ops = new Options();
        hp = new ScmHelpGenerator();

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
        ScmAdmin.checkHelpArgs(args);
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        String nodeTypeStr = cl.getOptionValue(ScmCommandUtil.OPT_SHORT_NODE_TYPE);

        ScmNodeType nodeType = ScmNodeType.getNodeTypeByStr(nodeTypeStr);

        String sdbUrl = cl.getOptionValue(OPT_LONG_SDB_URL);
        String sdbUser = cl.hasOption(OPT_LONG_SDB_USER) ? cl.getOptionValue(OPT_LONG_SDB_USER)
                : "";
        String sdbPwdFile = cl.hasOption(OPT_LONG_SDB_PWD_FILE)
                ? cl.getOptionValue(OPT_LONG_SDB_PWD_FILE)
                : "";

        ScmSysTableCleaner sysTableCleaner = ScmSysTableProcessorFactory
                .getSysTableCleaner(nodeType, sdbUrl, sdbUser, sdbPwdFile);
        if (sysTableCleaner != null) {
            sysTableCleaner.clean();
        }
    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        hp.printHelp(isFullHelp);
    }
}
