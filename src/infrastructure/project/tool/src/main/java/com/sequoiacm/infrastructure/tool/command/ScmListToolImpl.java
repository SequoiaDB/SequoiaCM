package com.sequoiacm.infrastructure.tool.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.element.ScmNodeInfo;
import com.sequoiacm.infrastructure.tool.element.ScmNodeInfoDetail;
import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.operator.ScmServiceNodeOperator;
import com.sequoiacm.infrastructure.tool.operator.ScmServiceNodeOperatorGroup;

public class ScmListToolImpl extends ScmTool {
    private final ScmServiceNodeOperatorGroup nodeOperators;
    private final String installPath;
    private Options options;
    private ScmHelpGenerator hp;

    private final String OPT_VALUE_LOCAL = "local";
    private final String OPT_VALUE_RUN = "run";

    public ScmListToolImpl(List<ScmServiceNodeOperator> nodeOperatorList) throws ScmToolsException {
        super("list");
        this.nodeOperators = new ScmServiceNodeOperatorGroup(nodeOperatorList);
        this.installPath = ScmHelper
                .getAbsolutePathFromTool(ScmHelper.getPwd() + File.separator + "..");
        nodeOperators.init(installPath);

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
        // TODO 日志
        CommandLine commandLine = ScmCommandUtil.parseArgs(args, options);
        boolean printRunningOnly = true;
        if (commandLine.hasOption(ScmCommandUtil.OPT_SHORT_MODE)) {
            String modeValue = commandLine.getOptionValue(ScmCommandUtil.OPT_SHORT_MODE);
            if (modeValue.equals(OPT_VALUE_LOCAL)) {
                printRunningOnly = false;
            }
            else if (!modeValue.equals(OPT_VALUE_RUN)) {
                throw new ScmToolsException("Unknown mode:" + modeValue,
                        ScmBaseExitCode.INVALID_ARG);
            }
        }

        List<ScmNodeInfo> nodeList = new ArrayList<>();
        List<String> pidList = new ArrayList<>();

        if (commandLine.hasOption(ScmCommandUtil.OPT_SHORT_PORT)) {
            String portStr = commandLine.getOptionValue(ScmCommandUtil.OPT_SHORT_PORT);
            int port = ScmCommon.convertStrToInt(portStr);
            ScmNodeInfoDetail node = nodeOperators.getNodeDetail(port);
            if (node != null) {
                if (node.getPid() != ScmNodeInfoDetail.NOT_RUNNING) {
                    nodeList.add(node.getNodeInfo());
                    pidList.add(node.getPid() + "");
                }
                else if (!printRunningOnly) {
                    nodeList.add(node.getNodeInfo());
                    pidList.add("-");
                }
            }
        }
        else {
            List<ScmNodeInfoDetail> allNodeDetail = nodeOperators.getAllNodeDetail();
            for (ScmNodeInfoDetail nodeInfoDetail : allNodeDetail) {
                if (nodeInfoDetail.getPid() != ScmNodeInfoDetail.NOT_RUNNING) {
                    nodeList.add(nodeInfoDetail.getNodeInfo());
                    pidList.add(nodeInfoDetail.getPid() + "");
                }
                else if (!printRunningOnly) {
                    nodeList.add(nodeInfoDetail.getNodeInfo());
                    pidList.add("-");
                }
            }
        }
        if (commandLine.hasOption(ScmCommandUtil.OPT_SHORT_LONG)) {
            for (int i = 0; i < pidList.size(); i++) {
                String propPath = nodeList.get(i).getConfPath() + File.separator + "application.properties";
                System.out.println(nodeList.get(i).getNodeType().getUpperName() + "("
                        + nodeList.get(i).getPort() + ")" + " (" + pidList.get(i) + ") "
                        + propPath);
            }
        }
        else {
            for (int i = 0; i < pidList.size(); i++) {
                System.out.println(nodeList.get(i).getNodeType().getUpperName() + "("
                        + nodeList.get(i).getPort() + ")" + " (" + pidList.get(i) + ")");
            }
        }
        System.out.println("Total:" + pidList.size());
        if (pidList.size() == 0) {
            throw new ScmToolsException(ScmBaseExitCode.EMPTY_OUT);
        }
    }

    @Override
    public void printHelp(boolean isFullHelp) {
        hp.printHelp(isFullHelp);
    }
}
