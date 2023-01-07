package com.sequoiacm.cloud.tools.command;

import java.util.List;

import org.apache.commons.cli.CommandLine;

import com.sequoiacm.infrastructure.tool.command.ScmStopToolImpl;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.operator.ScmServiceNodeOperator;

public class ScmStopToolImplCloud extends ScmStopToolImpl {
    private final String OPT_LONG_TIMEOUT = "timeout";

    public ScmStopToolImplCloud(List<ScmServiceNodeOperator> nodeOperatorList)
            throws ScmToolsException {
        super(nodeOperatorList);
        SLEEP_TIME = 1000;
        options.addOption(hp.createOpt(null, OPT_LONG_TIMEOUT,
                "sets the stopping timeout in seconds, default:30", false, true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        // 先解析一次命令行 设置 stop timeout
        CommandLine commandLine = ScmCommandUtil.parseArgs(args, options);
        if (commandLine.hasOption(OPT_LONG_TIMEOUT)) {
            STOP_TIMEOUT = ScmCommandUtil.getTimeout(commandLine, OPT_LONG_TIMEOUT);
        }

        // 再调用父类的 process
        super.process(args);
    }
}
