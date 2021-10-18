package com.sequoiacm.test.module;

import com.sequoiacm.test.common.CommonUtil;
import com.sequoiacm.test.common.StringUtil;
import com.sequoiacm.test.subcommand.ScmRunTestCommand;
import org.apache.commons.cli.CommandLine;

import java.util.List;

public class RunTestOptions {

    private String project;
    private List<String> testngConf;
    private String tag;

    private List<String> packages;
    private List<String> classes;
    private String threadCount;
    private String group;
    private boolean isNeedCompile;

    public RunTestOptions(CommandLine cl) {
        project = cl.getOptionValue(ScmRunTestCommand.OPT_PROJECT);
        tag = cl.getOptionValue(ScmRunTestCommand.OPT_TAG_TYPE);

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

        group = cl.getOptionValue(ScmRunTestCommand.OPT_GROUP);
        threadCount = cl.getOptionValue(ScmRunTestCommand.OPT_THREAD_COUNT);
        if (threadCount != null) {
            CommonUtil.assertTrue(StringUtil.isPositiveInteger(threadCount),
                    "The option of thread must be a positive integer");
        }

        isNeedCompile = !cl.hasOption(ScmRunTestCommand.OPT_NO_COMPILE);
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

    public String getTag() {
        return tag;
    }

    public String getGroup() {
        return group;
    }

    public String getThreadCount() {
        return threadCount;
    }

    public boolean isNeedCompile() {
        return isNeedCompile;
    }
}
