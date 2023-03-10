package com.sequoiacm.test.project;

import com.sequoiacm.test.common.BsonUtil;
import com.sequoiacm.test.common.ProjectDefine;
import com.sequoiacm.test.parser.ConfConverter;
import org.bson.BSONObject;

public class CompileInfo {

    private String project;
    private String pomPath;
    private String compileTarget;

    public static final ConfConverter<CompileInfo> CONVERTER = new ConfConverter<CompileInfo>() {
        @Override
        public CompileInfo convert(BSONObject bson) {
            return new CompileInfo(bson);
        }
    };

    public CompileInfo(BSONObject bson) {
        project = BsonUtil.getStringChecked(bson, ProjectDefine.COMPILE_INFO_PROJECT);
        pomPath = BsonUtil.getStringChecked(bson, ProjectDefine.COMPILE_INFO_POM_PATH);
        compileTarget = BsonUtil.getStringChecked(bson, ProjectDefine.COMPILE_INFO_COMPILE_TARGET);
    }

    public String getProject() {
        return project;
    }

    public String getPomPath() {
        return pomPath;
    }

    public String getCompileTarget() {
        return compileTarget;
    }

    @Override
    public String toString() {
        return "CompileInfo{" + "name='" + project + '\'' + ", pomPath='" + pomPath + '\''
                + ", compileTarget='" + compileTarget + '\'' + '}';
    }
}
