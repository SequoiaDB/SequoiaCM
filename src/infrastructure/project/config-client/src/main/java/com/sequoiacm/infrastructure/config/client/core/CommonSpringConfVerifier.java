package com.sequoiacm.infrastructure.config.client.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequoiacm.infrastructure.config.core.verifier.ScmConfigPropVerifier;
import com.sequoiacm.infrastructure.config.core.verifier.VerifyResult;

@Component
public class CommonSpringConfVerifier implements ScmConfigPropVerifier {
    private Map<String, PropCheckRule> checkRuleMap = new HashMap<>();

    public CommonSpringConfVerifier() throws JsonParseException, JsonMappingException, IOException {
        InputStream is = CommonSpringConfVerifier.class.getClassLoader()
                .getResourceAsStream("spring_conf_check_list.json");

        if (is == null) {
            return;
        }

        try {
            constructCheckRuleMap(is);
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

    private void constructCheckRuleMap(InputStream is) throws IOException, JsonProcessingException {
        ObjectMapper objMapper = new ObjectMapper();
        JsonNode json = objMapper.readTree(is);
        if (json.isObject()) {
            JsonNode checkList = json.get("checked_list");
            for (JsonNode node : checkList) {
                String type = node.get("type").asText();
                String key = node.get("key").asText();
                PropCheckRule rule = createCheckRuleByValueType(node, type);
                checkRuleMap.put(key, rule);
            }

            JsonNode forbiddenList = json.get("forbidden_list");
            for (JsonNode node : forbiddenList) {
                String key = node.get("key").asText();
                checkRuleMap.put(key, new ForbidModifyCheckRule());
            }
        }
        else {
            throw new IllegalArgumentException("json format error:not an array object");
        }
    }

    private PropCheckRule createCheckRuleByValueType(JsonNode node, String type) {
        switch (type) {
            case "integer":
                Integer valueMax = null;
                Integer valueMin = null;
                JsonNode max = node.get("max");
                if (max != null) {
                    valueMax = max.asInt();
                }

                JsonNode min = node.get("min");
                if (min != null) {
                    valueMin = min.asInt();
                }
                return new IntCheckRule(valueMax, valueMin);
            case "boolean":
                return new BooleanCheckRule();
            default:
                throw new IllegalArgumentException("unknown value type:" + type);
        }
    }

    @Override
    public VerifyResult verifyUpdate(String key, String value) {
        PropCheckRule rule = checkRuleMap.get(key);
        if (rule == null) {
            return VerifyResult.getUnrecognizedRes();
        }
        if (rule.checkValue(value)) {
            return VerifyResult.getValidRes();
        }
        return VerifyResult.createInvalidRes(rule.toString());
    }

    @Override
    public VerifyResult verifyDeletion(String key) {
        PropCheckRule rule = checkRuleMap.get(key);
        if (rule == null) {
            return VerifyResult.getUnrecognizedRes();
        }
        if (rule.isDeletable()) {
            return VerifyResult.getValidRes();
        }
        return VerifyResult.createInvalidRes(rule.toString());
    }

}

interface PropCheckRule {
    boolean checkValue(String value);

    boolean isDeletable();
}

abstract class DeletableCheckRuleBase implements PropCheckRule {

    @Override
    public boolean isDeletable() {
        return true;
    }
}

class IntCheckRule extends DeletableCheckRuleBase {
    private int min = Integer.MIN_VALUE;
    private int max = Integer.MAX_VALUE;

    public IntCheckRule() {

    }

    public IntCheckRule(Integer max, Integer min) {
        if (max != null) {
            this.max = max;
        }
        if (min != null) {
            this.min = min;
        }
    }

    @Override
    public boolean checkValue(String value) {
        try {
            Integer v = Integer.valueOf(value);
            if (v <= max && v > min) {
                return true;
            }
            return false;
        }
        catch (Exception e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "[int value, max=" + max + ",min" + min + "]";
    }
}

class BooleanCheckRule extends DeletableCheckRuleBase {
    public BooleanCheckRule() {

    }

    @Override
    public boolean checkValue(String value) {
        try {
            Boolean.valueOf(value);
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "[boolean value]";
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
