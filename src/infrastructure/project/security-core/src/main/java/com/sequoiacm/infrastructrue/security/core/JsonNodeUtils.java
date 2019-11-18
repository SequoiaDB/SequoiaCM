package com.sequoiacm.infrastructrue.security.core;

import com.fasterxml.jackson.databind.JsonNode;

final class JsonNodeUtils {
    private JsonNodeUtils() {
    }

    static JsonNode getOrNull(JsonNode node, String fieldName, JsonNode defaultValue) {
        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null");
        }

        if (fieldName == null || fieldName.isEmpty()) {
            throw new IllegalArgumentException("fieldName cannot be null or empty");
        }

        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null) {
            return defaultValue;
        }

        return fieldNode;
    }

    static JsonNode get(JsonNode node, String fieldName) {
        JsonNode fieldNode = getOrNull(node, fieldName, null);
        if (fieldNode == null) {
            throw new IllegalArgumentException("Missing field: " + fieldName);
        }

        return fieldNode;
    }

    static String textValue(JsonNode node, String fieldName) {
        return get(node, fieldName).textValue();
    }

    static int intValue(JsonNode node, String fieldName) {
        return get(node, fieldName).intValue();
    }

    static boolean booleanValue(JsonNode node, String fieldName) {
        return get(node, fieldName).booleanValue();
    }
}
