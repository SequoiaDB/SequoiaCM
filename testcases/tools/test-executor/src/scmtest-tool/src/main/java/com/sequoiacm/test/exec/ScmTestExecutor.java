package com.sequoiacm.test.exec;

import com.sequoiacm.test.common.CommonUtil;
import com.sequoiacm.test.config.LocalPathConfig;
import com.sequoiacm.test.module.ExecResult;
import com.sequoiacm.test.module.TestTaskInfo;
import com.sequoiacm.test.module.Worker;
import com.sequoiacm.test.project.TestNgXml;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

public abstract class ScmTestExecutor implements Callable<ExecResult> {

    protected TestTaskInfo taskInfo;

    protected ScmTestExecutor(TestTaskInfo taskInfo) throws IOException {
        this.taskInfo = taskInfo;
        Worker worker = taskInfo.getWorker();
        TestNgXml testNgXml = taskInfo.getTestNgXml();

        // clean up or create a product directory locally for this execution of the test
        // tool, for example:
        // .../executor/console-out/localhost/story
        // .../executor/test-output/localhost/story
        String consoleOutPath = LocalPathConfig.CONSOLE_OUT_PATH + worker.getName() + File.separator
                + testNgXml.getProject();
        String testOutputPath = LocalPathConfig.TEST_OUTPUT_PATH + worker.getName() + File.separator
                + testNgXml.getProject();

        CommonUtil.cleanOrInitDir(consoleOutPath, testOutputPath);
    }

    public TestTaskInfo getTaskInfo() {
        return taskInfo;
    }

    public void updateTaskInfo(TestTaskInfo taskInfo) {
        this.taskInfo = taskInfo;
    }
}
