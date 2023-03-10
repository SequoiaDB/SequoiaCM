package com.sequoiacm.test.subcommand;

import com.sequoiacm.test.common.CommonDefine;
import com.sequoiacm.test.common.CommonUtil;
import com.sequoiacm.test.common.ScmTestRunner;
import com.sequoiacm.test.config.LocalPathConfig;
import com.sequoiacm.test.module.RunTestOptions;
import com.sequoiacm.test.config.ScmTestToolProps;
import com.sequoiacm.test.module.TestTaskInfo;
import com.sequoiacm.test.module.TestTaskInfoGroup;
import com.sequoiacm.test.module.Worker;
import com.sequoiacm.test.project.ScmTestNgXmlRefactor;
import com.sequoiacm.test.project.ScmTestProject;
import com.sequoiacm.test.project.ScmTestProjectMgr;
import com.sequoiacm.test.project.TestNgXml;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Subcommand
public class ScmRunTestCommand extends ScmTestSubcommand<RunTestOptions> {

    public static final String NAME = "runtest";

    public static final String OPT_CONF = "conf";

    public static final String OPT_WORK_PATH = "work-path";
    public static final String OPT_PROJECT = "project";
    public static final String OPT_TESTNG = "testng-conf";
    public static final String OPT_PACKAGE = "packages";
    public static final String OPT_CLASS = "classes";

    public static final String OPT_SITES = "sites";
    public static final String OPT_THREAD_COUNT = "thread";

    public static final String OPT_NO_COMPILE = "nocompile";
    public static final String OPT_RUN_BASE = "runbase";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDesc() {
        return "execute test cases.";
    }

    @Override
    protected RunTestOptions parseCommandLineArgs(CommandLine commandLine) {
        CommonUtil.assertTrue(commandLine.hasOption(OPT_PROJECT),
                "missing required option:" + OPT_PROJECT);
        if ((commandLine.hasOption(OPT_CLASS) || commandLine.hasOption(OPT_PACKAGE))
                && !commandLine.hasOption(OPT_TESTNG)) {
            throw new IllegalArgumentException(
                    "Specify class or package must has required option: " + OPT_TESTNG);
        }

        return new RunTestOptions(commandLine);
    }

    @Override
    protected Options commandOptions() {
        Options ops = new Options();
        ops.addOption(
                Option.builder().longOpt(OPT_WORK_PATH).hasArg(true).desc("work path.").build());
        ops.addOption(
                Option.builder().longOpt(OPT_CONF).hasArg(true).desc("test conf path.").build());
        ops.addOption(Option.builder().longOpt(OPT_PROJECT).hasArg(true)
                .desc("specifies the test project to be executed.").build());
        ops.addOption(Option.builder().longOpt(OPT_TESTNG).hasArg(true)
                .desc("specifies the testng XML list.").build());
        ops.addOption(Option.builder().longOpt(OPT_PACKAGE).hasArg(true)
                .desc("specifies the list of package within testng XML.").build());
        ops.addOption(Option.builder().longOpt(OPT_CLASS).hasArg(true)
                .desc("specifies the list of class within testng XML.").build());
        ops.addOption(Option.builder().longOpt(OPT_SITES).hasArg(true)
                .desc("specifies the number of sites.").build());
        ops.addOption(Option.builder().longOpt(OPT_THREAD_COUNT).hasArg(true)
                .desc("the count of threads for concurrent testcase.").build());
        ops.addOption(Option.builder().longOpt(OPT_NO_COMPILE).hasArg(false)
                .desc("no need to compile the test project.").build());
        ops.addOption(Option.builder().longOpt(OPT_RUN_BASE).hasArg(false)
                .desc("run base testcase only.").build());
        return ops;
    }

    @Override
    protected void process(RunTestOptions options) throws Exception {
        // 1. 初始化工作目录、配置文件目录（默认在工具同级目录下）
        initWorkAndConfPath(options.getWorkPath(), options.getConfPath());

        // 2. 解析工具执行配置文件，执行机可达性检查
        ScmTestToolProps.getInstance().check();

        // 3. 解析测试工程信息
        ScmTestProjectMgr testProjectMgr = ScmTestProjectMgr.getInstance();
        ScmTestProject testProject = testProjectMgr.getTestProject(options.getProject());

        // 4. 编译测试工程
        if (options.isNeedCompile()) {
            testProject.doCompile();
        }

        List<TestNgXml> testNgXmlList = getTestNgXmlList(testProject, options);
        List<TestTaskInfoGroup> taskGroupList = generateTaskInfo(testNgXmlList, options);

        ScmTestRunner testRunner = null;
        try {
            testRunner = new ScmTestRunner(taskGroupList);
            testRunner.runAndCollectReport();
        }
        finally {
            if (testRunner != null) {
                testRunner.stop();
            }
        }
    }

