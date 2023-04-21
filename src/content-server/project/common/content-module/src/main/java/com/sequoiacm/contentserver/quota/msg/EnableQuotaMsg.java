package com.sequoiacm.contentserver.quota.msg;

public class EnableQuotaMsg implements QuotaMsg {

    private String type;
    private String name;
    private int quotaRoundNumber;

    public EnableQuotaMsg(String type, String name, int quotaRoundNumber) {
        this.type = type;
        this.name = name;
        this.quotaRoundNumber = quotaRoundNumber;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getName() {
        return name;
    }

    public int getQuotaRoundNumber() {
        return quotaRoundNumber;
    }

    @Override
    public String toString() {
        return "EnableQuotaMsg{" + "type='" + type + '\'' + ", name='" + name + '\''
                + ", quotaRoundNumber=" + quotaRoundNumber + '}';
    }
}
