package com.sequoiacm.tools.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.element.ScmNodeInfoDetail;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.common.ScmContentCommandUtil;
import com.sequoiacm.tools.common.ScmContentCommon;
import com.sequoiacm.tools.common.ScmContentServerNodeOperator;
import com.sequoiacm.tools.exception.ScmExitCode;

public class ScmListToolImpl extends ScmTool {
    private final String installPath;
    private final ScmContentServerNodeOperator operator;
    private Options options;
    private ScmHelpGenerator hp;

    private final String OPT_VALUE_LOCAL = "local";
    private final String OPT_VALUE_RUN = "run";

    public ScmListToolImpl() throws ScmToolsException {
        super("list");
        this.installPath = ScmHelper.getAbsolutePathFromTool(
                ScmHelper.getPwd() + File.separator + ".." + File.separator + "..");
        operator = new ScmContentServerNodeOperator();
        operator.init(installPath);
        options = new Options();
        hp = new ScmHelpGenerator();
        options.addOption(hp.createOpt(ScmCommandUtil.OPT_SHORT_PORT, ScmCommandUtil.OPT_LONG_PORT,
                "node port", false, true, false));
        options.addOption(hp.createOpt(ScmCommandUtil.OPT_SHORT_MODE, ScmCommandUtil.OPT_LONG_MODE,
                "list mode, 'run' or 'local', default:run.",
                false, true, false));
        options.addOption(hp.createOpt(ScmCommandUtil.OPT_SHORT_LONG, ScmCommandUtil.OPT_LONG_LONG,
                "show long style", false, false, true));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine commandLine = ScmContentCommandUtil.parseArgs(args, options);

        boolean printRunningOnly = true;
        if (commandLine.hasOption(ScmCommandUtil.OPT_SHORT_MODE)) {
            if (commandLine.getOptionValue(ScmCommandUtil.OPT_SHORT_MODE).equals(OPT_VALUE_LOCAL)) {
                printRunningOnly = false;
            }
            else if (!commandLine.getOptionValue(ScmCommandUtil.OPT_SHORT_MODE)
                    .equals(OPT_VALUE_RUN)) {
                throw new ScmToolsException(
                        "Unknow mode:" + commandLine.getOptionValue(ScmCommandUtil.OPT_SHORT_MODE),
                        ScmExitCode.INVALID_ARG);
            }
        }

        List<String> portList = new ArrayList<>();
        List<String> pidList = new ArrayList<>();
        List<String> confList = new ArrayList<>();

        if (commandLine.hasOption(ScmCommandUtil.OPT_SHORT_PORT)) {
            String portStr = commandLine.getOptionValue(ScmCommandUtil.OPT_SHORT_PORT);
            int port = ScmContentCommon.convertStrToInt(portStr);
            ScmNodeInfoDetail node = operator.getNodeInfoDetail(port);
            if (node != null) {
                int pid = node.getPid();
                if (pid != ScmNodeInfoDetail.NOT_RUNNING) {
                    portList.add(port + "");
                    pidList.add(pid + "");
                    confList.add(node.getNodeInfo().getConfPath());
                }
                else if (!printRunningOnly) {
                    portList.add(port + "");
                    pidList.add("-");
                    confList.add(node.getNodeInfo().getConfPath());
                }
            }
        }
        else {
            List<ScmNodeInfoDetail> allNode = operator.getAllNodeInfoDetail();
            for (ScmNodeInfoDetail node : allNode) {
                int pid = node.getPid();
                if (pid != ScmNodeInfoDetail.NOT_RUNNING) {
                    portList.add(node.getNodeInfo().getPort() + "");
                    pidList.add(pid + "");
                    confList.add(node.getNodeInfo().getConfPath());
                }
                else if (!printRunningOnly) {
                    portList.add(node.getNodeInfo().getPort() + "");
                    pidList.add("-");
                    confList.add(node.getNodeInfo().getConfPath());
                }
            }
        }

        if (commandLine.hasOption(ScmCommandUtil.OPT_SHORT_LONG)) {
            for (int i = 0; i < pidList.size(); i++) {
                String propPath = confList.get(i) + File.separator + "application.properties";
                System.out.println("CONTENT-SERVER(" + portList.get(i) + ") (" + pidList.get(i)
                        + ") " + propPath);
            }
        }
        else {
            for (int i = 0; i < pidList.size(); i++) {
                System.out.println(
                        "CONTENT-SERVER(" + portList.get(i) + ") (" + pidList.get(i) + ")");
            }
        }
        System.out.println("Total:" + pidList.size());
        if (pidList.size() == 0) {
            throw new ScmToolsException(ScmExitCode.EMPTY_OUT);
        }
    }

    @Override
    public void printHelp(boolean isFullHelp) {
        hp.printHelp(isFullHelp);
    }

}
