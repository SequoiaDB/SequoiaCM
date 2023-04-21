package com.sequoiacm.contentserver.quota.msg;

public class DisableQuotaMsg implements QuotaMsg {

    private String type;
    private String name;

    public DisableQuotaMsg(String type, String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "DisableQuotaMsg{" + "type='" + type + '\'' + ", name='" + name + '\'' + '}';
    }
}
