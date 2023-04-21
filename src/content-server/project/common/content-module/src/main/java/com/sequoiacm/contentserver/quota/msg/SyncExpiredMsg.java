package com.sequoiacm.contentserver.quota.msg;

public class SyncExpiredMsg implements QuotaSyncMsg {

    private String type;
    private String name;
    private int syncRoundNumber;

    public SyncExpiredMsg(String type, String name, int syncRoundNumber) {
        this.type = type;
        this.name = name;
        this.syncRoundNumber = syncRoundNumber;
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
    public int getSyncRoundNumber() {
        return syncRoundNumber;
    }

    @Override
    public String toString() {
        return "SyncExpiredMsg{" + "type='" + type + '\'' + ", name='" + name + '\''
                + ", syncRoundNumber=" + syncRoundNumber + '}';
    }
}
