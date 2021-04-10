package com.sequoiacm.infrastructure.tool.element;

public class ScmNodeRequiredParam {
    private String key;
    private String example;

    // isPrefixKey 为 true 表示字段 key 是一个前缀，
    // 他需要与 bindingKey 的 value 结合起来组成一个完整 key
    // 如 key = k1.， bindingKey = k2 ， -Dk2=v2， 那么 k1 完整
    // 的表示是 -Dk1.v2
    private boolean isPrefixKey;
    private String bindingKey;

    private ScmNodeRequiredParam(String key, String example, boolean isPrefixKey,
            String bindingKey) {
        this.key = key;
        this.example = example;
        this.isPrefixKey = isPrefixKey;
        this.bindingKey = bindingKey;
    }

    public static ScmNodeRequiredParam keyParamInstance(String key, String example) {
        return new ScmNodeRequiredParam(key, example, false, null);
    }

    public static ScmNodeRequiredParam preKeyParamInstance(String preKey, String example,
            String bindingKey) {
        return new ScmNodeRequiredParam(preKey, example, true, bindingKey);
    }

    public String getKey() {
        return key;
    }

    public String getExample() {
        return example;
    }

    public boolean isPrefixKey() {
        return isPrefixKey;
    }

    public String getBindingKey() {
        return bindingKey;
    }
}
