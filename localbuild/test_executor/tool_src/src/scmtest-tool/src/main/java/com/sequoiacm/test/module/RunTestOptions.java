package com.sequoiacm.test.module;

import com.sequoiacm.test.common.CommonUtil;
import com.sequoiacm.test.common.StringUtil;
import com.sequoiacm.test.subcommand.ScmRunTestCommand;
import org.apache.commons.cli.CommandLine;

import java.util.List;

public class RunTestOptions {

    private String project;
    private String workPath;
    private String confPath;
    private List<String> testngConf;

    private List<String> packages;
    private List<String> classes;
    private String threadCount;
    private String sites;
    private boolean isNeedCompile;
    private boolean isRunBase;

    public RunTestOptions(CommandLine cl) {
        if (cl.hasOption(ScmRunTestCommand.OPT_WORK_PATH)) {
            workPath = cl.getOptionValue(ScmRunTestCommand.OPT_WORK_PATH);
        }

        if (cl.hasOption(ScmRunTestCommand.OPT_PROJECT)) {
            confPath = cl.getOptionValue(ScmRunTestCommand.OPT_CONF);
        }
        project = cl.getOptionValue(ScmRunTestCommand.OPT_PROJECT);

        if (cl.hasOption(ScmRunTestCommand.OPT_TESTNG)) {
            testngConf = StringUtil.string2List(cl.getOptionValue(ScmRunTestCommand.OPT_TESTNG),
                    ",");
        }
        if (cl.hasOption(ScmRunTestCommand.OPT_PACKAGE)) {
            packages = StringUtil.string2List(cl.getOptionValue(ScmRunTestCommand.OPT_PACKAGE),
                    ",");
        }
        if (cl.hasOption(ScmRunTestCommand.OPT_CLASS)) {
            classes = StringUtil.string2List(cl.getOptionValue(ScmRunTestCommand.OPT_CLASS), ",");
        }

        if (packages != null || classes != null) {
            CommonUtil.assertTrue(testngConf.size() == 1,
                    "Only one testng XML can be specified when specifying the packages or classes");
        }

        sites = cl.getOptionValue(ScmRunTestCommand.OPT_SITES);
        threadCount = cl.getOptionValue(ScmRunTestCommand.OPT_THREAD_COUNT);
        if (threadCount != null) {
            CommonUtil.assertTrue(StringUtil.isPositiveInteger(threadCount),
                    "The option of thread must be a positive integer");
        }

        isNeedCompile = !cl.hasOption(ScmRunTestCommand.OPT_NO_COMPILE);
        isRunBase = cl.hasOption(ScmRunTestCommand.OPT_RUN_BASE);
    }

    public String getWorkPath() {
        return workPath;
    }

    public String getConfPath() {
        return confPath;
    }

    public String getProject() {
        return project;
    }

    public List<String> getTestngConf() {
        return testngConf;
    }

    public List<String> getPackages() {
        return packages;
    }

    public List<String> getClasses() {
        return classes;
    }

    public String getSites() {
        return sites;
    }

    public String getThreadCount() {
        return threadCount;
    }

    public boolean isNeedCompile() {
        return isNeedCompile;
    }

    public boolean isRunBase() {
        return isRunBase;
    }
}
