package com.sequoiacm.contentserver.quota.msg;

public class SetAgreementTimeMsg implements QuotaSyncMsg {
    private long agreementTime;
    private String type;
    private String name;
    private int syncRoundNumber;
    private int quotaRoundNumber;

    public SetAgreementTimeMsg(String type, String name, int syncRoundNumber, int quotaRoundNumber,
            long agreementTime) {
        this.type = type;
        this.name = name;
        this.syncRoundNumber = syncRoundNumber;
        this.quotaRoundNumber = quotaRoundNumber;
        this.agreementTime = agreementTime;
    }

    @Override
    public int getSyncRoundNumber() {
        return syncRoundNumber;
    }

    public long getAgreementTime() {
        return agreementTime;
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
    public int getQuotaRoundNumber() {
        return quotaRoundNumber;
    }

    @Override
    public String toString() {
        return "SetAgreementTimeMsg{" + "agreementTime=" + agreementTime + ", type='" + type + '\''
                + ", name='" + name + '\'' + ", syncRoundNumber=" + syncRoundNumber
                + ", quotaRoundNumber=" + quotaRoundNumber + '}';
    }
}
