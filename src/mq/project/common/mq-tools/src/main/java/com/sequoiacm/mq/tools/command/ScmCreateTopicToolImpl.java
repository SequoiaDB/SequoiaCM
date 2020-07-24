package com.sequoiacm.mq.tools.command;

import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.cli.CommandLine;

import com.sequoiacm.mq.client.config.AdminClient;
import com.sequoiacm.mq.core.exception.MqError;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.tools.MqAdmin;
import com.sequoiacm.mq.tools.exception.ScmExitCode;

public class ScmCreateTopicToolImpl extends MqToolBase {
    private final String OPT_SHORT_NAME = "n";
    private final String OPT_LONG_NAME = "name";
    private final String OPT_SHORT_PARTITION_COUNT = "p";
    private final String OPT_LONG_PARTITION_COUNT = "partition-count";

    public ScmCreateTopicToolImpl() throws ScmToolsException {
        super("createtopic");
        ops.addOption(
                hp.createOpt(OPT_SHORT_NAME, OPT_LONG_NAME, "topic name.", true, true, false));
        ops.addOption(hp.createOpt(OPT_SHORT_PARTITION_COUNT, OPT_LONG_PARTITION_COUNT,
                "partition count.", true, true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);

        String name = cl.getOptionValue(OPT_LONG_NAME);
        String partitionCountStr = cl.getOptionValue(OPT_LONG_PARTITION_COUNT);
        int partitionCount = ScmCommon.convertStrToInt(partitionCountStr);

        AdminClient client = getAdminClient(cl);
        try {
            client.createTopic(name, partitionCount);
            System.out.println("create topic success:" + name);
        }
        catch (MqException e) {
            if (e.getError() == MqError.TOPIC_EXIST) {
                throw new ScmToolsException("topic already exist:" + name, ScmExitCode.TOPIC_EXIST,
                        e);
            }
            throw new ScmToolsException(
                    "failed to create topic:" + name + ", cause by:" + e.getMessage(),
                    ScmExitCode.SYSTEM_ERROR, e);
        }
    }

}
