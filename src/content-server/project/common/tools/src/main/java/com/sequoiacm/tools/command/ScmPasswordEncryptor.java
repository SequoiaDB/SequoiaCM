package com.sequoiacm.tools.command;

import com.sequoiacm.tools.common.ScmContentCommon;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.crypto.ScmPasswordMgr;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.element.ScmUserInfo;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.common.ScmContentCommandUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ScmPasswordEncryptor extends ScmTool {
    private static final Logger logger = LoggerFactory.getLogger(ScmPasswordEncryptor.class);
    private final String LONG_PASSWD = "password";
    private final String SHORT_PASSWD = "p";
    private final String LONG_OUTPUT_FILE = "output-file";
    private final String SHORT_OUTPUT_FILE = "o";
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
        ops.addOption(hp.createOpt(SHORT_OUTPUT_FILE, LONG_OUTPUT_FILE,
                "encrypt password write to output file ", false, true, true, false, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmContentCommandUtil.parseArgs(args, ops);
        ScmUserInfo userInfo = ScmCommandUtil.checkAndGetUser(cl, LONG_USER, LONG_PASSWD, false);
        try {
            String encrypted = ScmPasswordMgr.getInstance()
                    .encrypt(ScmPasswordMgr.SCM_CRYPT_TYPE_DES, userInfo.getPassword());
            if (cl.hasOption(LONG_OUTPUT_FILE)) {
                String passwordFile = cl.getOptionValue(LONG_OUTPUT_FILE);
                passwdRedirectFile(userInfo, encrypted, passwordFile);
            }
            else {
                System.out.println(userInfo.getUsername() + ":" + encrypted);
            }
        }
        catch (Exception e) {
            logger.error("encrypt failed", e);
            ScmCommon.throwToolException("encrypt failed", e);
        }
    }

    private void passwdRedirectFile(ScmUserInfo userInfo, String encrypted, String passwordFile)
            throws ScmToolsException {
        File file = null;
        FileWriter fileWriter = null;
        BufferedWriter bw = null;
        try {
            file = new File(passwordFile);
            if (!file.isAbsolute()) {
                file = new File(
                        ScmCommon.getUserWorkingDir() + File.separator + passwordFile);
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            fileWriter = new FileWriter(file.getAbsoluteFile());
            bw = new BufferedWriter(fileWriter);
            bw.write(userInfo.getUsername() + ":" + encrypted);
            System.out.println("encrypt password write to " + file.getCanonicalPath() + " success");
        }
        catch (IOException e) {
            logger.error("encrypt password write to file failed", e);
            ScmCommon.throwToolException("encrypt password write to file failed", e);
        }
        finally {
            ScmContentCommon.closeResource(bw);
            ScmContentCommon.closeResource(fileWriter);
        }
    }

    @Override
    public void printHelp(boolean isFullHelp) {
        hp.printHelp(isFullHelp);
    }
}