package com.sequoiacm.infrastructure.tool.command;

import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.element.ScmNodeInfo;
import com.sequoiacm.infrastructure.tool.element.ScmNodeProcessInfo;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.exception.ScmExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.exec.ScmExecutorWrapper;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ScmListToolImpl extends ScmTool {
    private Options options;
    private ScmHelpGenerator hp;
    private ScmExecutorWrapper executor;
    private ScmNodeTypeList nodeTypes;

    public ScmListToolImpl(ScmNodeTypeList nodeTypes) throws ScmToolsException {
        super("list");
        this.nodeTypes = nodeTypes;
        options = new Options();
        hp = new ScmHelpGenerator();
        options.addOption(hp.createOpt("p", "port", "node port", false, true, false));
        options.addOption(hp.createOpt("m", "mode", "list mode, 'run' or 'local', default:run.",
                false, true, false));
        executor = new ScmExecutorWrapper(this.nodeTypes);
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        // TODO 日志
        CommandLine commandLine = ScmCommandUtil.parseArgs(args, options);
        Map<Integer, ScmNodeInfo> port2Node = executor.getAllNode();
        Map<String, ScmNodeProcessInfo> runningNodes = executor.getNodeStatus();
        boolean printRunningOnly = true;
        if (commandLine.hasOption("m")) {
            if (commandLine.getOptionValue("m").equals("local")) {
                printRunningOnly = false;
            }
            else if (!commandLine.getOptionValue("m").equals("run")) {
                throw new ScmToolsException("Unknow mode:" + commandLine.getOptionValue("m"),
                        ScmExitCode.INVALID_ARG);
            }
        }

        List<ScmNodeInfo> nodeList = new ArrayList<>();
        List<String> pidList = new ArrayList<>();

        if (commandLine.hasOption("p")) {
            String portStr = commandLine.getOptionValue("p");
            int port = ScmCommon.convertStrToInt(portStr);
            ScmNodeInfo node = port2Node.get(port);
            if (node != null) {
                String confPath = node.getConfPath();
                ScmNodeProcessInfo runningNodeInfo = runningNodes.get(confPath);
                if (runningNodeInfo != null) {
                    nodeList.add(node);
                    pidList.add(runningNodeInfo.getPid() + "");
                }
                else if (!printRunningOnly) {
                    nodeList.add(node);
                    pidList.add("-");
                }
            }
        }
        else {
            Iterator<Entry<Integer, ScmNodeInfo>> it = port2Node.entrySet().iterator();
            while (it.hasNext()) {
                Entry<Integer, ScmNodeInfo> entry = it.next();
                ScmNodeProcessInfo runningNode = runningNodes.get(entry.getValue().getConfPath());
                if (runningNode != null) {
                    Integer pidInteger = runningNode.getPid();
                    nodeList.add(entry.getValue());
                    pidList.add(pidInteger.toString());
                }
                else if (!printRunningOnly) {
                    nodeList.add(entry.getValue());
                    pidList.add("-");
                }
            }
        }
        for (int i = 0; i < pidList.size(); i++) {
            System.out.println(nodeList.get(i) + " (" + pidList.get(i) + ")");
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
