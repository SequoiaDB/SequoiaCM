package com.sequoiacm.mq.tools.command;

import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.cli.CommandLine;

import com.sequoiacm.mq.client.config.AdminClient;
import com.sequoiacm.mq.core.exception.MqError;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.ConsumerGroupOffsetEnum;
import com.sequoiacm.mq.tools.MqAdmin;
import com.sequoiacm.mq.tools.exception.ScmExitCode;

public class ScmCreateGroupToolImpl extends MqToolBase {
    private final String OPT_SHORT_NAME = "n";
    private final String OPT_LONG_NAME = "name";
    private final String OPT_SHORT_TOPIC = "t";
    private final String OPT_LONG_TOPIC = "topic";
    private final String OPT_LONG_OFFSET = "offset";
    private final String OPT_SHORT_OFFSET = "o";

    public ScmCreateGroupToolImpl() throws ScmToolsException {
        super("creategroup");
        ops.addOption(
                hp.createOpt(OPT_SHORT_NAME, OPT_LONG_NAME, "group name.", true, true, false));
        ops.addOption(
                hp.createOpt(OPT_SHORT_TOPIC, OPT_LONG_TOPIC, "topic name.", true, true, false));
        ops.addOption(hp.createOpt(OPT_SHORT_OFFSET, OPT_LONG_OFFSET,
                "optional values: oldest latest, default: oldest", false, true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);

        String name = cl.getOptionValue(OPT_LONG_NAME);
        String topic = cl.getOptionValue(OPT_LONG_TOPIC);
        String p = cl.getOptionValue(OPT_LONG_OFFSET, ConsumerGroupOffsetEnum.OLDEST.toString());

        ConsumerGroupOffsetEnum offset = null;
        try {
            offset = ConsumerGroupOffsetEnum.valueOf(p.toUpperCase());
        }
        catch (IllegalArgumentException e) {
            throw new ScmToolsException("unrecognized offset: " + p, ScmExitCode.INVALID_ARG, e);
        }

        AdminClient client = getAdminClient(cl);
        try {
            client.createGroup(name, topic, offset);
            System.out.println("create group success:" + name);
        }
        catch (MqException e) {
            if (e.getError() == MqError.CONSUMER_GROUP_EXIST) {
                throw new ScmToolsException("group already exist:" + name, ScmExitCode.TOPIC_EXIST,
                        e);
            }
            throw new ScmToolsException(
                    "failed to create group:" + name + ", cause by:" + e.getMessage(),
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        catch (Exception e) {
            throw new ScmToolsException(
                    "failed to create group:" + name + ", cause by:" + e.getMessage(),
                    ScmExitCode.SYSTEM_ERROR, e);
        }
    }
}
