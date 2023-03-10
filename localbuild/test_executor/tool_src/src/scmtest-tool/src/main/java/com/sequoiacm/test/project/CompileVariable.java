package com.sequoiacm.test.project;

import com.sequoiacm.test.common.BsonUtil;
import com.sequoiacm.test.common.ProjectDefine;
import com.sequoiacm.test.parser.ConfConverter;
import org.bson.BSONObject;

public class CompileVariable {

    private String project;
    private String variableName;
    private String variableValue;

    public static final ConfConverter<CompileVariable> CONVERTER = new ConfConverter<CompileVariable>() {
        @Override
        public CompileVariable convert(BSONObject bson) {
            return new CompileVariable(bson);
        }
    };

    public CompileVariable(BSONObject bson) {
        project = BsonUtil.getStringChecked(bson, ProjectDefine.COMPILE_VARIABLE_PROJECT);
        variableName = BsonUtil.getStringChecked(bson,
                ProjectDefine.COMPILE_VARIABLE_VARIABLE_NAME);
        variableValue = BsonUtil.getStringChecked(bson,
                ProjectDefine.COMPILE_VARIABLE_VARIABLE_VALUE);
    }

    public String getProject() {
        return project;
    }

    public String getVariableName() {
        return variableName;
    }

    public String getVariableValue() {
        return variableValue;
    }

    public void setVariableValue(String variableValue) {
        this.variableValue = variableValue;
    }

    @Override
    public String toString() {
        return "CompileVariable{" + "project='" + project + '\'' + ", variableName='" + variableName
                + '\'' + ", variableValue='" + variableValue + '\'' + '}';
    }
}
