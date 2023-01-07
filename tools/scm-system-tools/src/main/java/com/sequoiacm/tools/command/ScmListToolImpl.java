package com.sequoiacm.tools.command;


import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.common.ScmToolsDefine;
import com.sequoiacm.infrastructure.tool.element.ScmNodeInfoDetail;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.operator.ScmServiceNodeOperator;
import com.sequoiacm.tools.common.ScmSysToolUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ScmListToolImpl extends ScmTool {
    private static Logger logger = LoggerFactory.getLogger(ScmListToolImpl.class);
    private ScmHelpGenerator hp;
    private Options options;
    private ScmNodeTypeList allNodeTypes;
    private final String OPT_VALUE_LOCAL = "local";
    private final String OPT_VALUE_RUN = "run";

    private Map<ScmNodeType, ScmServiceNodeOperator> allOperators;

    private String installPath;

    public ScmListToolImpl() throws ScmToolsException {
        super("list");

        options = new Options();
        hp = new ScmHelpGenerator();
        allNodeTypes = ScmSysToolUtil.getAllNodeTypes();
        ScmCommandUtil.addTypeOptionForStartOrStopWithOutTypeNum(allNodeTypes, options, hp);

        options.addOption(hp.createOpt(ScmCommandUtil.OPT_SHORT_MODE, ScmCommandUtil.OPT_LONG_MODE,
                "list mode, 'run' or 'local', default:run.",
                false, true, false));
        options.addOption(hp.createOpt(ScmCommandUtil.OPT_SHORT_LONG, ScmCommandUtil.OPT_LONG_LONG,
                "show long style", false, false, true));

        installPath = Paths.get(ScmHelper.getPwd() + File.separator + "..").normalize().toString();
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        ScmHelper.configToolsLog(ScmToolsDefine.FILE_NAME.LOGBACK);
        allOperators = ScmSysToolUtil.initOperators(ScmSysToolUtil.getAllNodeTypes(), installPath);

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

        List<ScmServiceNodeOperator> listOperators = new ArrayList();
        String type = ScmToolsDefine.NODE_TYPE.ALL_STR;
        if (commandLine.getOptionValue(ScmCommandUtil.OPT_SHORT_NODE_TYPE) != null) {
            type = commandLine.getOptionValue(ScmCommandUtil.OPT_SHORT_NODE_TYPE).trim();
        }
        if (type.equals(ScmToolsDefine.NODE_TYPE.ALL_STR)) {
            listOperators.addAll(allOperators.values());
        }
        else {
            ScmNodeType nodeType = allNodeTypes.getNodeTypeByStr(type);
            listOperators.add(allOperators.get(nodeType));
        }

        List<ScmNodeInfoDetail> printNodes = new ArrayList<>();
        for (ScmServiceNodeOperator operator : listOperators) {
            List<ScmNodeInfoDetail> allNodes = operator.getAllNodeInfoDetail();
            for (ScmNodeInfoDetail nodeInfoDetail : allNodes){
                if (nodeInfoDetail.getPid() != ScmNodeInfoDetail.NOT_RUNNING){
                    printNodes.add(nodeInfoDetail);
                }
                else if (!printRunningOnly) {
                    printNodes.add(nodeInfoDetail);
                }
            }
        }

        if (commandLine.hasOption(ScmCommandUtil.OPT_SHORT_PORT)) {
            for (ScmNodeInfoDetail nodeInfo : printNodes) {
                String propPath = nodeInfo.getNodeInfo().getConfPath() + File.separator + "application.properties";
                String pidStr = "-";
                if (nodeInfo.getPid() != ScmNodeInfoDetail.NOT_RUNNING){
                    pidStr = "" + nodeInfo.getPid();
                }
                System.out.println(nodeInfo.getNodeInfo().getNodeType().getUpperName() + "("
                        + nodeInfo.getNodeInfo().getPort() + ")" + " (" + pidStr + ") "
                        + propPath);
            }
        }
        else {
            for (ScmNodeInfoDetail nodeInfo : printNodes) {
                String pidStr = "-";
                if (nodeInfo.getPid() != ScmNodeInfoDetail.NOT_RUNNING){
                    pidStr = "" + nodeInfo.getPid();
                }
                System.out.println(nodeInfo.getNodeInfo().getNodeType().getUpperName() + "("
                        + nodeInfo.getNodeInfo().getPort() + ")" + " (" + pidStr + ")");
            }
        }
        System.out.println("Total:" + printNodes.size());
        if (printNodes.size() == 0) {
            throw new ScmToolsException(ScmBaseExitCode.EMPTY_OUT);
        }
    }

    @Override
    public void printHelp(boolean isFullHelp) {
        hp.printHelp(isFullHelp);
    }
}
