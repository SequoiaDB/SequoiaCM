package com.sequoiacm.test.exec;

import com.sequoiacm.test.common.CommonUtil;
import com.sequoiacm.test.config.LocalPathConfig;
import com.sequoiacm.test.module.ExecResult;
import com.sequoiacm.test.module.HostInfo;
import com.sequoiacm.test.module.TestTaskInfo;
import com.sequoiacm.test.project.TestNgXml;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

public abstract class ScmTestExecutor implements Callable<ExecResult> {

    protected TestTaskInfo taskInfo;

    protected ScmTestExecutor(TestTaskInfo taskInfo) throws IOException {
        this.taskInfo = taskInfo;
        HostInfo hostInfo = taskInfo.getHostInfo();
        TestNgXml testNgXml = taskInfo.getTestNgXml();

        // clean up or create a product directory locally for this execution of the test
        // tool, for example:
        // .../executor/output/localhost/story
        // .../executor/test-output/localhost/story
        String outputPath = LocalPathConfig.OUTPUT_PATH + hostInfo.getHostname() + File.separator
                + testNgXml.getProject();
        String testOutputPath = LocalPathConfig.TEST_OUTPUT_PATH + hostInfo.getHostname()
                + File.separator + testNgXml.getProject();

        CommonUtil.cleanOrInitDir(outputPath, testOutputPath);
    }

    public TestTaskInfo getTaskInfo() {
        return taskInfo;
    }

    public void updateTaskInfo(TestTaskInfo taskInfo) {
        this.taskInfo = taskInfo;
    }
}
