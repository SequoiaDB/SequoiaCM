package com.sequoiacm.tools.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.crypto.ScmPasswordMgr;
import com.sequoiacm.tools.ScmAdmin;
import com.sequoiacm.tools.common.ScmCommandUtil;
import com.sequoiacm.tools.common.ScmHelpGenerator;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiacm.tools.exception.ScmToolsException;

public class ScmPasswordEncryptor implements ScmTool {
    private static final Logger logger = LoggerFactory.getLogger(ScmPasswordEncryptor.class);
    private final String LONG_PASSWD = "password";
    private final String SHORT_PASSWD = "p";
    private final String LONG_USER = "user";
    private final String SHORT_USER = "u";

    private Options ops;
    private ScmHelpGenerator hp;

    public ScmPasswordEncryptor() throws ScmToolsException {
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt(SHORT_PASSWD, LONG_PASSWD, "password to be encrypted.", true,
                true, false));
        ops.addOption(hp.createOpt(SHORT_USER, LONG_USER, "password to be encrypted.", true, true,
                false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        ScmAdmin.checkHelpArgs(args);
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        String user = cl.getOptionValue(LONG_USER);
        String passwd = cl.getOptionValue(LONG_PASSWD);
        try {
            String encrypted = ScmPasswordMgr.getInstance()
                    .encrypt(ScmPasswordMgr.SCM_CRYPT_TYPE_DES, passwd);
            System.out.println(user + ":" + encrypted);
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