    private void initWorkAndConfPath(String workPath, String confPath) throws IOException {
        if (workPath != null) {
            LocalPathConfig.WORK_PATH = workPath;
            LocalPathConfig.CONSOLE_OUT_PATH = workPath + File.separator + "console-out" + File.separator;
            LocalPathConfig.TEST_OUTPUT_PATH = workPath + File.separator + "test-output" + File.separator;
            LocalPathConfig.TMP_PATH = workPath + File.separator + "tmp" + File.separator;
            LocalPathConfig.TEST_RESULT_PATH = LocalPathConfig.TMP_PATH + "test.result";

            // 本地执行目录：${测试工具目录}/executor/local-exec
            LocalPathConfig.EXEC_PATH = workPath + File.separator + "local-exec" + File.separator;
        }
        File workDir = new File(
                LocalPathConfig.WORK_PATH);
        if (workDir.exists() && !workDir.isDirectory()) {
            throw new IllegalArgumentException(
                    String.format("File is not a directory, option: %s, path=%s", OPT_WORK_PATH,
                            workDir.getAbsolutePath()));
        }
        CommonUtil.createDir(LocalPathConfig.WORK_PATH);
        CommonUtil.createDir(LocalPathConfig.TMP_PATH);

        if (confPath != null) {
            File confDir = new File(confPath);
            if (!confDir.exists()) {
                throw new IllegalArgumentException(
                        String.format("Directory not exist, option: %s, path=%s", OPT_CONF,
                                confDir.getAbsolutePath()));
            }
            if (!confDir.isDirectory()) {
                throw new IllegalArgumentException(
                        String.format("File is not a directory, option: %s, path=%s", OPT_CONF,
                                confDir.getAbsolutePath()));
            }
            LocalPathConfig.CONF_PATH = confPath;
            LocalPathConfig.PRO_INFO_PATH = confPath + File.separator + "scmtest-project.cfg";
            LocalPathConfig.TOOL_CONF_PATH = confPath + File.separator + "scmtest.properties";
        }
    }

    private List<TestTaskInfoGroup> generateTaskInfo(List<TestNgXml> testNgXmlList,
            RunTestOptions options) throws IOException {
        ScmTestToolProps testToolProps = ScmTestToolProps.getInstance();
        List<Worker> workers = testToolProps.getWorkers();

        List<TestTaskInfoGroup> taskGroupList = new ArrayList<>();
        for (TestNgXml testNgXml : testNgXmlList) {
            ScmTestNgXmlRefactor refactor = testNgXml.createRefactor();
            refactor.resetParameters(testToolProps.getTestNgParameters());
            if (options.getSites() != null) {
                refactor.updateParameter(CommonDefine.SITES, options.getSites());
                // 部分预置 XML 仍使用组过滤站点数，兼容此类情况
                refactor.updateGroupFilter(options.getSites());
            }
            if (options.isRunBase()) {
                refactor.updateParameter(CommonDefine.RUNBASETEST, "true");
            }

            if (options.getPackages() != null || options.getClasses() != null) {
                refactor.cleanPackagesAndClasses();
                if (options.getPackages() != null) {
                    refactor.resetPackages(options.getPackages());
                }
                if (options.getClasses() != null) {
                    refactor.resetClasses(options.getClasses());
                }
            }
            if (testNgXml.isConcurrent()) {
                if (options.getThreadCount() != null) {
                    refactor.resetThreadCount(options.getThreadCount());
                }
                refactor.divide(workers);
            }
            else {
                refactor.divide(Collections.singletonList(workers.get(0)));
            }

            List<TestTaskInfo> taskInfoList = refactor.doRefactor();
            TestTaskInfoGroup taskGroup = new TestTaskInfoGroup(testNgXml.getName(),
                    testNgXml.getPriority(), taskInfoList);
            taskGroupList.add(taskGroup);
        }

        // 根据优先级排序 taskGroup（升序）
        taskGroupList.sort(Comparator.comparing(TestTaskInfoGroup::getPriority));
        return taskGroupList;
    }

    private List<TestNgXml> getTestNgXmlList(ScmTestProject testProject, RunTestOptions option) {
        if (option.getTestngConf() != null) {
            return testProject.getTestNgXmlByName(option.getTestngConf());
        }

        return testProject.getTestNgXmlList();
    }
}
