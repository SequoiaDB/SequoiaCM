package com.sequoiacm.mq.tools.command;

import org.apache.commons.cli.CommandLine;

import com.sequoiacm.mq.client.config.AdminClient;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.tools.MqAdmin;
import com.sequoiacm.mq.tools.common.ScmCommandUtil;
import com.sequoiacm.mq.tools.common.ScmCommon;
import com.sequoiacm.mq.tools.exception.ScmExitCode;
import com.sequoiacm.mq.tools.exception.ScmToolsException;

public class ScmUpdateTopicToolImpl extends MqToolBase {
    private final String OPT_SHORT_NAME = "n";
    private final String OPT_LONG_NAME = "name";
    private final String OPT_LONG_NEW_PARTITION = "new-partition-count";
    private final String OPT_SHORT_NEW_PARTITION = "p";
    private final String OPT_SHORT_TIMEOUT = "t";
    private final String OPT_LONG_TIMEOUT = "timeout";

    public ScmUpdateTopicToolImpl() throws ScmToolsException {
        super();
        ops.addOption(
                hp.createOpt(OPT_SHORT_NAME, OPT_LONG_NAME, "topic name.", true, true, false));
        ops.addOption(hp.createOpt(OPT_SHORT_NEW_PARTITION, OPT_LONG_NEW_PARTITION,
                "new partition count.", true, true, false));
        ops.addOption(hp.createOpt(OPT_SHORT_TIMEOUT, OPT_LONG_TIMEOUT, "timeout for update.",
                false, true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        MqAdmin.checkHelpArgs(args);
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        String name = cl.getOptionValue(OPT_LONG_NAME);
        String newPartitionCountStr = cl.getOptionValue(OPT_LONG_NEW_PARTITION);
        int newPartitionCount = ScmCommon.convertStrToInt(newPartitionCountStr);
        if (newPartitionCount < 1) {
            throw new ScmToolsException("invalid partition count:" + newPartitionCount,
                    ScmExitCode.INVALID_ARG);
        }
        String timeoutStr = cl.getOptionValue(OPT_LONG_TIMEOUT, Long.MAX_VALUE + "");
        long timeout = ScmCommon.convertStrToLong(timeoutStr);
        if (timeout < 0) {
            throw new ScmToolsException("invalid timeout:" + timeout, ScmExitCode.INVALID_ARG);
        }
        AdminClient client = getAdminClient(cl);
        try {
            client.updateTopicPartitionCount(name, newPartitionCount, timeout);
            System.out.println("update topic success:" + name);
        }
        catch (MqException e) {
            throw new ScmToolsException(
                    "failed to update topic:" + name + ", cause by:" + e.getMessage(),
                    ScmExitCode.SYSTEM_ERROR, e);
        }
    }

}
