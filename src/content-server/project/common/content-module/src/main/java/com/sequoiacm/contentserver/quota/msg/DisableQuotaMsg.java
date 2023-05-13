package com.sequoiacm.contentserver.quota.msg;

public class DisableQuotaMsg implements QuotaMsg {

    private String type;
    private String name;
    private int quotaRoundNumber;

    public DisableQuotaMsg(String type, String name, int quotaRoundNumber) {
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
        return "DisableQuotaMsg{" + "type='" + type + '\'' + ", name='" + name + '\''
                + ", quotaRoundNumber=" + quotaRoundNumber + '}';
    }
}
