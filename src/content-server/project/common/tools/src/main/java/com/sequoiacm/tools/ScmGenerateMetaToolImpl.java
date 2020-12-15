package com.sequoiacm.tools;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.common.ScmContentCommandUtil;
import com.sequoiacm.tools.common.ScmContentCommon;
import com.sequoiacm.tools.common.ScmMetaGenerator;
import com.sequoiacm.tools.element.ScmSdbInfo;
import com.sequoiacm.tools.exception.ScmExitCode;

public class ScmGenerateMetaToolImpl extends ScmTool {
    private final String OPT_SHORT_WS = "w";
    private final String OPT_LONG_WS = "workspace";
    private final String OPT_SHORT_CL = "c";
    private final String OPT_LONG_CL = "collection";
    private final String OPT_SHORT_HELP = "h";
    private final String OPT_LONG_HELP = "help";
    private static Logger logger = LoggerFactory.getLogger(ScmGenerateMetaToolImpl.class);
    private Options ops = new Options();
    private ScmHelpGenerator hp = new ScmHelpGenerator();

    public static void main(String[] args) {
        ScmGenerateMetaToolImpl tool;
        try {
            ScmContentCommon.setLogAndProperties(ScmContentCommon.GENERATE_META_LOG_PATH,
                    ScmContentCommon.LOG_FILE_GENERATE_META);

            logger.error(
                    "can't reused sdb's ObjectId as scm's fileId. see more details on scm's id generator");
            throw new Exception(
                    "can't reused sdb's ObjectId as scm's fileId. see more details on scm's id generator");
            // tool = new ScmGenerateMetaToolImpl();
            // tool.process(args);
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

    public ScmGenerateMetaToolImpl() throws ScmToolsException {
        super("generatemeta");
        ops.addOption(hp.createOpt(OPT_SHORT_WS, OPT_LONG_WS, "workspace name", true, true, false));
        ops.addOption(hp.createOpt(OPT_SHORT_CL, OPT_LONG_CL,
                "lob collection full name, eg:'cs.cl'", true, true, false));
        ScmContentCommandUtil.addDsOption(ops, hp);
        ops.addOption(hp.createOpt(ScmContentCommandUtil.OPT_SHORT_VER, ScmContentCommandUtil.OPT_LONG_VER,
                "version", false, false, false));
        ops.addOption(hp.createOpt(OPT_SHORT_HELP, OPT_LONG_HELP, "help", false, false, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        if (ScmContentCommandUtil.isContainHelpArg(args)) {
            printHelp(false);
            return;
        }
        if (ScmContentCommandUtil.isNeedPrintVersion(args)) {
            ScmContentCommon.printVersion();
            return;
        }

        CommandLine cl = ScmContentCommandUtil.parseArgs(args, ops);
        // cs & cl
        String clFullName = cl.getOptionValue(OPT_LONG_CL);
        String[] clAndCs = clFullName.split("\\.");
        if (clAndCs.length != 2) {
            logger.error("invalid collection full name:" + clFullName);
            throw new ScmToolsException("invalid collection full name:" + clFullName,
                    ScmExitCode.INVALID_ARG);
        }
        String lobClName = clAndCs[1];
        String lonCsName = clAndCs[0];

        ScmSdbInfo mainSdb = ScmContentCommandUtil.parseDsOption(cl);
        ScmMetaGenerator smg = new ScmMetaGenerator(cl.getOptionValue(OPT_SHORT_WS), lonCsName,
                lobClName, mainSdb);

        try {
            smg.generate();
        }
        catch (ScmToolsException e) {
            e.printErrorMsg();
            throw new ScmToolsException(e.getExitCode());
        }
        finally {
            logger.info("Total:" + smg.getGeneratedCount() + " meta has been generated");
            System.out.println("Total:" + smg.getGeneratedCount() + " meta has been generated");
            smg.close();
        }

    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        hp.printHelp(isFullHelp);
    }

    //
    // public static void main(String[] args) throws ParseException {
    // SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    // Date d = sdf.parse("2017-07-13 08:45:59");
    // System.out.println(sdf.format(d));
    // }
}
