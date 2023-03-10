package com.sequoiacm.test.module;

import java.util.List;

public class TestTaskInfoGroup {

    private String xmlName;
    private int priority;
    private List<TestTaskInfo> taskInfoList;

    public TestTaskInfoGroup(String testNgXmlName, int priority, List<TestTaskInfo> taskInfoList) {
        this.xmlName = testNgXmlName;
        this.priority = priority;
        this.taskInfoList = taskInfoList;
    }

    public String getXmlName() {
        return xmlName;
    }

    public int getPriority() {
        return priority;
    }

    public List<TestTaskInfo> getTaskInfoList() {
        return taskInfoList;
    }
}
