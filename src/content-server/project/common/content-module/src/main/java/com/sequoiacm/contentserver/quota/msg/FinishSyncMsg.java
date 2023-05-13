package com.sequoiacm.contentserver.quota.msg;

public class FinishSyncMsg implements QuotaSyncMsg {

    private String type;
    private String name;
    private int syncRoundNumber;
    private int quotaRoundNumber;

    public FinishSyncMsg(String type, String name, int syncRoundNumber, int quotaRoundNumber) {
        this.type = type;
        this.name = name;
        this.syncRoundNumber = syncRoundNumber;
        this.quotaRoundNumber = quotaRoundNumber;
    }

    @Override
    public int getSyncRoundNumber() {
        return syncRoundNumber;
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
        return "FinishSyncMsg{" + "type='" + type + '\'' + ", name='" + name + '\''
                + ", syncRoundNumber=" + syncRoundNumber + ", quotaRoundNumber=" + quotaRoundNumber
                + '}';
    }
}
