package com.sequoiacm.tools.command;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.common.ScmContentCommandUtil;
import com.sequoiacm.tools.common.ScmCommon;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiacm.tools.exec.ScmExecutorWrapper;

public class ScmListToolImpl extends ScmTool {
    private Options options;
    private ScmHelpGenerator hp;
    private ScmExecutorWrapper executor;

    public ScmListToolImpl() throws ScmToolsException {
        super("list");
        options = new Options();
        hp = new ScmHelpGenerator();
        options.addOption(hp.createOpt("p", "port", "node port", false, true, false));
        options.addOption(hp.createOpt("m", "mode", "list mode, 'run' or 'local', default:run.",
                false, true, false));
        executor = new ScmExecutorWrapper();
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine commandLine = ScmContentCommandUtil.parseArgs(args, options);
        Map<Integer, String> node2Conf = executor.getAllNode();
        Map<String, Integer> runningNode = executor.getNodeStatus();
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

        List<String> portList = new ArrayList<>();
        List<String> pidList = new ArrayList<>();

        if (commandLine.hasOption("p")) {
            String portStr = commandLine.getOptionValue("p");
            int port = ScmCommon.convertStrToInt(portStr);
            String confPath = node2Conf.get(port);
            if (confPath != null) {
                Integer pidInteger = runningNode.get(confPath);
                if (pidInteger != null) {
                    portList.add(port + "");
                    pidList.add(pidInteger.toString());
                }
                else if (!printRunningOnly) {
                    portList.add(port + "");
                    pidList.add("-");
                }
            }
        }
        else {
            Iterator<Entry<Integer, String>> it = node2Conf.entrySet().iterator();
            while (it.hasNext()) {
                Entry<Integer, String> entry = it.next();
                Integer pidInteger = runningNode.get(entry.getValue());
                if (pidInteger != null) {
                    portList.add(entry.getKey() + "");
                    pidList.add(pidInteger.toString());
                }
                else if (!printRunningOnly) {
                    portList.add(entry.getKey() + "");
                    pidList.add("-");
                }
            }
        }
        for (int i = 0; i < pidList.size(); i++) {
            System.out.println("sequoiacm(" + portList.get(i) + ") (" + pidList.get(i) + ")");
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
