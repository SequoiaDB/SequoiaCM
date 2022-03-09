package com.sequoiacm.infrastructure.monitor.endpoint;

import com.sequoiacm.infrastructure.monitor.model.ScmThreadInfo;
import org.springframework.boot.actuate.endpoint.Endpoint;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class ScmThreadInfoEndpoint implements Endpoint<ScmThreadInfo> {
    @Override
    public String getId() {
        return "thread_info";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean isSensitive() {
        return false;
    }

    @Override
    public ScmThreadInfo invoke() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);
        int waitingCount = 0;
        int runnableCount = 0;
        for (ThreadInfo threadInfo : threadInfos) {
            Thread.State threadState = threadInfo.getThreadState();
            if (threadState == Thread.State.RUNNABLE) {
                runnableCount++;
            }
            else if (threadState == Thread.State.WAITING
                    || threadState == Thread.State.TIMED_WAITING) {
                waitingCount++;
            }
        }
        ScmThreadInfo scmThreadInfo = new ScmThreadInfo();
        scmThreadInfo.setAll(threadInfos.length);
        scmThreadInfo.setRunnable(runnableCount);
        scmThreadInfo.setWaiting(waitingCount);
        return scmThreadInfo;
    }
}
