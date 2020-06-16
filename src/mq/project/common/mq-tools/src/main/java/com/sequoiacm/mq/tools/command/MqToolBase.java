package com.sequoiacm.mq.tools.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import com.sequoiacm.mq.client.config.AdminClient;
import com.sequoiacm.mq.client.remote.AdminFeignClient;
import com.sequoiacm.mq.core.exception.FeignExceptionConverter;
import com.sequoiacm.mq.tools.common.ScmHelpGenerator;
import com.sequoiacm.mq.tools.exception.ScmToolsException;

public abstract class MqToolBase implements ScmTool {
    protected String OPT_LONG_URL = "url";
    protected String OPT_SHORT_URL = "u";
    protected Options ops;
    protected ScmHelpGenerator hp;

    public MqToolBase() throws ScmToolsException {
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt(OPT_SHORT_URL, OPT_LONG_URL,
                "message queue server url, default: localhost:8610.", false, true, false));
    }

    protected AdminClient getAdminClient(CommandLine cl) {
        String url = cl.getOptionValue(OPT_LONG_URL);
        if (url == null) {
            url = "localhost:8610";
        }
        AdminFeignClient feign = ScmFeignClient.builderForNotSpring()
                .exceptionConverter(new FeignExceptionConverter())
                .instanceTarget(AdminFeignClient.class, url);
        return new AdminClient(feign);
    }

    @Override
    public void printHelp(boolean isHelpFull) {
        hp.printHelp(isHelpFull);
    }

}
