package com.sequoiacm.test.project;

import com.sequoiacm.test.common.BsonUtil;
import com.sequoiacm.test.common.ProjectDefine;
import com.sequoiacm.test.common.StringUtil;
import com.sequoiacm.test.parser.ConfConverter;
import org.bson.BSONObject;

import java.io.IOException;
import java.util.List;

public class TestNgXml {

    private String project;
    private String name;
    private String path;
    private boolean isConcurrent;
    private List<String> tags;
    private int priority;

    public static final ConfConverter<TestNgXml> CONVERTER = new ConfConverter<TestNgXml>() {
        @Override
        public TestNgXml convert(BSONObject bson) {
            return new TestNgXml(bson);
        }
    };

    public TestNgXml(BSONObject bson) {
        project = BsonUtil.getStringChecked(bson, ProjectDefine.TEST_SUITE_PROJECT);
        name = BsonUtil.getStringChecked(bson, ProjectDefine.TEST_SUITE_NAME);
        path = BsonUtil.getStringChecked(bson, ProjectDefine.TEST_SUITE_TESTNG_XML_PATH);
        isConcurrent = BsonUtil.getBooleanChecked(bson, ProjectDefine.TEST_SUITE_IS_CONCURRENT);
        tags = StringUtil.string2List(BsonUtil.getStringChecked(bson, ProjectDefine.TEST_SUITE_TAGS), ",");
        priority = BsonUtil.getIntegerChecked(bson, ProjectDefine.TEST_SUITE_PRIORITY);
    }

    public TestNgXml(String project, String name, String path) {
        this.project = project;
        this.name = name;
        this.path = path;
    }

    public ScmTestNgXmlRefactor createRefactor() throws IOException {
        return new ScmTestNgXmlRefactor(this);
    }

    public String getProject() {
        return project;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public boolean isConcurrent() {
        return isConcurrent;
    }

    public List<String> getTags() {
        return tags;
    }

    public int getPriority() {
        return priority;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "TestNgXml{" + "project='" + project + '\'' + ", name='" + name + '\'' + ", path='"
                + path + '\'' + ", isConcurrent=" + isConcurrent + ", tags=" + tags + ", priority="
                + priority + '}';
    }
}
