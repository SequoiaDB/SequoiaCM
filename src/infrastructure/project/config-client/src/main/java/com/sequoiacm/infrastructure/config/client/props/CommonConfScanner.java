package com.sequoiacm.infrastructure.config.client.props;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CommonConfScanner {

    public Map<ScmPropsMatchRule, PropInfo> scanCommonProps(ConversionService conversionService,
            String confDescFile) throws IOException {
        InputStream is = CommonConfScanner.class.getClassLoader().getResourceAsStream(confDescFile);

        if (is == null) {
            return Collections.emptyMap();
        }

        try {
            return constructCheckRuleMap(is, conversionService);
        }
        finally {
            try {
                is.close();
            }
            catch (Exception e) {
                // ignore
            }
        }
    }

    private Map<ScmPropsMatchRule, PropInfo> constructCheckRuleMap(InputStream is,
            ConversionService conversionService) throws IOException, JsonProcessingException {
        Map<ScmPropsMatchRule, PropInfo> checkRuleMap = new HashMap<>();
        ObjectMapper objMapper = new ObjectMapper();
        JsonNode json = objMapper.readTree(is);
        if (json.isObject()) {
            JsonNode checkList = json.get("checked_list");
            for (JsonNode node : checkList) {
                String type = node.get("type").asText();
                String key = node.get("key").asText();
                JsonNode refreshable = node.get("refreshable");
                boolean isRefreshable = refreshable != null && refreshable.asBoolean();
                PropCheckRule rule = createCheckRuleByValueType(node, type, conversionService);
                checkRuleMap.put(new ScmPropsExactMatchRule(key),
                        new PropInfo(rule, isRefreshable));
            }

            JsonNode forbiddenList = json.get("forbidden_list");
            for (JsonNode node : forbiddenList) {
                String key = node.get("key").asText();
                checkRuleMap.put(new ScmPropsExactMatchRule(key),
                        new PropInfo(new ForbidModifyCheckRule(), false));
            }
        }
        else {
            throw new IllegalArgumentException("json format error:not an array object");
        }
        return checkRuleMap;
    }

    private PropCheckRule createCheckRuleByValueType(JsonNode node, String type,
            ConversionService conversionService) {
        switch (type) {
            case "number":
                Long valueMax = null;
                Long valueMin = null;
                JsonNode max = node.get("max");
                if (max != null) {
                    valueMax = max.asLong();
                }

                JsonNode min = node.get("min");
                if (min != null) {
                    valueMin = min.asLong();
                }
                return new NumberCheckRule(conversionService, valueMax, valueMin);
            case "boolean":
                return new CommonTypeCheckRule(conversionService, Boolean.class);
            case "string":
                return new CommonTypeCheckRule(conversionService, String.class);
            default:
                throw new IllegalArgumentException("unknown value type:" + type);
        }
    }

}

class CommonTypeCheckRule implements PropCheckRule {
    private static final Logger logger = LoggerFactory.getLogger(CommonTypeCheckRule.class);
    private final Class<?> confType;
    private final ConversionService conversionService;

    public CommonTypeCheckRule(ConversionService conversionService, Class<?> confType) {
        this.confType = confType;
        this.conversionService = conversionService;
    }

    @Override
    public boolean checkValue(String value) {
        try {
            conversionService.convert(value, confType);
            return true;
        }
        catch (ConversionFailedException e) {
            logger.warn("failed to convert value to {}: value={}", confType.getName(), value, e);
            return false;
        }
    }

    @Override
    public boolean isDeletable() {
        return true;
    }

    @Override
    public String toString() {
        return "[" + confType.getName() + " value]";
    }
}

class ForbidModifyCheckRule implements PropCheckRule {
    public ForbidModifyCheckRule() {
    }

    @Override
    public boolean checkValue(String value) {
        return false;
    }

    @Override
    public String toString() {
        return "[forbid to modify]";
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

}
