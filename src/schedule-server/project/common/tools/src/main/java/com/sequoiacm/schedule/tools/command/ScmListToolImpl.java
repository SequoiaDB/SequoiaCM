package com.sequoiacm.schedule.tools.command;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import com.sequoiacm.schedule.tools.SchCtl;
import com.sequoiacm.schedule.tools.common.ScmCommandUtil;
import com.sequoiacm.schedule.tools.common.ScmCommon;
import com.sequoiacm.schedule.tools.common.ScmHelpGenerator;
import com.sequoiacm.schedule.tools.element.ScmNodeInfo;
import com.sequoiacm.schedule.tools.element.ScmNodeProcessInfo;
import com.sequoiacm.schedule.tools.exception.ScmExitCode;
import com.sequoiacm.schedule.tools.exception.ScmToolsException;
import com.sequoiacm.schedule.tools.exec.ScmExecutorWrapper;

public class ScmListToolImpl implements ScmTool {
    private Options options;
    private ScmHelpGenerator hp;
    private ScmExecutorWrapper executor;

    public ScmListToolImpl() throws ScmToolsException {
        options = new Options();
        hp = new ScmHelpGenerator();
        options.addOption(hp.createOpt("p", "port", "node port", false, true, false));
        options.addOption(hp.createOpt("m", "mode", "list mode, 'run' or 'local', default:run.",
                false, true, false));
        executor = new ScmExecutorWrapper();
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        SchCtl.checkHelpArgs(args);

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
