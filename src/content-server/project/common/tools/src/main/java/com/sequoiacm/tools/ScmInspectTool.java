package com.sequoiacm.tools;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.common.ScmContentCommandUtil;
import com.sequoiacm.tools.common.ScmCommon;
import com.sequoiacm.tools.common.ScmInspector;
import com.sequoiacm.tools.element.ScmSdbInfo;
import com.sequoiacm.tools.exception.ScmExitCode;

public class ScmInspectTool extends ScmTool {
    private final String OPT_SHORT_WS = "w";
    private final String OPT_LONG_WS = "workspace";
    private final String OPT_SHORT_SITE = "s";
    private final String OPT_LONG_SITE = "site";
    private Options ops;
    private ScmHelpGenerator hp;
    private static final Logger logger = LoggerFactory.getLogger(ScmInspectTool.class);

    public static void main(String[] argss) {
        try {
            ScmCommon.setLogAndProperties(ScmCommon.INSPECT_LOG_PATH, ScmCommon.LOG_FILE_INSPECT);
            ScmInspectTool tool = new ScmInspectTool();
            tool.process(argss);
            System.exit(ScmExitCode.SUCCESS);
        }
        catch (ScmToolsException e) {
            e.printErrorMsg();
            System.exit(e.getExitCode());
        }
        catch (Exception e) {
            logger.error("process failed", e);
            System.err.println("process failed,stack trace:");
            e.printStackTrace();
            System.exit(ScmExitCode.SYSTEM_ERROR);
        }
    }

    public ScmInspectTool() throws ScmToolsException {
        super("inspect");
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt(OPT_SHORT_WS, OPT_LONG_WS, "workspace name", true, true, false));
        ops.addOption(hp.createOpt(OPT_SHORT_SITE, OPT_LONG_SITE,
                "site name, which must belong to data location of\nthe workspcace(specified by -w).",
                true, true, false));
        ScmContentCommandUtil.addDsOption(ops, hp);

        ops.addOption(hp.createOpt(ScmContentCommandUtil.OPT_SHORT_VER, ScmContentCommandUtil.OPT_LONG_VER,
                "version", false, false, false));
        ops.addOption(hp.createOpt(ScmContentCommandUtil.OPT_SHORT_HELP, ScmContentCommandUtil.OPT_LONG_HELP,
                "help", false, false, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        if (ScmContentCommandUtil.isContainHelpArg(args)) {
            printHelp(false);
            return;
        }
        if (ScmContentCommandUtil.isNeedPrintVersion(args)) {
            ScmCommon.printVersion();
            return;
        }
        CommandLine cl = ScmContentCommandUtil.parseArgs(args, ops);
        ScmSdbInfo mainSiteSdb = ScmContentCommandUtil.parseDsOption(cl);

        ScmInspector inspector = new ScmInspector(cl.getOptionValue(OPT_LONG_SITE),
                cl.getOptionValue(OPT_LONG_WS), mainSiteSdb);
        try {
            inspector.inspectAll();
        }
        catch (ScmToolsException e) {
            e.printErrorMsg();
            throw new ScmToolsException(e.getExitCode());
        }
        finally {
            logger.info("Total residual lob:" + inspector.getCount() + ",workspace:"
                    + cl.getOptionValue(OPT_LONG_WS) + ",site:" + cl.getOptionValue(OPT_LONG_SITE));
            System.out.println("Total residual lob:" + inspector.getCount() + ",workspace:"
                    + cl.getOptionValue(OPT_LONG_WS) + ",site:" + cl.getOptionValue(OPT_LONG_SITE));
            inspector.close();
        }

    }

    @Override
    public void printHelp(boolean isFullHelp) {
        hp.printHelp(false);
    }
}
