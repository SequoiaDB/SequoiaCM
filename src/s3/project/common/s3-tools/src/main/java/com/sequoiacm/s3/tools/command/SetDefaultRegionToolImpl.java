package com.sequoiacm.s3.tools.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3.tools.common.ScmCommandUtil;
import com.sequoiacm.s3.tools.common.ScmHelpGenerator;
import com.sequoiacm.s3.tools.exception.ScmExitCode;

public class SetDefaultRegionToolImpl extends ScmTool {
    private final String OPT_SHORT_REGION = "r";
    private final String OPT_LONG_REGION = "region";
    private final String OPT_LONG_USER = "user";
    private final String OPT_SHORT_USER = "u";
    private final String OPT_SHORT_PWD = "p";
    private final String OPT_LONG_PWD = "password";
    private final String OPT_LONG_URL = "url";

    private Options ops;
    private ScmHelpGenerator hp;
    private final Logger logger = LoggerFactory.getLogger(SetDefaultRegionToolImpl.class.getName());

    public SetDefaultRegionToolImpl() throws ScmToolsException {
        super("set-default-region");
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt(OPT_SHORT_REGION, OPT_LONG_REGION,
                "region name (workspace name).", true, true, false));
        ops.addOption(hp.createOpt(OPT_SHORT_USER, OPT_LONG_USER, "username for login.", true, true,
                false));
        ops.addOption(hp.createOpt(OPT_SHORT_PWD, OPT_LONG_PWD, "password for login.", true, true,
                false, true, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_URL, "gate way url, default:localhost:8080",
                false, true, false));

    }

    @Override
    public void printHelp(boolean isHelpFull) {
        hp.printHelp(isHelpFull);
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        String user = cl.getOptionValue(OPT_SHORT_USER);
        String passwd = cl.getOptionValue(OPT_SHORT_PWD);
        String region = cl.getOptionValue(OPT_SHORT_REGION);
        if (passwd == null) {
            System.out.print("password: ");
            passwd = ScmCommandUtil.readPasswdFromStdIn();
        }
        String url = "localhost:8080";
        if (cl.hasOption(OPT_LONG_URL)) {
            url = cl.getOptionValue(OPT_LONG_URL);
        }

        ScmSession session = null;
        try {
            session = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, passwd));

            ScmFactory.S3.setDefaultRegion(session, region);
            System.out.println("set default region successfully:region=" + region);
            logger.info("set default region:region={}", region);
        }
        catch (Exception e) {
            logger.error("set default region failed:{}", region, e);
            System.out.println("failed set default region:" + e.getMessage());
            throw new ScmToolsException("set default region failed", ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            if (session != null) {
                session.close();
            }
        }
    }
}
