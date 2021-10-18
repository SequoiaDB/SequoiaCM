package com.sequoiacm.test.exec;

import com.sequoiacm.test.common.BashUtil;
import com.sequoiacm.test.config.LocalPathConfig;
import com.sequoiacm.test.module.ExecResult;
import com.sequoiacm.test.module.TestTaskInfo;

import java.io.File;
import java.io.IOException;

public class ScmTestLocalExecutor extends ScmTestExecutor {

    public ScmTestLocalExecutor(TestTaskInfo taskInfo) throws IOException {
        super(taskInfo);

        File testProgressFile = new File(LocalPathConfig.LOCAL_TEST_PROGRESS_PATH);
        if (testProgressFile.exists() && !testProgressFile.delete()) {
            throw new IOException("Failed to delete the local test progress file, path="
                    + testProgressFile.getAbsolutePath());
        }
    }

    @Override
    public ExecResult call() throws Exception {
        return BashUtil.exec(taskInfo.getExecCommand());
    }
}
