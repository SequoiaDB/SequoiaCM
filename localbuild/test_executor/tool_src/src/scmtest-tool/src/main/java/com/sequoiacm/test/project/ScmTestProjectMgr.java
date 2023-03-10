package com.sequoiacm.test.project;

import com.sequoiacm.test.common.ProjectDefine;
import com.sequoiacm.test.config.LocalPathConfig;
import com.sequoiacm.test.parser.ProjectInfoConfParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScmTestProjectMgr {

    private final static Logger logger = LoggerFactory.getLogger(ScmTestProjectMgr.class);
    private static volatile ScmTestProjectMgr INSTANCE;

    private Map<String, ScmTestProject> projects = new HashMap<>();

    public static ScmTestProjectMgr getInstance() {
        if (INSTANCE == null) {
            synchronized (ScmTestProjectMgr.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ScmTestProjectMgr();
                }
            }
        }
        return INSTANCE;
    }

    private ScmTestProjectMgr() {
        try {
            init();
        }
        catch (IOException e) {
            throw new IllegalArgumentException("Configuration error:" + e.getMessage(), e);
        }
    }

    public ScmTestProject getTestProject(String name) {
        return projects.get(name);
    }

    private ScmTestProject getOrInitTestProject(String name) {
        ScmTestProject testProject = getTestProject(name);
        if (testProject == null) {
            testProject = new ScmTestProject();
            projects.put(name, testProject);
        }
        return testProject;
    }

    private void init() throws IOException {
        logger.info("Parsing the project info...");
        ProjectInfoConfParser parser = new ProjectInfoConfParser(LocalPathConfig.PRO_INFO_PATH);
        initCompileInfo(parser);
        initCompileVariable(parser);
        initTestNgXml(parser);
        logger.info("Parse the project info success");
    }

    private void initCompileInfo(ProjectInfoConfParser parser) {
        List<CompileInfo> compileInfoList = parser
                .getSeactionWithCheck(ProjectDefine.SEACTION_COMPILE_INFO, CompileInfo.CONVERTER);
        for (CompileInfo compileInfo : compileInfoList) {
            ScmTestProject testProject = getOrInitTestProject(compileInfo.getProject());
            testProject.setName(compileInfo.getProject());
            testProject.setPomPath(getFileAbsolutePath(compileInfo.getPomPath()));
            testProject.setCompileTargetPath(getFileAbsolutePath(compileInfo.getCompileTarget()));
        }
    }

    private void initCompileVariable(ProjectInfoConfParser parser) {
        List<CompileVariable> compileVariableList = parser.getSeactionWithCheck(
                ProjectDefine.SEACTION_COMPILE_VARIABLE, CompileVariable.CONVERTER);
        for (CompileVariable compileVariable : compileVariableList) {
            ScmTestProject testProject = getOrInitTestProject(compileVariable.getProject());
            String value = compileVariable.getVariableValue();
            if (value.startsWith("../") || value.startsWith("./")) {
                compileVariable.setVariableValue(getFileAbsolutePath(value));
            }
            testProject.getCompileVariableList().add(compileVariable);
        }
    }

    private void initTestNgXml(ProjectInfoConfParser parser) {
        List<TestNgXml> testNgXmlList = parser
                .getSeactionWithCheck(ProjectDefine.SEACTION_TEST_SUITE, TestNgXml.CONVERTER);
        for (TestNgXml testNgXml : testNgXmlList) {
            ScmTestProject testProject = getOrInitTestProject(testNgXml.getProject());
            testNgXml.setPath(getFileAbsolutePath(testNgXml.getPath()));
            testProject.getTestNgXmlList().add(testNgXml);
        }
    }

    private String getFileAbsolutePath(String path) {
        File file = new File(path);
        return file.isAbsolute() ? path : LocalPathConfig.BASE_PATH + path;
    }
}
