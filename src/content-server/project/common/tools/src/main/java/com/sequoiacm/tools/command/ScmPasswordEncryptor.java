package com.sequoiacm.tools.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.crypto.ScmPasswordMgr;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.element.ScmUserInfo;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.common.ScmContentCommandUtil;
import com.sequoiacm.tools.exception.ScmExitCode;

public class ScmPasswordEncryptor extends ScmTool {
    private static final Logger logger = LoggerFactory.getLogger(ScmPasswordEncryptor.class);
    private final String LONG_PASSWD = "password";
    private final String SHORT_PASSWD = "p";
    private final String LONG_USER = "user";
    private final String SHORT_USER = "u";

    private Options ops;
    private ScmHelpGenerator hp;

    public ScmPasswordEncryptor() throws ScmToolsException {
        super("encrypt");
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt(SHORT_USER, LONG_USER, "the name of user.", true, true, false));
        ops.addOption(hp.createOpt(SHORT_PASSWD, LONG_PASSWD, "password to be encrypted.", true,
                true, true, false, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmContentCommandUtil.parseArgs(args, ops);
        ScmUserInfo userInfo = ScmCommandUtil.checkAndGetUser(cl, LONG_USER, LONG_PASSWD, false);
        try {
            String encrypted = ScmPasswordMgr.getInstance()
                    .encrypt(ScmPasswordMgr.SCM_CRYPT_TYPE_DES, userInfo.getPassword());
            System.out.println(userInfo.getUsername() + ":" + encrypted);
        }
        catch (Exception e) {
            logger.error("encrypt failed", e);
            throw new ScmToolsException("encrypt failed", ScmExitCode.SYSTEM_ERROR);
        }
    }

    @Override
    public void printHelp(boolean isFullHelp) {
        hp.printHelp(isFullHelp);
    }
}