package com.sequoiacm.s3import.common.convertor;

import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3import.common.CommonDefine;

import java.util.HashMap;
import java.util.Map;

public class ArgumentConvertor {
    private static Map<String, ValueParser> parserMap = new HashMap<>();

    static {
        initMap();
    }

    public static Object getParse(String name, String value) throws ScmToolsException {
        ValueParser valueParser = parserMap.get(name);
        if (valueParser == null) {
            throw new ScmToolsException("Cannot find " + name + " parser", ScmBaseExitCode.INVALID_ARG);
        }
        return valueParser.convert(name, value);
    }

    private static void addParser(String name, ValueParser valueParser) {
        if (name != null && valueParser != null) {
            parserMap.put(name, valueParser);
        }
    }

    public static void initMap() {
        addParser(CommonDefine.Prop.BATCH_SITE, new IntegerParser(1, 10000));
        addParser(CommonDefine.Prop.STRICT_COMPARISON_MODE, new BooleanParser());
        addParser(CommonDefine.Prop.MAX_FAIL_COUNT, new IntegerParser(1, Integer.MAX_VALUE));
        addParser(CommonDefine.Prop.WORK_COUNT, new IntegerParser(1, 100));
        addParser(CommonDefine.Option.MAX_EXEC_TIME, new LongParser(1, Long.MAX_VALUE));
    }
}
